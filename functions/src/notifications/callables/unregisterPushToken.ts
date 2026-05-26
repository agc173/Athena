import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import * as logger from 'firebase-functions/logger';
import {ENV} from '../../config/env';
import {sha256Token} from '../tokenHash';
import {EmptyOkResponse, PushPlatform, UnregisterPushTokenData} from '../types';

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

export const unregisterPushToken = onCall({
  region: 'europe-west1',
  enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
}, async (request): Promise<EmptyOkResponse> => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

  const data = (request.data ?? {}) as UnregisterPushTokenData;
  const token = asNonEmptyToken(data.token);
  const platform = asPlatform(data.platform);

  const tokenHash = sha256Token(token);
  const docRef = getFirestore().collection('users').doc(uid).collection('pushTokens').doc(tokenHash);

  await docRef.set({
    tokenHash,
    platform,
    enabled: false,
    lastSeenAt: FieldValue.serverTimestamp(),
    invalidatedAt: FieldValue.serverTimestamp(),
    invalidateReason: 'CLIENT_UNREGISTER',
  }, {merge: true});

  logger.info('unregisterPushToken success', {uid, tokenHashPrefix: tokenHash.slice(0, 10), platform});
  return {ok: true};
});
