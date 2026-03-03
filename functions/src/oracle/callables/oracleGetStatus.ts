import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {RequestStatus, type OracleGetStatusData} from '../types';

export const oracleGetStatus = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: true,
    },
    async (request) => {
      const data = request.data as OracleGetStatusData;

      if (!data?.requestId) {
        throw new HttpsError('invalid-argument', 'requestId is required');
      }

      // TODO(oracle-v1): Replace mock response with Firestore-backed request status lookup.
      return {
        requestId: data.requestId,
        status: RequestStatus.QUEUED,
        message: 'oracleGetStatus scaffold response',
      };
    }
);
