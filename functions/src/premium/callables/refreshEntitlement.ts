import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {refreshEntitlementForUid} from '../service';
import type {PremiumEntitlementResponse} from '../types';

export const refreshEntitlement = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<PremiumEntitlementResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      return refreshEntitlementForUid(uid);
    }
);
