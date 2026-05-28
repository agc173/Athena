import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';

const USER_CONSTELLATION_PROGRESS_COLLECTION = 'userConstellationProgress';
const DEFAULT_MAX_TOTAL_PROGRESS = 226;
const TODAY_ISO_REGEX = /^\d{4}-\d{2}-\d{2}$/;

type ClaimDailyConstellationProgressData = {
  todayIso?: unknown;
};

type ConstellationProgressDoc = {
  totalProgress?: number;
  lastRewardDateIso?: string;
  updatedAt?: Timestamp;
  createdAt?: Timestamp;
};

type ClaimDailyConstellationProgressResponse = {
  totalProgress: number;
  previousTotalProgress: number;
  rewarded: boolean;
  isComplete: boolean;
};

function parseTodayIso(value: unknown): string {
  if (typeof value !== 'string' || !TODAY_ISO_REGEX.test(value)) {
    throw new HttpsError('invalid-argument', 'todayIso must be yyyy-MM-dd');
  }
  return value;
}

export const claimDailyConstellationProgress = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<ClaimDailyConstellationProgressResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as ClaimDailyConstellationProgressData;
      const todayIso = parseTodayIso(data.todayIso);
      const maxTotalProgress = DEFAULT_MAX_TOTAL_PROGRESS;

      const db = getFirestore();
      return db.runTransaction(async (tx) => {
        const docRef = db.collection(USER_CONSTELLATION_PROGRESS_COLLECTION).doc(uid);
        const snap = await tx.get(docRef);
        const current = (snap.data() as ConstellationProgressDoc | undefined) ?? {};

        const previousTotalProgress = Math.max(0, Math.floor(Number(current.totalProgress ?? 0)));
        const clampedPrevious = Math.min(previousTotalProgress, maxTotalProgress);
        const alreadyClaimedToday = current.lastRewardDateIso === todayIso;
        const wasComplete = clampedPrevious >= maxTotalProgress;

        if (alreadyClaimedToday) {
          return {
            totalProgress: clampedPrevious,
            previousTotalProgress: clampedPrevious,
            rewarded: false,
            isComplete: wasComplete,
          };
        }

        const nextProgress = wasComplete ? clampedPrevious : Math.min(maxTotalProgress, clampedPrevious + 1);
        const rewarded = !wasComplete && nextProgress > clampedPrevious;
        const now = Timestamp.now();

        tx.set(docRef, {
          totalProgress: nextProgress,
          lastRewardDateIso: todayIso,
          updatedAt: now,
          createdAt: current.createdAt ?? now,
        } as ConstellationProgressDoc, {merge: true});

        return {
          totalProgress: nextProgress,
          previousTotalProgress: clampedPrevious,
          rewarded,
          isComplete: nextProgress >= maxTotalProgress,
        };
      });
    }
);
