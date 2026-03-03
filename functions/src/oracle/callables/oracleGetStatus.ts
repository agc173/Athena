import {onCall} from 'firebase-functions/v2/https';
import {RequestStatus} from '../types';

export const oracleGetStatus = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: true,
    },
    async () => {
      // TODO(oracle-v1): Replace mock response with global Firestore-backed oracle status.
      return {
        status: RequestStatus.QUEUED,
        message: 'oracleGetStatus scaffold response',
      };
    }
);
