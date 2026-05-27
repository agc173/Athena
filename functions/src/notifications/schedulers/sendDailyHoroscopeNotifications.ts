import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {getMessaging} from 'firebase-admin/messaging';
import * as logger from 'firebase-functions/logger';
import {onSchedule} from 'firebase-functions/v2/scheduler';
import {randomUUID} from 'node:crypto';
import {DateTime} from 'luxon';
import {ENV} from '../../config/env';

const FALLBACK_TIMEZONE = 'Europe/Madrid';
const TARGET_HOUR = 9;
const TARGET_MINUTE = 30;
const WINDOW_MINUTES = 30;

function isInvalidTokenErrorCode(code: string | undefined): boolean {
  return code === 'messaging/registration-token-not-registered' ||
    code === 'messaging/invalid-registration-token';
}

function resolveTimezone(value: unknown): string {
  if (typeof value !== 'string' || value.trim().length === 0) return FALLBACK_TIMEZONE;
  const zone = value.trim();
  return DateTime.now().setZone(zone).isValid ? zone : FALLBACK_TIMEZONE;
}

function shouldSendForLocalTime(nowLocal: DateTime): boolean {
  if (nowLocal.hour !== TARGET_HOUR) return false;
  return nowLocal.minute >= TARGET_MINUTE && nowLocal.minute < TARGET_MINUTE + WINDOW_MINUTES;
}

export const sendDailyHoroscopeNotifications = onSchedule({
  schedule: 'every 30 minutes',
  timeZone: 'UTC',
  region: 'europe-west1',
  retryCount: 0,
  timeoutSeconds: 540,
}, async () => {
  if (!ENV.PUSH_ENABLED || !ENV.PUSH_DAILY_HOROSCOPE_ENABLED) {
    logger.info('sendDailyHoroscopeNotifications skipped by env flags', {
      pushEnabled: ENV.PUSH_ENABLED,
      dailyEnabled: ENV.PUSH_DAILY_HOROSCOPE_ENABLED,
    });
    return;
  }

  const db = getFirestore();
  const sentAtIso = new Date().toISOString();
  const title = 'Tu horóscopo diario está listo';
  const body = 'Descubre la energía de hoy en BWitch ✨';

  let attempted = 0;
  let sent = 0;
  let failed = 0;
  let skipped = 0;
  let errors = 0;

  // NOTE: This collectionGroup query may require a Firestore collection-group index for pushTokens.enabled depending on project index state.
  const tokensSnapshot = await db.collectionGroup('pushTokens')
    .where('enabled', '==', true)
    .get();

  for (const tokenDoc of tokensSnapshot.docs) {
    const tokenRef = tokenDoc.ref;
    const uid = tokenRef.parent.parent?.id;

    if (!uid) {
      skipped++;
      logger.warn('sendDailyHoroscopeNotifications skipped token without uid', {
        tokenHashPrefix: tokenDoc.id.slice(0, 10),
      });
      continue;
    }

    try {
      const timezone = resolveTimezone(tokenDoc.get('timezone'));
      const nowLocal = DateTime.now().setZone(timezone);

      if (!shouldSendForLocalTime(nowLocal)) {
        skipped++;
        continue;
      }

      const dateIso = nowLocal.toISODate();
      if (!dateIso) {
        skipped++;
        continue;
      }

      const campaignId = `daily_horoscope_${dateIso}`;

      const preferencesRef = db.collection('users').doc(uid).collection('notificationPreferences').doc('current');
      const preferencesDoc = await preferencesRef.get();
      const globalEnabled = preferencesDoc.exists ? preferencesDoc.get('globalEnabled') : false;
      const dailyHoroscopeEnabled = preferencesDoc.exists ? preferencesDoc.get('dailyHoroscopeEnabled') : false;

      if (globalEnabled !== true || dailyHoroscopeEnabled !== true) {
        skipped++;
        continue;
      }

      const sendDocId = `${dateIso}_daily_horoscope`;
      const sendRef = db.collection('users').doc(uid).collection('pushNotificationSends').doc(sendDocId);

      try {
        await sendRef.create({
          type: 'daily_horoscope',
          dateIso,
          sentAt: FieldValue.serverTimestamp(),
          status: 'reserved',
          campaignId,
          timezone,
        });
      } catch (error) {
        const code = (error as {code?: number})?.code;
        if (code === 6 || (error as Error).message.includes('Already exists')) {
          skipped++;
          continue;
        }
        throw error;
      }

      const token = tokenDoc.get('token');
      if (typeof token !== 'string' || token.trim().length === 0) {
        await tokenRef.set({
          enabled: false,
          invalidatedAt: FieldValue.serverTimestamp(),
          invalidateReason: 'missing-token',
          lastFailureAt: FieldValue.serverTimestamp(),
          failureCount: FieldValue.increment(1),
        }, {merge: true});

        await sendRef.set({
          sentAt: FieldValue.serverTimestamp(),
          status: 'failed_missing_token',
        }, {merge: true});

        errors++;
        continue;
      }

      attempted++;

      try {
        await getMessaging().send({
          token,
          notification: {title, body},
          data: {
            type: 'daily_horoscope',
            route: 'horoscope',
            campaignId,
            messageId: randomUUID(),
            sentAtIso,
            deeplinkVersion: 'v1',
            title,
            body,
          },
          android: {
            notification: {
              channelId: 'bwitch_daily',
            },
          },
        });

        await Promise.all([
          tokenRef.set({
            lastSuccessAt: FieldValue.serverTimestamp(),
          }, {merge: true}),
          sendRef.set({
            sentAt: FieldValue.serverTimestamp(),
            status: 'sent',
          }, {merge: true}),
        ]);

        sent++;
      } catch (error) {
        const code = (error as {code?: string})?.code;

        if (isInvalidTokenErrorCode(code)) {
          await tokenRef.set({
            enabled: false,
            invalidatedAt: FieldValue.serverTimestamp(),
            invalidateReason: code,
            lastFailureAt: FieldValue.serverTimestamp(),
            failureCount: FieldValue.increment(1),
          }, {merge: true});
        } else {
          await tokenRef.set({
            lastFailureAt: FieldValue.serverTimestamp(),
            failureCount: FieldValue.increment(1),
          }, {merge: true});
        }

        await sendRef.set({
          sentAt: FieldValue.serverTimestamp(),
          status: `failed_${code ?? 'unknown'}`,
        }, {merge: true});

        failed++;

        logger.warn('sendDailyHoroscopeNotifications token send failed', {
          uid,
          tokenHashPrefix: tokenDoc.id.slice(0, 10),
          code,
        });
      }
    } catch (error) {
      errors++;
      logger.error('sendDailyHoroscopeNotifications item failed', {
        uid,
        tokenHashPrefix: tokenDoc.id.slice(0, 10),
        error: (error as Error)?.message ?? String(error),
      });
    }
  }

  logger.info('sendDailyHoroscopeNotifications completed', {
    attempted,
    sent,
    failed,
    skipped,
    errors,
    totalTokens: tokensSnapshot.size,
  });
});
