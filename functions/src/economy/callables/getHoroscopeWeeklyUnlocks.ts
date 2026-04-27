import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import type {
  GetHoroscopeWeeklyUnlocksData,
  GetHoroscopeWeeklyUnlocksResponse,
} from '../types';
import {assertAllowedWeekKey, normalizeWeekKey} from './horoscopePeriodKeys';
import {
  getUnlockedPeriodKeyList,
  normalizePeriodKeyList,
} from './horoscopePeriodGetUnlocks';

export const getHoroscopeWeeklyUnlocks = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<GetHoroscopeWeeklyUnlocksResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as GetHoroscopeWeeklyUnlocksData;
      const weekKeyList = normalizePeriodKeyList(data.weekKeyList, {
        keyListLabel: 'weekKeyList',
        normalizePeriodKey: normalizeWeekKey,
      });

      if (weekKeyList.length === 0) {
        return {unlockedWeekKeyList: []};
      }

      const unlockedWeekKeyList = await getUnlockedPeriodKeyList(uid, weekKeyList, {
        unlockKeyPrefix: 'weekly',
        assertAllowedPeriodKey: assertAllowedWeekKey,
      });

      return {unlockedWeekKeyList};
    }
);
