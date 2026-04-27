import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {economyHoroscopeUnlockRef} from '../firestorePaths';
import type {
  GetHoroscopeWeeklyUnlocksData,
  GetHoroscopeWeeklyUnlocksResponse,
} from '../types';
import {assertAllowedWeekKey, normalizeWeekKey} from './horoscopePeriodKeys';

function normalizeWeekKeyList(value: unknown): string[] {
  if (!Array.isArray(value)) {
    throw new HttpsError('invalid-argument', 'weekKeyList must be an array');
  }

  const normalized = value.map((item) => normalizeWeekKey(item));
  if (normalized.length === 0) return [];
  if (normalized.length > 2) {
    throw new HttpsError('invalid-argument', 'weekKeyList max length is 2');
  }

  return Array.from(new Set(normalized));
}

export const getHoroscopeWeeklyUnlocks = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<GetHoroscopeWeeklyUnlocksResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as GetHoroscopeWeeklyUnlocksData;
      const weekKeyList = normalizeWeekKeyList(data.weekKeyList);
      if (weekKeyList.length === 0) {
        return {unlockedWeekKeyList: []};
      }

      for (const weekKey of weekKeyList) {
        assertAllowedWeekKey(weekKey);
      }

      const snaps = await Promise.all(
          weekKeyList.map((weekKey) => economyHoroscopeUnlockRef(uid, `weekly:${weekKey}`).get())
      );

      const unlockedWeekKeyList = weekKeyList.filter((weekKey, index) => snaps[index].exists);
      return {unlockedWeekKeyList};
    }
);
