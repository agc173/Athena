import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import * as logger from 'firebase-functions/logger';
import {ENV} from '../../config/env';
import {normalizeUsername, validateNormalizedUsername} from '../username';
import {BIRTH_ESSENCE_SUMMARY_MAX_LENGTH, normalizeSingleLineInput, removeUnsafeControlChars} from '../../utils/inputNormalization';

type SaveUserProfileData = {
  displayName?: unknown;
  photoUrl?: unknown;
  email?: unknown;
  username?: unknown;
  birthDate?: unknown;
  zodiacSign?: unknown;
  description?: unknown;
  descriptionProvided?: unknown;
  birthEssenceSummary?: unknown;
  updatedAtEpochMillis?: unknown;
};

type UserProfileDoc = {
  displayName?: string;
  photoUrl?: string;
  email?: string;
  username?: string;
  birthDate?: string;
  zodiacSign?: string;
  description?: string;
  birthEssenceSummary?: string;
  updatedAtEpochMillis?: number;
  updatedAt?: Timestamp;
};

const DISPLAY_NAME_MAX_LENGTH = 60;
const EMAIL_MAX_LENGTH = 254;
const PHOTO_URL_MAX_LENGTH = 2048;
const DESCRIPTION_MAX_LENGTH = 160;
const UPDATED_AT_MAX_EPOCH_MILLIS = 4102444800000; // 2100-01-01T00:00:00.000Z

const ZODIAC_SIGNS = new Set([
  'aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo',
  'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces',
]);

const EMAIL_REGEX = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const BIDI_AND_ZERO_WIDTH_REGEX = /[\u200B-\u200F\u202A-\u202E\u2060-\u206F\uFEFF]/g;

type UsernameIndexDoc = {
  uid: string;
  username: string;
  updatedAt?: Timestamp;
};

type UsernameIndexWrite = {
  uid: string;
  username: string;
  updatedAt: FirebaseFirestore.FieldValue;
};

type SanitizedProfileLog = {
  uid: string;
  hasUsername: boolean;
  usernameLength: number;
  hasDisplayName: boolean;
  hasPhotoUrl: boolean;
  hasEmail: boolean;
  hasBirthDate: boolean;
  hasZodiacSign: boolean;
  hasDescription: boolean;
  hasBirthEssenceSummary: boolean;
  updatedAtEpochMillis: number;
};

function normalizeSafeSingleLine(value: string): string {
  return normalizeSingleLineInput(
      removeUnsafeControlChars(value)
          .replace(BIDI_AND_ZERO_WIDTH_REGEX, '')
  );
}

function asOptionalTrimmedString(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const normalized = normalizeSafeSingleLine(value);
  return normalized.length > 0 ? normalized : null;
}

function asOptionalStringWithin(value: unknown, field: string, maxLength: number): string | null {
  const text = asOptionalTrimmedString(value);
  if (!text) return null;
  if (text.length > maxLength) {
    throw new HttpsError('invalid-argument', `${field} is too long`);
  }
  return text;
}

function asOptionalDisplayName(value: unknown): string | null {
  return asOptionalStringWithin(value, 'displayName', DISPLAY_NAME_MAX_LENGTH);
}

function asOptionalEmail(value: unknown): string | null {
  const email = asOptionalStringWithin(value, 'email', EMAIL_MAX_LENGTH);
  if (!email) return null;
  if (!EMAIL_REGEX.test(email)) {
    throw new HttpsError('invalid-argument', 'email is invalid');
  }
  return email;
}

function asOptionalPhotoUrl(value: unknown): string | null {
  const photoUrl = asOptionalStringWithin(value, 'photoUrl', PHOTO_URL_MAX_LENGTH);
  if (!photoUrl) return null;
  if (!photoUrl.startsWith('https://')) {
    throw new HttpsError('invalid-argument', 'photoUrl must use https');
  }
  return photoUrl;
}

function asOptionalZodiacSign(value: unknown): string | null {
  const sign = asOptionalTrimmedString(value)?.toLowerCase() ?? null;
  if (!sign) return null;
  if (!ZODIAC_SIGNS.has(sign)) {
    throw new HttpsError('invalid-argument', 'zodiacSign is invalid');
  }
  return sign;
}

