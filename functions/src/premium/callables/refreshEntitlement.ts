import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {refreshEntitlementForUid} from '../service';
import type {PremiumEntitlementDto} from '../types';

export const refreshEntitlement = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<PremiumEntitlementDto> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const data = (request.data ?? {}) as {force?: unknown};
      if (data.force != null && typeof data.force !== 'boolean') {
        throw new HttpsError('invalid-argument', 'force must be a boolean');
      }
      return refreshEntitlementForUid(uid, data.force === true);
    }
);
