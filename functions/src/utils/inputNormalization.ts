export const ORACLE_QUESTION_MAX_LENGTH = 150;
export const DAILY_RITUAL_TEXT_MAX_LENGTH = 500;
export const BIRTH_ESSENCE_SUMMARY_MAX_LENGTH = 120;

// eslint-disable-next-line no-control-regex
const CONTROL_CHARS_UNSAFE_REGEX = /[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g;
const NON_BREAKING_SPACES_REGEX = /[\u00A0\u202F\u2007]/g;

export function removeUnsafeControlChars(input: string): string {
  return input.replace(CONTROL_CHARS_UNSAFE_REGEX, '');
}

export function normalizeSingleLineInput(input: string): string {
  const withoutControls = removeUnsafeControlChars(input)
      .replace(/\r\n?/g, ' ')
      .replace(NON_BREAKING_SPACES_REGEX, ' ');

  return withoutControls
      .replace(/\s+/g, ' ')
      .trim();
}

export function normalizeMultilineInput(input: string): string {
  const withoutControls = removeUnsafeControlChars(input)
      .replace(/\r\n?/g, '\n')
      .replace(NON_BREAKING_SPACES_REGEX, ' ');

  return withoutControls
      .replace(/[ \t]+\n/g, '\n')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
}