function asOptionalBirthDate(value: unknown): string | null {
  const text = asOptionalTrimmedString(value);
  if (!text) return null;

  const datePattern = /^\d{4}-\d{2}-\d{2}$/;
  if (!datePattern.test(text)) {
    throw new HttpsError('invalid-argument', 'birthDate must be YYYY-MM-DD');
  }
  return text;
}


function asOptionalBirthEssenceSummary(value: unknown): string | null {
  const text = asOptionalTrimmedString(value);
  if (!text) return null;
  return text.slice(0, BIRTH_ESSENCE_SUMMARY_MAX_LENGTH);
}

function asOptionalLong(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  return Math.trunc(value);
}

function safeClientUpdatedAtEpochMillis(value: unknown, fallback: number): number {
  const candidate = asOptionalLong(value);
  if (candidate == null) return fallback;
  if (candidate < 0 || candidate > UPDATED_AT_MAX_EPOCH_MILLIS) return fallback;
  return candidate;
}
function asBoolean(value: unknown): boolean {
  return value === true;
}

function asDisplayNameOrFallback(displayNameRaw: unknown, normalizedUsername: string): string {
  return asOptionalDisplayName(displayNameRaw) ?? normalizedUsername;
}

export const __testables = {
  asOptionalTrimmedString,
  asOptionalBirthDate,
  asOptionalBirthEssenceSummary,
  asOptionalEmail,
  asOptionalPhotoUrl,
  asOptionalZodiacSign,
  asOptionalLong,
  safeClientUpdatedAtEpochMillis,
  asDisplayNameOrFallback,
  sanitizedProfileLog,
};

function sanitizedProfileLog(input: {
  uid: string;
  username: string | null;
  displayName: string | null;
  photoUrl: string | null;
  email: string | null;
  birthDate: string | null;
  zodiacSign: string | null;
  description: string | null;
  birthEssenceSummary: string | null;
  updatedAtEpochMillis: number;
}): SanitizedProfileLog {
  return {
    uid: input.uid,
    hasUsername: input.username != null,
    usernameLength: input.username?.length ?? 0,
    hasDisplayName: input.displayName != null,
    hasPhotoUrl: input.photoUrl != null,
    hasEmail: input.email != null,
    hasBirthDate: input.birthDate != null,
    hasZodiacSign: input.zodiacSign != null,
    hasDescription: input.description != null,
    hasBirthEssenceSummary: input.birthEssenceSummary != null,
    updatedAtEpochMillis: input.updatedAtEpochMillis,
  };
}


function isHttpsError(error: unknown): error is HttpsError {
  return error instanceof HttpsError;
}

function safeUidTag(uid: string): string {
  if (uid.length <= 6) return uid;
  return uid.slice(0, 3) + '***' + uid.slice(-3);
}

function safeErrorInfo(error: unknown): {errorCode: string | null; errorMessage: string | null} {
  if (typeof error !== 'object' || error == null) {
    return {errorCode: null, errorMessage: null};
  }

  const codeRaw = 'code' in error ? (error as {code?: unknown}).code : null;
  const messageRaw = 'message' in error ? (error as {message?: unknown}).message : null;

  return {
    errorCode: typeof codeRaw === 'string' ? codeRaw : null,
    errorMessage: typeof messageRaw === 'string' ? messageRaw.slice(0, 120) : null,
  };
}

