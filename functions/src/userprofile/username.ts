const USERNAME_REGEX = /^[a-z0-9._]+$/;

export const USERNAME_MIN_LENGTH = 3;
export const USERNAME_MAX_LENGTH = 30;

export function normalizeUsername(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;

  const trimmed = raw.trim();
  if (!trimmed) return null;

  const withoutAt = trimmed.startsWith('@') ? trimmed.slice(1) : trimmed;
  const normalized = withoutAt.trim().toLowerCase();

  return normalized.length > 0 ? normalized : null;
}

export function validateNormalizedUsername(username: string): string | null {
  if (username.length < USERNAME_MIN_LENGTH || username.length > USERNAME_MAX_LENGTH) {
    return `username must be ${USERNAME_MIN_LENGTH}-${USERNAME_MAX_LENGTH} chars`;
  }
  if (!USERNAME_REGEX.test(username)) {
    return 'username may only contain letters, digits, . and _';
  }
  return null;
}
