import {getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';

const USER_CONSTELLATION_PROGRESS_COLLECTION = 'userConstellationProgress';
const DEFAULT_MAX_TOTAL_PROGRESS = 226;

type ConstellationProgressDoc = {
  totalProgress?: number;
  lastRewardDateIso?: string;
};

type GetConstellationProgressResponse = {
  totalProgress: number;
  lastRewardDateIso: string | null;
  isComplete: boolean;
};

export const getConstellationProgress = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<GetConstellationProgressResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const db = getFirestore();
      const docRef = db.collection(USER_CONSTELLATION_PROGRESS_COLLECTION).doc(uid);
      const snap = await docRef.get();
      const current = snap.exists ? (snap.data() as ConstellationProgressDoc | undefined) : undefined;

      if (!current) {
        return {
          totalProgress: 0,
          lastRewardDateIso: null,
          isComplete: false,
        };
      }

      const totalProgress = Math.min(
          DEFAULT_MAX_TOTAL_PROGRESS,
          Math.max(0, Math.floor(Number(current.totalProgress ?? 0)))
      );

      return {
        totalProgress,
        lastRewardDateIso: current.lastRewardDateIso ?? null,
        isComplete: totalProgress >= DEFAULT_MAX_TOTAL_PROGRESS,
      };
    }
);
