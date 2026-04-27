import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {economyHoroscopeUnlockRef} from '../firestorePaths';
import type {
  GetHoroscopeMonthlyUnlocksData,
  GetHoroscopeMonthlyUnlocksResponse,
} from '../types';
import {assertAllowedMonthKey, normalizeMonthKey} from './horoscopePeriodKeys';

function normalizeMonthKeyList(value: unknown): string[] {
  if (!Array.isArray(value)) {
    throw new HttpsError('invalid-argument', 'monthKeyList must be an array');
  }

  const normalized = value.map((item) => normalizeMonthKey(item));
  if (normalized.length === 0) return [];
  if (normalized.length > 2) {
    throw new HttpsError('invalid-argument', 'monthKeyList max length is 2');
  }

  return Array.from(new Set(normalized));
}

export const getHoroscopeMonthlyUnlocks = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<GetHoroscopeMonthlyUnlocksResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as GetHoroscopeMonthlyUnlocksData;
      const monthKeyList = normalizeMonthKeyList(data.monthKeyList);
      if (monthKeyList.length === 0) {
        return {unlockedMonthKeyList: []};
      }

      for (const monthKey of monthKeyList) {
        assertAllowedMonthKey(monthKey);
      }

      const snaps = await Promise.all(
          monthKeyList.map((monthKey) => economyHoroscopeUnlockRef(uid, `monthly:${monthKey}`).get())
      );

      const unlockedMonthKeyList = monthKeyList.filter((monthKey, index) => snaps[index].exists);
      return {unlockedMonthKeyList};
    }
);
