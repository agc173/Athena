import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {dateIsoMadrid, economyHoroscopeUnlockRef} from '../firestorePaths';
import type {GetHoroscopeDailyUnlocksData, GetHoroscopeDailyUnlocksResponse} from '../types';

function normalizeDateIsoList(value: unknown): string[] {
  if (!Array.isArray(value)) {
    throw new HttpsError('invalid-argument', 'dateIsoList must be an array');
  }

  const normalized = value.map((item) => {
    if (typeof item !== 'string') {
      throw new HttpsError('invalid-argument', 'dateIsoList must contain strings');
    }
    const trimmed = item.trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
      throw new HttpsError('invalid-argument', 'dateIso format must be YYYY-MM-DD');
    }
    return trimmed;
  });

  if (normalized.length === 0) return [];
  if (normalized.length > 7) {
    throw new HttpsError('invalid-argument', 'dateIsoList max length is 7');
  }

  return Array.from(new Set(normalized));
}

export const getHoroscopeDailyUnlocks = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<GetHoroscopeDailyUnlocksResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as GetHoroscopeDailyUnlocksData;
      const dateIsoList = normalizeDateIsoList(data.dateIsoList);
      if (dateIsoList.length === 0) {
        return {unlockedDateIsoList: []};
      }

      const todayIso = dateIsoMadrid();
      const maxIso = new Date(`${todayIso}T00:00:00.000Z`);
      maxIso.setUTCDate(maxIso.getUTCDate() + 6);
      const maxDateIso = maxIso.toISOString().slice(0, 10);

      for (const dateIso of dateIsoList) {
        if (dateIso < todayIso || dateIso > maxDateIso) {
          throw new HttpsError('failed-precondition', 'dateIsoList must be between today and today+6');
        }
      }

      const snaps = await Promise.all(
          dateIsoList.map((dateIso) => economyHoroscopeUnlockRef(uid, `daily:${dateIso}`).get())
      );

      const unlockedDateIsoList = dateIsoList.filter((dateIso, index) => snaps[index].exists);
      return {unlockedDateIsoList};
    }
);
