import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {validateGooglePlayPurchase} from '../service';
import type {PremiumEntitlementDto} from '../types';

export const validateGooglePlaySubscription = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<PremiumEntitlementDto> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const outcome = await validateGooglePlayPurchase({
        uid,
        input: (request.data ?? {}) as Record<string, unknown>,
        source: 'google_play_validation',
      });
      return outcome.entitlement;
    }
);
