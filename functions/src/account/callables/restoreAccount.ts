import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {restoreAccountForUid} from '../service';

export const restoreAccount = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request) => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      return restoreAccountForUid(uid);
    }
);
