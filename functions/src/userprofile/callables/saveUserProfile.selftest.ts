import test from 'node:test';
import assert from 'node:assert/strict';
import {HttpsError} from 'firebase-functions/v2/https';
import {normalizeUsername, validateNormalizedUsername} from '../username';
import {__testables} from './saveUserProfile';

test('displayName falls back to normalized username when missing/blank', () => {
  assert.equal(__testables.asDisplayNameOrFallback(undefined, 'witch.user'), 'witch.user');
  assert.equal(__testables.asDisplayNameOrFallback('   ', 'witch.user'), 'witch.user');
  assert.equal(__testables.asDisplayNameOrFallback('  Luna  ', 'witch.user'), 'Luna');
});

test('username validation enforces required value', () => {
  const normalizedMissing = normalizeUsername(undefined);
  assert.equal(normalizedMissing, null);

  const toHttpsError = () => {
    if (normalizedMissing == null) {
      throw new HttpsError('invalid-argument', 'username is required');
    }
  };

  assert.throws(toHttpsError, (error: unknown) => {
    assert.ok(error instanceof HttpsError);
    assert.equal(error.code, 'invalid-argument');
    assert.equal(error.message, 'username is required');
    return true;
  });
});

test('optional fields are normalized to null (never undefined)', () => {
  assert.equal(__testables.asOptionalTrimmedString(undefined), null);
  assert.equal(__testables.asOptionalTrimmedString('  '), null);
  assert.equal(__testables.asOptionalBirthDate(undefined), null);
  assert.equal(__testables.asOptionalBirthEssenceSummary(undefined), null);
  assert.equal(__testables.asOptionalLong(undefined), null);
});

test('sanitized log omits sensitive values and tracks presence flags', () => {
  const logEntry = __testables.sanitizedProfileLog({
    uid: 'uid-123',
    username: 'witch.user',
    displayName: null,
    photoUrl: 'https://cdn/avatar.png',
    email: 'user@example.com',
    birthDate: null,
    zodiacSign: 'aries',
    birthEssenceSummary: null,
    updatedAtEpochMillis: 123456,
  });

  assert.deepEqual(logEntry, {
    uid: 'uid-123',
    hasUsername: true,
    usernameLength: 10,
    hasDisplayName: false,
    hasPhotoUrl: true,
    hasEmail: true,
    hasBirthDate: false,
    hasZodiacSign: true,
    hasBirthEssenceSummary: false,
    updatedAtEpochMillis: 123456,
  });
});

test('normalized username still passes format validation', () => {
  const normalized = normalizeUsername('  @Witch.User  ');
  assert.equal(normalized, 'witch.user');
  assert.equal(validateNormalizedUsername(normalized!), null);
});
