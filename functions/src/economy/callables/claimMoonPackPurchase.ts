import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {onCall, HttpsError} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {dateIsoMadrid, economyBalanceRef, economyLedgerRef} from '../firestorePaths';
import type {EconomyBalanceDoc, EconomyLedgerEntryDoc} from '../types';
import {getGooglePlayAndroidPublisherProvider} from '../../premium/googlePlayProvider';
import {hashPurchaseToken} from '../../premium/service';

const MOON_PACKS: Record<string, number> = {
  bwitch_moons_pack_10: 10,
  bwitch_moons_pack_30: 30,
  bwitch_moons_pack_80: 80,
};

const PRODUCT_PURCHASE_STATE_PURCHASED = 0;
const PRODUCT_CONSUMPTION_STATE_YET_TO_BE_CONSUMED = 0;
const PRODUCT_ACKNOWLEDGEMENT_STATE_PENDING = 0;
const PRODUCT_ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED = 1;

type ProductPurchase = {
  kind?: string;
  purchaseState?: number;
  consumptionState?: number;
  acknowledgementState?: number;
  orderId?: string;
  purchaseTimeMillis?: string;
  purchaseType?: number;
};

function asRequiredString(value: unknown, fieldName: string): string {
  if (typeof value !== 'string' || value.trim().length === 0) {
    throw new HttpsError('invalid-argument', `${fieldName} is required`);
  }
  return value.trim();
}

function asCount(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

// TODO(test): extraer AndroidPublisher client detrás de una interfaz inyectable para test unitario sin red.
async function queryInAppPurchase(packageName: string, productId: string, purchaseToken: string): Promise<ProductPurchase> {
  const provider = getGooglePlayAndroidPublisherProvider() as unknown as {
    getAuthClient: () => Promise<{request<T>(options: {url: string; method: 'GET'}): Promise<{data: T}>}>;
  };
  const client = await provider.getAuthClient();
  const encodedPackageName = encodeURIComponent(packageName);
  const encodedProductId = encodeURIComponent(productId);
  const encodedToken = encodeURIComponent(purchaseToken);
  const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodedPackageName}/purchases/products/${encodedProductId}/tokens/${encodedToken}`;
  const response = await client.request<ProductPurchase>({url, method: 'GET'});
  return response.data;
}

export const claimMoonPackPurchase = onCall(
    {region: 'europe-west1', enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV},
    async (request) => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'auth_required');

      const data = (request.data ?? {}) as Record<string, unknown>;
      const productId = asRequiredString(data.productId, 'productId');
      const purchaseToken = asRequiredString(data.purchaseToken, 'purchaseToken');
      const packageName = asRequiredString(data.packageName, 'packageName');
      if (packageName != ENV.ANDROID_PACKAGE_NAME) throw new HttpsError('permission-denied', 'package_name_mismatch');
      if (!(productId in MOON_PACKS)) throw new HttpsError('invalid-argument', 'unsupported_product');

      const purchaseTokenHash = hashPurchaseToken(purchaseToken);
      const amount = MOON_PACKS[productId];
      const dateIso = dateIsoMadrid();

      let purchase: ProductPurchase;
      try {
        purchase = await queryInAppPurchase(packageName, productId, purchaseToken);
      } catch (error) {
        const status = typeof error === 'object' && error != null && 'response' in error ?
          (error as {response?: {status?: unknown}}).response?.status : undefined;
        if (status === 400 || status === 404 || status === 410) {
          throw new HttpsError('failed-precondition', 'google_play_token_invalid');
        }
        throw error;
      }

      if (purchase.purchaseState !== PRODUCT_PURCHASE_STATE_PURCHASED) {
        throw new HttpsError('failed-precondition', 'purchase_not_completed');
      }

      const db = getFirestore();
      const result = await db.runTransaction(async (tx) => {
        const tokenRef = db.doc(`purchaseTokenIndex/${purchaseTokenHash}`);
        const requestRef = db.doc(`economyRequests/${uid}/requests/moon-pack-${purchaseTokenHash}`);
        const balanceRef = economyBalanceRef(uid);

        const [tokenSnap, requestSnap, balanceSnap] = await Promise.all([tx.get(tokenRef), tx.get(requestRef), tx.get(balanceRef)]);

        if (tokenSnap.exists) {
          const ownerUid = tokenSnap.data()?.uid;
          if (ownerUid !== uid) throw new HttpsError('already-exists', 'purchase_token_owned_by_other_user');
          if (requestSnap.exists) {
            const existingBalance = asCount((balanceSnap.data() as EconomyBalanceDoc | undefined)?.balance);
            return {result: 'ALREADY_CLAIMED', moonsGranted: 0, balance: existingBalance, purchaseTokenHash};
          }
        }

        const currentBalance = asCount((balanceSnap.data() as EconomyBalanceDoc | undefined)?.balance);
        const nextBalance = currentBalance + amount;
        const now = Timestamp.now();

        tx.set(balanceRef, {balance: nextBalance, updatedAt: now}, {merge: true});
        tx.set(tokenRef, {uid, platform: 'android', kind: 'inapp_moon_pack', productId, updatedAt: now}, {merge: true});
        tx.set(economyLedgerRef(uid, `moon-pack:${purchaseTokenHash}`), {
          type: 'MOON_PACK_PURCHASE',
          amount,
          requestId: `moon-pack-${purchaseTokenHash}`,
          dateIso,
          source: 'GOOGLE_PLAY',
          module: 'STORE',
          createdAt: now,
        } as EconomyLedgerEntryDoc, {merge: true});
        tx.set(requestRef, {
          requestId: `moon-pack-${purchaseTokenHash}`,
          type: 'CLAIM_MOON_PACK_PURCHASE',
          result: 'CLAIMED',
          responsePayload: {productId, packageName, purchaseTokenHash, orderId: purchase.orderId ?? null},
          response: {result: 'CLAIMED', amount, balance: nextBalance, purchaseTokenHash},
          dateIso,
          createdAt: now,
          updatedAt: now,
        }, {merge: true});

        return {result: 'CLAIMED', moonsGranted: amount, balance: nextBalance, purchaseTokenHash};
      });

      const consumptionState = purchase.consumptionState;
      const shouldConsume = consumptionState === PRODUCT_CONSUMPTION_STATE_YET_TO_BE_CONSUMED || consumptionState == null;

      return {
        ...result,
        shouldConsume,
        isAcknowledged: purchase.acknowledgementState === PRODUCT_ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED,
        consumptionState: consumptionState ?? null,
        acknowledgementState: purchase.acknowledgementState ?? PRODUCT_ACKNOWLEDGEMENT_STATE_PENDING,
      };
    }
);
