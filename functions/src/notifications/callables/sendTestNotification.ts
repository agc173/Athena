import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {getMessaging} from 'firebase-admin/messaging';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import * as logger from 'firebase-functions/logger';
import {randomUUID} from 'node:crypto';
import {ENV} from '../../config/env';

type TestNotificationType = 'daily_horoscope' | 'daily_reward' | 'tarot_oracle_reminder' | 'spiritual';

type SendTestNotificationData = {
  title?: unknown;
  body?: unknown;
  type?: unknown;
};

type SendTestNotificationResponse = {
  ok: true;
  attempted: number;
  sent: number;
  failed: number;
};

function asOptionalString(value: unknown, field: string): string | undefined {
  if (value === undefined) return undefined;
  if (typeof value !== 'string') throw new HttpsError('invalid-argument', `${field} must be string`);
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function asOptionalType(value: unknown): TestNotificationType {
  if (value === undefined) return 'spiritual';
  if (
    value === 'daily_horoscope' ||
    value === 'daily_reward' ||
    value === 'tarot_oracle_reminder' ||
    value === 'spiritual'
  ) {
    return value;
  }
  throw new HttpsError('invalid-argument', 'type must be one of: daily_horoscope, daily_reward, tarot_oracle_reminder, spiritual');
}

function asAndroidChannelId(type: TestNotificationType): string {
  if (type === 'daily_horoscope') return 'bwitch_daily';
  if (type === 'daily_reward') return 'bwitch_rewards';
  return 'bwitch_spiritual';
}

function asRoute(type: TestNotificationType): string {
  if (type === 'daily_horoscope') return 'horoscope';
  if (type === 'daily_reward') return 'rewards';
  if (type === 'tarot_oracle_reminder') return 'oracle';
  return 'spiritual';
}

function isInvalidTokenErrorCode(code: string | undefined): boolean {
  return code === 'messaging/registration-token-not-registered' ||
    code === 'messaging/invalid-registration-token';
}

export const sendTestNotification = onCall({
  region: 'europe-west1',
  enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
}, async (request): Promise<SendTestNotificationResponse> => {
  if (!ENV.PUSH_TEST_CALLABLE_ENABLED) {
    throw new HttpsError('permission-denied', 'sendTestNotification is disabled');
  }

  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

  const data = (request.data ?? {}) as SendTestNotificationData;
  const title = asOptionalString(data.title, 'title') ?? 'ATHENA';
  const body = asOptionalString(data.body, 'body') ?? 'Tus notificaciones ya están activas ✨';
  const type = asOptionalType(data.type);
  const route = asRoute(type);
  const sentAtIso = new Date().toISOString();
  const messageId = randomUUID();

  const db = getFirestore();
  const userRef = db.collection('users').doc(uid);
  const preferencesRef = userRef.collection('notificationPreferences').doc('current');
  const preferencesDoc = await preferencesRef.get();

  const globalEnabled = preferencesDoc.exists ? preferencesDoc.get('globalEnabled') : false;
  if (globalEnabled !== true) {
    throw new HttpsError('failed-precondition', 'Push notifications are disabled for this user');
  }

  const tokensSnapshot = await userRef.collection('pushTokens').where('enabled', '==', true).get();
  if (tokensSnapshot.empty) {
    throw new HttpsError('failed-precondition', 'No enabled push tokens found for this user');
  }

  const androidChannelId = asAndroidChannelId(type);
  const attempts = await Promise.all(tokensSnapshot.docs.map(async (tokenDoc) => {
    const token = tokenDoc.get('token');
    if (typeof token !== 'string' || token.trim().length === 0) {
      await tokenDoc.ref.set({
        enabled: false,
        invalidatedAt: FieldValue.serverTimestamp(),
        invalidateReason: 'missing-token',
        lastFailureAt: FieldValue.serverTimestamp(),
        failureCount: FieldValue.increment(1),
      }, {merge: true});
      return {sent: false};
    }

    try {
      await getMessaging().send({
        token,
        notification: {title, body},
        data: {
          type,
          route,
          campaignId: 'qa_test',
          messageId,
          sentAtIso,
          deeplinkVersion: 'v1',
          title,
          body,
        },
        android: {
          notification: {
            channelId: androidChannelId,
          },
        },
      });

      await tokenDoc.ref.set({
        lastSuccessAt: FieldValue.serverTimestamp(),
      }, {merge: true});

      return {sent: true};
    } catch (error) {
      const code = (error as {code?: string})?.code;

      if (isInvalidTokenErrorCode(code)) {
        await tokenDoc.ref.set({
          enabled: false,
          invalidatedAt: FieldValue.serverTimestamp(),
          invalidateReason: code,
          lastFailureAt: FieldValue.serverTimestamp(),
          failureCount: FieldValue.increment(1),
        }, {merge: true});
      } else {
        await tokenDoc.ref.set({
          lastFailureAt: FieldValue.serverTimestamp(),
          failureCount: FieldValue.increment(1),
        }, {merge: true});
      }

      logger.warn('sendTestNotification token send failed', {
        uid,
        tokenHashPrefix: tokenDoc.id.slice(0, 10),
        code,
      });

      return {sent: false};
    }
  }));

  const attempted = attempts.length;
  const sent = attempts.filter((item) => item.sent).length;
  const failed = attempted - sent;

  logger.info('sendTestNotification completed', {uid, attempted, sent, failed, type, route});

  return {ok: true, attempted, sent, failed};
});
