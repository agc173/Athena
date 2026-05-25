import {onCall, HttpsError} from 'firebase-functions/v2/https';
import {createHash} from 'node:crypto';

function asRequiredString(value: unknown, fieldName: string): string {
  if (typeof value !== 'string' || value.trim().length === 0) {
    throw new HttpsError('invalid-argument', `${fieldName} is required`);
  }
  return value.trim();
}

export const claimMoonPackPurchase = onCall(
    {region: 'europe-west1', enforceAppCheck: true},
    async (request) => {
      if (!request.auth?.uid) {
        throw new HttpsError('unauthenticated', 'auth_required');
      }

      const data = (request.data ?? {}) as Record<string, unknown>;
      const productId = asRequiredString(data.productId, 'productId');
      const purchaseToken = asRequiredString(data.purchaseToken, 'purchaseToken');
      const packageName = asRequiredString(data.packageName, 'packageName');
      const orderId = typeof data.orderId === 'string' ? data.orderId.trim() : null;
      const purchaseTokenHash = createHash('sha256').update(purchaseToken, 'utf8').digest('hex');

      throw new HttpsError('failed-precondition', 'not_implemented', {
        productId,
        packageName,
        orderId,
        purchaseTokenHash,
        ledgerType: 'MOON_PACK_PURCHASE',
        idempotencyKey: purchaseTokenHash,
      });
    }
);
