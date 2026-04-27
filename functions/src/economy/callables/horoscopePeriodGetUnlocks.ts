import {HttpsError} from 'firebase-functions/v2/https';
import {economyHoroscopeUnlockRef} from '../firestorePaths';

type GetHoroscopePeriodUnlocksConfig = {
  keyListLabel: 'weekKeyList' | 'monthKeyList';
  normalizePeriodKey: (value: unknown) => string;
};

type GetUnlockedPeriodKeyListConfig = {
  unlockKeyPrefix: 'weekly' | 'monthly';
  assertAllowedPeriodKey: (periodKey: string) => void;
};

export function normalizePeriodKeyList(
    value: unknown,
    config: GetHoroscopePeriodUnlocksConfig
): string[] {
  if (!Array.isArray(value)) {
    throw new HttpsError('invalid-argument', `${config.keyListLabel} must be an array`);
  }

  const normalized = value.map((item) => config.normalizePeriodKey(item));
  if (normalized.length === 0) return [];
  if (normalized.length > 2) {
    throw new HttpsError('invalid-argument', `${config.keyListLabel} max length is 2`);
  }

  return Array.from(new Set(normalized));
}

export async function getUnlockedPeriodKeyList(
    uid: string,
    periodKeyList: string[],
    config: GetUnlockedPeriodKeyListConfig
): Promise<string[]> {
  for (const periodKey of periodKeyList) {
    config.assertAllowedPeriodKey(periodKey);
  }

  const snaps = await Promise.all(
      periodKeyList.map(
          (periodKey) => economyHoroscopeUnlockRef(uid, `${config.unlockKeyPrefix}:${periodKey}`).get()
      )
  );

  return periodKeyList.filter((periodKey, index) => snaps[index].exists);
}
