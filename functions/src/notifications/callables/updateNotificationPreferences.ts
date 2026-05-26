import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {EmptyOkResponse, UpdateNotificationPreferencesData} from '../types';

function asRequiredBoolean(value: unknown, field: string): boolean {
  if (typeof value !== 'boolean') throw new HttpsError('invalid-argument', `${field} must be boolean`);
  return value;
}

export const updateNotificationPreferences = onCall({
  region: 'europe-west1',
  enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
}, async (request): Promise<EmptyOkResponse> => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

  const data = (request.data ?? {}) as UpdateNotificationPreferencesData;

  await getFirestore().collection('users').doc(uid).collection('notificationPreferences').doc('current').set({
    globalEnabled: asRequiredBoolean(data.globalEnabled, 'globalEnabled'),
    dailyHoroscopeEnabled: asRequiredBoolean(data.dailyHoroscopeEnabled, 'dailyHoroscopeEnabled'),
    dailyRewardEnabled: asRequiredBoolean(data.dailyRewardEnabled, 'dailyRewardEnabled'),
    tarotOracleReminderEnabled: asRequiredBoolean(data.tarotOracleReminderEnabled, 'tarotOracleReminderEnabled'),
    ritualsEnabled: asRequiredBoolean(data.ritualsEnabled, 'ritualsEnabled'),
    habitsEnabled: asRequiredBoolean(data.habitsEnabled, 'habitsEnabled'),
    updatedAt: FieldValue.serverTimestamp(),
    updatedBy: 'CLIENT_CALLABLE',
  }, {merge: true});

  return {ok: true};
});
