import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {RequestType, type OracleAskData} from '../types';

export const oracleAsk = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: true,
    },
    async (request) => {
      const data = request.data as OracleAskData;

      if (data?.requestType !== RequestType.ORACLE_1Q) {
        throw new HttpsError('invalid-argument', 'requestType must be ORACLE_1Q');
      }

      if (!data.question || data.question.trim().length === 0) {
        throw new HttpsError('invalid-argument', 'question is required');
      }

      // TODO(oracle-v1): Wire LLMRouter + cost guard + Firestore persistence.
      throw new HttpsError(
          'unimplemented',
          'oracleAsk scaffold created; business logic pending'
      );
    }
);