export const saveUserProfile = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request) => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as SaveUserProfileData;

      const normalizedUsername = normalizeUsername(data.username);
      if (normalizedUsername == null) {
        throw new HttpsError('invalid-argument', 'username is required');
      }

      const validationError = validateNormalizedUsername(normalizedUsername);
      if (validationError) {
        throw new HttpsError('invalid-argument', validationError);
      }

      const displayName = asDisplayNameOrFallback(data.displayName, normalizedUsername);
      const photoUrl = asOptionalPhotoUrl(data.photoUrl);
      const authEmail = asOptionalEmail(request.auth?.token?.email);
      const email = authEmail ?? asOptionalEmail(data.email);
      const birthDate = asOptionalBirthDate(data.birthDate);
      const zodiacSign = asOptionalZodiacSign(data.zodiacSign);
      const description = asOptionalTrimmedString(data.description);
      const descriptionProvided = asBoolean(data.descriptionProvided);
      const birthEssenceSummary = asOptionalBirthEssenceSummary(data.birthEssenceSummary);
      // Keep the existing epoch-millis field for sync compatibility, but do not trust
      // arbitrary client values outside a conservative timestamp range.
      const updatedAtEpochMillis = safeClientUpdatedAtEpochMillis(data.updatedAtEpochMillis, Date.now());

      logger.info('saveUserProfile normalized payload', sanitizedProfileLog({
        uid,
        username: normalizedUsername,
        displayName,
        photoUrl,
        email,
        birthDate,
        zodiacSign,
        description,
        birthEssenceSummary,
        updatedAtEpochMillis,
      }));

      const db = getFirestore();
      const profileRef = db.collection('users').doc(uid).collection('profile').doc('current');

      const profilePatch: FirebaseFirestore.UpdateData<UserProfileDoc> = {
        updatedAtEpochMillis,
        updatedAt: Timestamp.now(),
      };

      profilePatch.displayName = displayName;
      profilePatch.username = normalizedUsername;
      if (photoUrl != null) profilePatch.photoUrl = photoUrl;
      if (email != null) profilePatch.email = email;
      if (birthDate != null) profilePatch.birthDate = birthDate;
      if (zodiacSign != null) profilePatch.zodiacSign = zodiacSign;
      if (descriptionProvided) {
        if (description != null) profilePatch.description = description.slice(0, DESCRIPTION_MAX_LENGTH);
        else profilePatch.description = FieldValue.delete() as unknown as string;
      }
      if (birthEssenceSummary != null) profilePatch.birthEssenceSummary = birthEssenceSummary;

      try {
        await db.runTransaction(async (tx) => {
          const profileSnap = await tx.get(profileRef);
          const currentProfile = profileSnap.data() as UserProfileDoc | undefined;
          const currentNormalizedUsername = normalizeUsername(currentProfile?.username);

          if (currentNormalizedUsername === normalizedUsername) {
            tx.set(profileRef, profilePatch, {merge: true});
            return;
          }

          const newUsernameRef = db.collection('usernames').doc(normalizedUsername);
          const newUsernameSnap = await tx.get(newUsernameRef);

          if (newUsernameSnap.exists) {
            const ownerUid = (newUsernameSnap.data() as UsernameIndexDoc | undefined)?.uid;
            if (ownerUid !== uid) {
              throw new HttpsError('failed-precondition', 'username_taken');
            }
          }

          const usernameIndexWrite: UsernameIndexWrite = {
            uid,
            username: normalizedUsername,
            updatedAt: FieldValue.serverTimestamp(),
          };
          tx.set(newUsernameRef, usernameIndexWrite);

          if (currentNormalizedUsername != null) {
            const oldUsernameRef = db.collection('usernames').doc(currentNormalizedUsername);
            const oldUsernameSnap = await tx.get(oldUsernameRef);
            if (oldUsernameSnap.exists) {
              const ownerUid = (oldUsernameSnap.data() as UsernameIndexDoc | undefined)?.uid;
              if (ownerUid === uid) {
                tx.delete(oldUsernameRef);
              }
            }
          }

          tx.set(profileRef, profilePatch, {merge: true});
        });
      } catch (error) {
        if (isHttpsError(error)) throw error;

        const errorInfo = safeErrorInfo(error);
        logger.error('saveUserProfile transaction failed', {
          uidTag: safeUidTag(uid),
          hasUid: uid.length > 0,
          hasUsername: normalizedUsername.length > 0,
          usernameLength: normalizedUsername.length,
          errorCode: errorInfo.errorCode,
          errorMessage: errorInfo.errorMessage,
        });

        throw new HttpsError('internal', 'save_user_profile_failed');
      }

      return {
        username: normalizedUsername,
      };
    }
);
