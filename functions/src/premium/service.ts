import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {ENV} from '../config/env';
import {
  buildEntitlementFields,
  mapGoogleValidationToSubscriptionStatus,
  shouldReplaceEntitlement,
  timestampFromIso,
  timestampFromMillis,
  toPremiumEntitlementDto,
} from './entitlementMapper';
import {buildGooglePlayValidator} from './googlePlayValidator';
import {assertValidPurchaseToken, hashPurchaseToken} from './tokenHash';
import type {
  GooglePlayValidationInput,
  GooglePlayValidationResult,
  PremiumEntitlementDto,
  PurchaseReceiptDoc,
  PurchaseTokenIndexDoc,
  SubscriptionStatus,
  UserEntitlementDoc,
} from './types';

const DEFAULT_PRODUCT_ALLOWLIST = 'bwitch_premium_monthly,premium_monthly';
const DEFAULT_PACKAGE_NAME = 'com.bwitch.app';
const STALE_REFRESH_MS = 24 * 60 * 60 * 1000;

export type GooglePlayPurchaseInput = {
  productId?: unknown;
  purchaseToken?: unknown;
  packageName?: unknown;
  basePlanId?: unknown;
  offerId?: unknown;
  clientPurchaseState?: unknown;
  clientAcknowledged?: unknown;
};

export type ValidationOutcome = {
  entitlement: PremiumEntitlementDto;
  active: boolean;
};

function csv(name: string, fallback: string): string[] {
  return (process.env[name] ?? fallback)
      .split(',')
      .map((value) => value.trim())
      .filter(Boolean);
}

function normalizeOptionalString(value: unknown, field: string, maxLength = 200): string | undefined {
  if (value == null) return undefined;
  if (typeof value !== 'string') {
    throw new HttpsError('invalid-argument', `${field} must be a string`);
  }
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  if (trimmed.length > maxLength) {
    throw new HttpsError('invalid-argument', `${field} is too long`);
  }
  return trimmed;
}

function normalizeRequiredString(value: unknown, field: string, maxLength = 200): string {
  const normalized = normalizeOptionalString(value, field, maxLength);
  if (!normalized) {
    throw new HttpsError('invalid-argument', `${field} is required`);
  }
  return normalized;
}

function normalizeClientAcknowledged(value: unknown): boolean | undefined {
  if (value == null) return undefined;
  if (typeof value !== 'boolean') {
    throw new HttpsError('invalid-argument', 'clientAcknowledged must be a boolean');
  }
  return value;
}

function productAllowlist(): Set<string> {
  return new Set(csv('GOOGLE_PLAY_PRODUCT_ALLOWLIST', DEFAULT_PRODUCT_ALLOWLIST));
}

function normalizePackageName(value: unknown): string {
  const packageName = normalizeOptionalString(value, 'packageName', 255) ?? process.env.GOOGLE_PLAY_PACKAGE_NAME ?? DEFAULT_PACKAGE_NAME;
  if (!/^[a-zA-Z][\w]*(\.[a-zA-Z][\w]*)+$/.test(packageName)) {
    throw new HttpsError('invalid-argument', 'packageName is invalid');
  }
  return packageName;
}

export function normalizeGooglePlayPurchaseInput(data: GooglePlayPurchaseInput): Omit<GooglePlayValidationInput, 'uid'> {
  const productId = normalizeRequiredString(data.productId, 'productId');
  if (!productAllowlist().has(productId)) {
    throw new HttpsError('invalid-argument', 'Unsupported productId');
  }

  let purchaseToken: string;
  try {
    purchaseToken = assertValidPurchaseToken(data.purchaseToken);
  } catch (error) {
    throw new HttpsError('invalid-argument', (error as Error).message);
  }

  return {
    productId,
    purchaseToken,
    packageName: normalizePackageName(data.packageName),
    basePlanId: normalizeOptionalString(data.basePlanId, 'basePlanId'),
    offerId: normalizeOptionalString(data.offerId, 'offerId'),
    clientPurchaseState: normalizeOptionalString(data.clientPurchaseState, 'clientPurchaseState'),
    clientAcknowledged: normalizeClientAcknowledged(data.clientAcknowledged),
  };
}

