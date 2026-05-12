import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {googlePlayAndroidPublisherProvider} from '../googlePlayProvider';
import {restoreGooglePlayPurchasesForUid} from '../service';
import type {PremiumEntitlementResponse} from '../types';

type RestoreGooglePlayPurchasesPayload = {
  purchases?: unknown;
};

export const restoreGooglePlayPurchases = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<PremiumEntitlementResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const data = (request.data ?? {}) as RestoreGooglePlayPurchasesPayload;
      return restoreGooglePlayPurchasesForUid({
        uid,
        purchases: data.purchases,
        provider: googlePlayAndroidPublisherProvider,
      });
    }
);
