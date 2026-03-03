import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {RequestType, type TarotDrawData} from '../types';

const ALLOWED_TAROT_TYPES = new Set<RequestType>([
  RequestType.TAROT_1,
  RequestType.TAROT_3,
]);

export const tarotDraw = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: true,
    },
    async (request) => {
      const data = request.data as TarotDrawData;

      if (!data?.requestType || !ALLOWED_TAROT_TYPES.has(data.requestType)) {
        throw new HttpsError(
            'invalid-argument',
            'requestType must be TAROT_1 or TAROT_3'
        );
      }

      // TODO(oracle-v1): Implement deterministic draw by requestId using the full 78-card deck.
      throw new HttpsError(
          'unimplemented',
          'tarotDraw scaffold created; business logic pending'
      );
    }
);
