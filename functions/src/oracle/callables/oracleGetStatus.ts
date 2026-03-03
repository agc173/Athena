import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';

type SystemMode = 'NORMAL' | 'DEGRADED' | 'EMERGENCY';

type OracleStatusResponse = {
  mode: SystemMode;
  message?: string;
  updatedAt?: Timestamp;
};

function parseSystemMode(value: unknown): SystemMode {
  if (value === 'DEGRADED') return 'DEGRADED';
  if (value === 'EMERGENCY') return 'EMERGENCY';
  return 'NORMAL';
}

export const oracleGetStatus = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<OracleStatusResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const db = getFirestore();
      const statusSnap = await db.doc('oracleSystemStatus/current').get();

      if (!statusSnap.exists) {
        return {mode: 'NORMAL'};
      }

      const data = statusSnap.data();

      return {
        mode: parseSystemMode(data?.mode),
        message: typeof data?.message === 'string' ? data.message : undefined,
        updatedAt: data?.updatedAt instanceof Timestamp ? data.updatedAt : undefined,
      };
    }
);
