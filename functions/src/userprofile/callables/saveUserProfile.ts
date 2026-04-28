import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {normalizeUsername, validateNormalizedUsername} from '../username';

type SaveUserProfileData = {
  displayName?: unknown;
  photoUrl?: unknown;
  email?: unknown;
  username?: unknown;
  birthDate?: unknown;
  zodiacSign?: unknown;
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
  birthEssenceSummary?: string;
  updatedAtEpochMillis?: number;
  updatedAt?: Timestamp;
};

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

function asOptionalTrimmedString(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
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
  return text.slice(0, 120);
}

function asOptionalLong(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  return Math.trunc(value);
}

function asDisplayNameOrFallback(displayNameRaw: unknown, normalizedUsername: string): string {
  return asOptionalTrimmedString(displayNameRaw) ?? normalizedUsername;
}

export const __testables = {
  asOptionalTrimmedString,
  asOptionalBirthDate,
  asOptionalBirthEssenceSummary,
  asOptionalLong,
  asDisplayNameOrFallback,
};

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
      const photoUrl = asOptionalTrimmedString(data.photoUrl);
      const email = asOptionalTrimmedString(data.email);
      const birthDate = asOptionalBirthDate(data.birthDate);
      const zodiacSign = asOptionalTrimmedString(data.zodiacSign);
      const birthEssenceSummary = asOptionalBirthEssenceSummary(data.birthEssenceSummary);
      const updatedAtEpochMillis = asOptionalLong(data.updatedAtEpochMillis) ?? Date.now();

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
      if (birthEssenceSummary != null) profilePatch.birthEssenceSummary = birthEssenceSummary;

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

      return {
        username: normalizedUsername,
      };
    }
);
