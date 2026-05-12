import {createHash} from 'node:crypto';
import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {PREMIUM_CONFIG} from './config';
import type {
  AndroidPublisherProvider,
  GooglePlayPurchasePayload,
  PremiumEntitlementResponse,
  PurchaseTokenIndexDoc,
  UserEntitlementDoc,
} from './types';

const SOURCE_GOOGLE_PLAY = 'google_play';
const ACTIVE_STATUS = 'active';
const INACTIVE_STATUS = 'inactive';

export type EntitlementActivation = {
  uid: string;
  productId: string;
  basePlanId: string;
  packageName: string;
  purchaseTokenHash: string;
  expiresAtMillis: number | null;
};

export type EntitlementStore = {
  activateGooglePlayEntitlement(activation: EntitlementActivation): Promise<void>;
  readUserEntitlement(uid: string): Promise<UserEntitlementDoc | null>;
};

export class FirestoreEntitlementStore implements EntitlementStore {
  async activateGooglePlayEntitlement(activation: EntitlementActivation): Promise<void> {
    const db = getFirestore();
    const entitlementRef = db.collection('userEntitlements').doc(activation.uid);
    const tokenIndexRef = db.collection('purchaseTokenIndex').doc(activation.purchaseTokenHash);

    await db.runTransaction(async (tx) => {
      const tokenSnap = await tx.get(tokenIndexRef);
      const tokenOwner = (tokenSnap.data() as PurchaseTokenIndexDoc | undefined)?.uid;
      if (tokenSnap.exists && tokenOwner != null && tokenOwner !== activation.uid) {
        throw new HttpsError('failed-precondition', 'purchase_token_already_linked');
      }

      const entitlementPatch: FirebaseFirestore.UpdateData<UserEntitlementDoc> = {
        isSubscriber: true,
        status: ACTIVE_STATUS,
        productId: activation.productId,
        basePlanId: activation.basePlanId,
        source: SOURCE_GOOGLE_PLAY,
        purchaseTokenHash: activation.purchaseTokenHash,
        packageName: activation.packageName,
        updatedAt: FieldValue.serverTimestamp(),
      };

      if (activation.expiresAtMillis != null) {
        entitlementPatch.expiresAt = Timestamp.fromMillis(activation.expiresAtMillis);
      }

      tx.set(entitlementRef, entitlementPatch, {merge: true});
      tx.set(tokenIndexRef, {
        uid: activation.uid,
        productId: activation.productId,
        packageName: activation.packageName,
        updatedAt: FieldValue.serverTimestamp(),
      });
    });
  }

  async readUserEntitlement(uid: string): Promise<UserEntitlementDoc | null> {
    const db = getFirestore();
    const entitlementSnap = await db.collection('userEntitlements').doc(uid).get();
    return entitlementSnap.exists ? (entitlementSnap.data() as UserEntitlementDoc | undefined) ?? null : null;
  }
}

export const firestoreEntitlementStore = new FirestoreEntitlementStore();

export function hashPurchaseToken(purchaseToken: string): string {
  return createHash('sha256').update(purchaseToken, 'utf8').digest('hex');
}

function asRequiredString(value: unknown, fieldName: string): string {
  if (typeof value !== 'string') {
    throw new HttpsError('invalid-argument', `${fieldName} is required`);
  }
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    throw new HttpsError('invalid-argument', `${fieldName} is required`);
  }
  return trimmed;
}

