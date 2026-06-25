import {getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';

export type GetNotificationPreferencesResponse = {
  exists: boolean;
  globalEnabled: boolean;
  dailyHoroscopeEnabled: boolean;
  dailyRewardEnabled: boolean;
  tarotOracleReminderEnabled: boolean;
  ritualsEnabled: boolean;
  habitsEnabled: boolean;
};

function asBooleanOrFalse(value: unknown): boolean {
  return value === true;
}

export const getNotificationPreferences = onCall({
  region: 'europe-west1',
  enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
}, async (request): Promise<GetNotificationPreferencesResponse> => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

  const doc = await getFirestore()
    .collection('users')
    .doc(uid)
    .collection('notificationPreferences')
    .doc('current')
    .get();

  if (!doc.exists) {
    return {
      exists: false,
      globalEnabled: false,
      dailyHoroscopeEnabled: false,
      dailyRewardEnabled: false,
      tarotOracleReminderEnabled: false,
      ritualsEnabled: false,
      habitsEnabled: false,
    };
  }

  return {
    exists: true,
    globalEnabled: asBooleanOrFalse(doc.get('globalEnabled')),
    dailyHoroscopeEnabled: asBooleanOrFalse(doc.get('dailyHoroscopeEnabled')),
    dailyRewardEnabled: asBooleanOrFalse(doc.get('dailyRewardEnabled')),
    tarotOracleReminderEnabled: asBooleanOrFalse(doc.get('tarotOracleReminderEnabled')),
    ritualsEnabled: asBooleanOrFalse(doc.get('ritualsEnabled')),
    habitsEnabled: asBooleanOrFalse(doc.get('habitsEnabled')),
  };
});
