import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {googlePlayAndroidPublisherProvider} from '../googlePlayProvider';
import {validateGooglePlayPurchaseForUid} from '../service';
import type {GooglePlayPurchasePayload, PremiumEntitlementResponse} from '../types';

export const validateGooglePlayPurchase = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<PremiumEntitlementResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      return validateGooglePlayPurchaseForUid({
        uid,
        payload: (request.data ?? {}) as GooglePlayPurchasePayload,
        provider: googlePlayAndroidPublisherProvider,
      });
    }
);
