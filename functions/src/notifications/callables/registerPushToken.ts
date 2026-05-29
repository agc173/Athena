import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import * as logger from 'firebase-functions/logger';
import {ENV} from '../../config/env';
import {sha256Token} from '../tokenHash';
import {EmptyOkResponse, PushPlatform, RegisterPushTokenData} from '../types';

function asNonEmptyToken(value: unknown): string {
  if (typeof value !== 'string' || value.trim().length === 0) {
    throw new HttpsError('invalid-argument', 'token is required');
  }
  return value.trim();
}

function asPlatform(value: unknown): PushPlatform {
  if (value === 'android' || value === 'ios') return value;
  throw new HttpsError('invalid-argument', 'platform must be android or ios');
}

function asOptionalNullableString(value: unknown): string | null | undefined {
  if (value === undefined) return undefined;
  if (value === null) return null;
  if (typeof value !== 'string') {
    throw new HttpsError('invalid-argument', 'optional fields must be string or null');
  }
  return value;
}

function asBoolean(value: unknown, field: string): boolean {
  if (typeof value !== 'boolean') throw new HttpsError('invalid-argument', `${field} must be boolean`);
  return value;
}

export const registerPushToken = onCall({
  region: 'europe-west1',
  enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
}, async (request): Promise<EmptyOkResponse> => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

  const data = (request.data ?? {}) as RegisterPushTokenData;
  const token = asNonEmptyToken(data.token);
  const platform = asPlatform(data.platform);
  const permissionGranted = asBoolean(data.permissionGranted, 'permissionGranted');
  const appVersion = asOptionalNullableString(data.appVersion);
  const locale = asOptionalNullableString(data.locale);
  const timezone = asOptionalNullableString(data.timezone);

  const db = getFirestore();
  const tokenHash = sha256Token(token);
  const docRef = db.collection('users').doc(uid).collection('pushTokens').doc(tokenHash);
  const indexRef = db.collection('pushTokenIndex').doc(tokenHash);

  const patch: Record<string, unknown> = {
    token,
    tokenHash,
    platform,
    permissionGranted,
    enabled: permissionGranted === true,
    lastSeenAt: FieldValue.serverTimestamp(),
    invalidatedAt: null,
    invalidateReason: null,
  };

  if (appVersion !== undefined) patch.appVersion = appVersion;
  if (locale !== undefined) patch.locale = locale;
  if (timezone !== undefined) patch.timezone = timezone;

  let previousUid: string | undefined;

  await db.runTransaction(async (tx) => {
    const indexSnap = await tx.get(indexRef);
    const owner = indexSnap.exists ? indexSnap.get('uid') : undefined;
    previousUid = typeof owner === 'string' && owner !== uid ? owner : undefined;

    if (previousUid) {
      const previousTokenRef = db.collection('users').doc(previousUid).collection('pushTokens').doc(tokenHash);
      tx.set(previousTokenRef, {
        enabled: false,
        permissionGranted: false,
        invalidatedAt: FieldValue.serverTimestamp(),
        invalidateReason: 'TOKEN_REASSIGNED',
        lastSeenAt: FieldValue.serverTimestamp(),
      }, {merge: true});
    }

    tx.set(docRef, patch, {merge: true});
    tx.set(indexRef, {
      uid,
      tokenHash,
      platform,
      updatedAt: FieldValue.serverTimestamp(),
      lastSeenAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  });

  logger.info('registerPushToken success', {
    uid,
    tokenHashPrefix: tokenHash.slice(0, 10),
    platform,
    previousUidChanged: Boolean(previousUid),
  });
  return {ok: true};
});
