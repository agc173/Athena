import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {restoreGooglePlayPurchaseArray} from '../service';
import type {PremiumEntitlementDto} from '../types';

type RestoreResponse = {
  entitlement: PremiumEntitlementDto;
  restoredCount: number;
  activeTokenFound: boolean;
};

export const restoreGooglePlayPurchases = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<RestoreResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const data = (request.data ?? {}) as {purchases?: unknown};
      return restoreGooglePlayPurchaseArray(uid, data.purchases ?? []);
    }
);