function purchaseTokenHashSecret(): string {
  const configured = process.env.PURCHASE_TOKEN_HASH_SECRET ?? '';
  if (configured) return configured;

  if ((process.env.GOOGLE_PLAY_VALIDATION_MODE ?? '').toLowerCase() === 'mock' || ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV) {
    return 'bwitch-local-dev-purchase-token-hash-secret';
  }

  throw new HttpsError('failed-precondition', 'PURCHASE_TOKEN_HASH_SECRET is not configured');
}

function removeUndefined<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map(removeUndefined) as T;
  }
  if (value && typeof value === 'object' && !(value instanceof Timestamp)) {
    const out: Record<string, unknown> = {};
    for (const [key, item] of Object.entries(value)) {
      if (item !== undefined) out[key] = removeUndefined(item);
    }
    return out as T;
  }
  return value;
}

function receiptPath(uid: string, tokenHash: string): string {
  return `purchaseReceipts/${uid}/items/${tokenHash}`;
}

function buildReceiptDoc(params: {
  uid: string;
  tokenHash: string;
  validation: GooglePlayValidationResult;
  linkedPurchaseTokenHash?: string;
  status: SubscriptionStatus;
  now: Timestamp;
  firstSeenAt: Timestamp;
  previousValidationCount: number;
  requestId?: string;
}): PurchaseReceiptDoc {
  return {
    uid: params.uid,
    purchaseTokenHash: params.tokenHash,
    platform: 'google_play',
    environment: params.validation.environment,
    packageName: params.validation.packageName,
    productId: params.validation.productId,
    basePlanId: params.validation.basePlanId,
    offerId: params.validation.offerId,
    subscriptionStatus: params.status,
    linkedPurchaseTokenHash: params.linkedPurchaseTokenHash,
    google: {
      rawState: params.validation.rawState,
      acknowledgementState: params.validation.acknowledgementState,
      lineItemExpiryTime: params.validation.lineItemExpiryTime,
      autoRenewing: params.validation.autoRenewing,
      cancelReason: params.validation.cancelReason,
      orderId: params.validation.orderId,
      latestOrderId: params.validation.latestOrderId,
      regionCode: params.validation.regionCode,
      testPurchase: params.validation.testPurchase,
    },
    acknowledged: params.validation.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED',
    premiumUntil: timestampFromIso(params.validation.lineItemExpiryTime),
    startedAt: timestampFromMillis(params.validation.startedAtMillis),
    lastValidatedAt: params.now,
    firstSeenAt: params.firstSeenAt,
    updatedAt: params.now,
    validationCount: params.previousValidationCount + 1,
    lastValidationRequestId: params.requestId,
    schemaVersion: 1,
  };
}

