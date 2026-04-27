import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import type {
  UnlockHoroscopeWeeklyData,
  UnlockHoroscopeWeeklyResponse,
} from '../types';
import {assertAllowedWeekKey, normalizeWeekKey} from './horoscopePeriodKeys';
import {unlockHoroscopePeriod} from './horoscopePeriodUnlock';

export const unlockHoroscopeWeekly = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<UnlockHoroscopeWeeklyResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as UnlockHoroscopeWeeklyData;

      return unlockHoroscopePeriod<UnlockHoroscopeWeeklyResponse>(uid, data, {
        periodType: 'weekly',
        requestType: 'HOROSCOPE_UNLOCK_WEEKLY',
        module: 'HOROSCOPE_WEEKLY',
        ledgerType: 'HOROSCOPE_WEEKLY_MOON_SPEND',
        keyField: 'weekKey',
        unlockKeyPrefix: 'weekly',
        usageCounter: 'horoscopeWeeklyMoonUsed',
        normalizePeriodKey: normalizeWeekKey,
        assertAllowedPeriodKey: assertAllowedWeekKey,
        source: 'unlockHoroscopeWeekly',
      });
    }
);
