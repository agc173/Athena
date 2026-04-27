import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import type {
  UnlockHoroscopeMonthlyData,
  UnlockHoroscopeMonthlyResponse,
} from '../types';
import {assertAllowedMonthKey, normalizeMonthKey} from './horoscopePeriodKeys';
import {unlockHoroscopePeriod} from './horoscopePeriodUnlock';

export const unlockHoroscopeMonthly = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<UnlockHoroscopeMonthlyResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as UnlockHoroscopeMonthlyData;

      return unlockHoroscopePeriod<UnlockHoroscopeMonthlyResponse>(uid, data, {
        periodType: 'monthly',
        requestType: 'HOROSCOPE_UNLOCK_MONTHLY',
        module: 'HOROSCOPE_MONTHLY',
        ledgerType: 'HOROSCOPE_MONTHLY_MOON_SPEND',
        keyField: 'monthKey',
        unlockKeyPrefix: 'monthly',
        usageCounter: 'horoscopeMonthlyMoonUsed',
        normalizePeriodKey: normalizeMonthKey,
        assertAllowedPeriodKey: assertAllowedMonthKey,
        source: 'unlockHoroscopeMonthly',
      });
    }
);