export async function validateGooglePlayPurchase(params: {
  uid: string;
  input: GooglePlayPurchaseInput;
  source: 'google_play_validation' | 'restore';
  requestId?: string;
}): Promise<ValidationOutcome> {
  const normalized = normalizeGooglePlayPurchaseInput(params.input);
  const tokenHash = hashPurchaseToken(normalized.purchaseToken, purchaseTokenHashSecret());
  const validator = buildGooglePlayValidator();
  const validation = await validator.validateSubscription({uid: params.uid, ...normalized});
  const status = mapGoogleValidationToSubscriptionStatus(validation);
  const linkedPurchaseTokenHash = validation.linkedPurchaseToken ?
    hashPurchaseToken(validation.linkedPurchaseToken, purchaseTokenHashSecret()) : undefined;

  const db = getFirestore();
  const entitlement = await db.runTransaction(async (tx) => {
    const entitlementRef = db.doc(`userEntitlements/${params.uid}`);
    const receiptRef = db.doc(receiptPath(params.uid, tokenHash));
    const indexRef = db.doc(`purchaseTokenIndex/${tokenHash}`);

    const [entitlementSnap, receiptSnap, indexSnap] = await Promise.all([
      tx.get(entitlementRef),
      tx.get(receiptRef),
      tx.get(indexRef),
    ]);

    if (indexSnap.exists) {
      const indexDoc = indexSnap.data() as PurchaseTokenIndexDoc;
      if (indexDoc.uid !== params.uid) {
        throw new HttpsError('already-exists', 'Purchase token is already linked to another account');
      }
    }

    const now = Timestamp.now();
    const existingReceipt = receiptSnap.data() as PurchaseReceiptDoc | undefined;
    const firstSeenAt = existingReceipt?.firstSeenAt ?? now;
    const receiptDoc = buildReceiptDoc({
      uid: params.uid,
      tokenHash,
      validation,
      linkedPurchaseTokenHash,
      status,
      now,
      firstSeenAt,
      previousValidationCount: existingReceipt?.validationCount ?? 0,
      requestId: params.requestId,
    });

    const currentEntitlement = entitlementSnap.data() as UserEntitlementDoc | undefined;
    const candidate = buildEntitlementFields({
      validation,
      status,
      tokenHash,
      receiptPath: receiptRef.path,
      source: params.source,
      now,
      createdAt: currentEntitlement?.createdAt ?? now,
    });

    tx.set(receiptRef, removeUndefined(receiptDoc), {merge: true});
    tx.set(indexRef, removeUndefined({
      uid: params.uid,
      receiptPath: receiptRef.path,
      productId: validation.productId,
      platform: 'google_play' as const,
      firstSeenAt: (indexSnap.data() as PurchaseTokenIndexDoc | undefined)?.firstSeenAt ?? now,
      updatedAt: now,
    }), {merge: true});

    const shouldReplace = shouldReplaceEntitlement(currentEntitlement, candidate);
    if (shouldReplace) {
      tx.set(entitlementRef, removeUndefined(candidate), {merge: true});
      return candidate;
    }

    return currentEntitlement;
  });

  return {
    entitlement: toPremiumEntitlementDto(entitlement),
    active: entitlement?.isSubscriber === true,
  };
}

export async function restoreGooglePlayPurchaseArray(uid: string, purchases: unknown): Promise<{
  entitlement: PremiumEntitlementDto;
  restoredCount: number;
  activeTokenFound: boolean;
}> {
  if (!Array.isArray(purchases)) {
    throw new HttpsError('invalid-argument', 'purchases must be an array');
  }
  if (purchases.length > 20) {
    throw new HttpsError('invalid-argument', 'purchases array is too large');
  }

  if (purchases.length === 0) {
    return {
      entitlement: toPremiumEntitlementDto(undefined),
      restoredCount: 0,
      activeTokenFound: false,
    };
  }

  let restoredCount = 0;
  let activeTokenFound = false;
  let latestEntitlement: PremiumEntitlementDto | undefined;

  for (const purchase of purchases) {
    const outcome = await validateGooglePlayPurchase({
      uid,
      input: (purchase ?? {}) as GooglePlayPurchaseInput,
      source: 'restore',
    });
    restoredCount += 1;
    activeTokenFound = activeTokenFound || outcome.active;
    latestEntitlement = outcome.entitlement;
  }

  return {
    entitlement: latestEntitlement ?? toPremiumEntitlementDto(undefined),
    restoredCount,
    activeTokenFound,
  };
}

export async function refreshEntitlementForUid(uid: string, force: boolean): Promise<PremiumEntitlementDto> {
  const db = getFirestore();
  const snap = await db.doc(`userEntitlements/${uid}`).get();
  const doc = snap.data() as UserEntitlementDoc | undefined;
  if (!doc) return toPremiumEntitlementDto(undefined);

  const validatedAt = doc.lastValidatedAt?.toMillis() ?? 0;
  const stale = Date.now() - validatedAt > STALE_REFRESH_MS;
  const needsRestore = (force || stale) && doc.platform === 'google_play';
  return toPremiumEntitlementDto(doc, needsRestore);
}
