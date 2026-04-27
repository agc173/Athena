import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import type {
  GetHoroscopeMonthlyUnlocksData,
  GetHoroscopeMonthlyUnlocksResponse,
} from '../types';
import {assertAllowedMonthKey, normalizeMonthKey} from './horoscopePeriodKeys';
import {
  getUnlockedPeriodKeyList,
  normalizePeriodKeyList,
} from './horoscopePeriodGetUnlocks';

export const getHoroscopeMonthlyUnlocks = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<GetHoroscopeMonthlyUnlocksResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as GetHoroscopeMonthlyUnlocksData;
      const monthKeyList = normalizePeriodKeyList(data.monthKeyList, {
        keyListLabel: 'monthKeyList',
        normalizePeriodKey: normalizeMonthKey,
      });

      if (monthKeyList.length === 0) {
        return {unlockedMonthKeyList: []};
      }

      const unlockedMonthKeyList = await getUnlockedPeriodKeyList(uid, monthKeyList, {
        unlockKeyPrefix: 'monthly',
        assertAllowedPeriodKey: assertAllowedMonthKey,
      });

      return {unlockedMonthKeyList};
    }
);