function asOptionalString(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

export type NormalizedGooglePlayPurchase = {
  productId: string;
  basePlanId: string | null;
  purchaseToken: string;
  purchaseState: string;
  isAcknowledged: boolean | null;
  orderId: string | null;
  packageName: string;
};

export function normalizeGooglePlayPurchasePayload(
    payload: GooglePlayPurchasePayload,
    options: {requirePurchased: boolean}
): NormalizedGooglePlayPurchase {
  const productId = asRequiredString(payload.productId, 'productId');
  if (productId !== PREMIUM_CONFIG.googlePlayProductMonthly) {
    throw new HttpsError('invalid-argument', 'invalid_product_id');
  }

  const basePlanId = asOptionalString(payload.basePlanId);
  if (basePlanId != null && basePlanId !== PREMIUM_CONFIG.googlePlayBasePlanMonthly) {
    throw new HttpsError('invalid-argument', 'invalid_base_plan_id');
  }

  const packageName = asRequiredString(payload.packageName, 'packageName');
  if (packageName !== PREMIUM_CONFIG.androidPackageName) {
    throw new HttpsError('invalid-argument', 'invalid_package_name');
  }

  const purchaseToken = asRequiredString(payload.purchaseToken, 'purchaseToken');
  const purchaseState = asRequiredString(payload.purchaseState, 'purchaseState');
  if (options.requirePurchased && purchaseState !== 'PURCHASED') {
    throw new HttpsError('failed-precondition', 'purchase_not_purchased');
  }

  return {
    productId,
    basePlanId,
    purchaseToken,
    purchaseState,
    isAcknowledged: typeof payload.isAcknowledged === 'boolean' ? payload.isAcknowledged : null,
    orderId: asOptionalString(payload.orderId),
    packageName,
  };
}

export function inactiveResponse(productId: string | null = null): PremiumEntitlementResponse {
  return {
    isActive: false,
    active: false,
    status: INACTIVE_STATUS,
    productId,
    planType: null,
  };
}

export function activeMonthlyResponse(productId: string): PremiumEntitlementResponse {
  return {
    isActive: true,
    active: true,
    status: ACTIVE_STATUS,
    productId,
    planType: 'monthly',
  };
}

export async function validateGooglePlayPurchaseForUid(params: {
  uid: string;
  payload: GooglePlayPurchasePayload;
  provider: AndroidPublisherProvider;
  store?: EntitlementStore;
}): Promise<PremiumEntitlementResponse> {
  const purchase = normalizeGooglePlayPurchasePayload(params.payload, {requirePurchased: true});
  return validateNormalizedGooglePlayPurchaseForUid({
    uid: params.uid,
    purchase,
    provider: params.provider,
    store: params.store ?? firestoreEntitlementStore,
  });
}

export async function restoreGooglePlayPurchasesForUid(params: {
  uid: string;
  purchases: unknown;
  provider: AndroidPublisherProvider;
  store?: EntitlementStore;
}): Promise<PremiumEntitlementResponse> {
  if (!Array.isArray(params.purchases)) {
    throw new HttpsError('invalid-argument', 'purchases must be an array');
  }

  for (const rawPurchase of params.purchases) {
    const purchase = normalizeGooglePlayPurchasePayload(
        (rawPurchase ?? {}) as GooglePlayPurchasePayload,
        {requirePurchased: false}
    );

    if (purchase.purchaseState !== 'PURCHASED') continue;

    const response = await validateNormalizedGooglePlayPurchaseForUid({
      uid: params.uid,
      purchase,
      provider: params.provider,
      store: params.store ?? firestoreEntitlementStore,
    });
    if (response.active) return response;
  }

  return inactiveResponse(PREMIUM_CONFIG.googlePlayProductMonthly);
}

async function validateNormalizedGooglePlayPurchaseForUid(params: {
  uid: string;
  purchase: NormalizedGooglePlayPurchase;
  provider: AndroidPublisherProvider;
  store: EntitlementStore;
}): Promise<PremiumEntitlementResponse> {
  const validation = await params.provider.validateSubscription({
    packageName: params.purchase.packageName,
    productId: params.purchase.productId,
    basePlanId: params.purchase.basePlanId ?? PREMIUM_CONFIG.googlePlayBasePlanMonthly,
    purchaseToken: params.purchase.purchaseToken,
  });

  if (!validation.active || validation.productId !== params.purchase.productId) {
    return inactiveResponse(params.purchase.productId);
  }

  const googleBasePlanId = validation.basePlanId ?? params.purchase.basePlanId;
  if (googleBasePlanId != null && googleBasePlanId !== PREMIUM_CONFIG.googlePlayBasePlanMonthly) {
    return inactiveResponse(params.purchase.productId);
  }

  const purchaseTokenHash = hashPurchaseToken(params.purchase.purchaseToken);
  await params.store.activateGooglePlayEntitlement({
    uid: params.uid,
    productId: params.purchase.productId,
    basePlanId: googleBasePlanId ?? PREMIUM_CONFIG.googlePlayBasePlanMonthly,
    packageName: params.purchase.packageName,
    purchaseTokenHash,
    expiresAtMillis: validation.expiresAtMillis,
  });

  return activeMonthlyResponse(params.purchase.productId);
}

export async function refreshEntitlementForUid(
    uid: string,
    store: EntitlementStore = firestoreEntitlementStore
): Promise<PremiumEntitlementResponse> {
  const entitlement = await store.readUserEntitlement(uid);
  if (entitlement == null) return inactiveResponse(null);

  if (entitlement?.isSubscriber !== true) {
    return inactiveResponse(entitlement?.productId ?? null);
  }

  if (entitlement.expiresAt instanceof Timestamp && entitlement.expiresAt.toMillis() <= Date.now()) {
    return inactiveResponse(entitlement.productId ?? null);
  }

  if (entitlement.productId !== PREMIUM_CONFIG.googlePlayProductMonthly) {
    return inactiveResponse(entitlement.productId ?? null);
  }

  return activeMonthlyResponse(entitlement.productId);
}

export const __testables = {
  activeMonthlyResponse,
  hashPurchaseToken,
  inactiveResponse,
  normalizeGooglePlayPurchasePayload,
};
