const SAFE_ERROR_MESSAGE_MAX_LENGTH = 120;

export function buildUidTag(uid?: string): string {
  if (!uid) return 'anon';
  if (uid.length <= 8) return `${uid.slice(0, 2)}***`;
  return `${uid.slice(0, 4)}***${uid.slice(-2)}`;
}

export function safeErrorMessage(error: unknown): string {
  const raw = error instanceof Error ? error.message : String(error ?? '');
  if (raw.includes('DAILY_LLM_CAP_EXCEEDED')) return 'DAILY_LLM_CAP_EXCEEDED';
  return raw.slice(0, SAFE_ERROR_MESSAGE_MAX_LENGTH);
}

export function safeErrorMetadata(error: unknown): {name: string | null; message: string; code: string | null} {
  const safeMessage = safeErrorMessage(error);
  if (error instanceof Error) {
    const maybeCode = (error as {code?: unknown}).code;
    return {
      name: error.name || null,
      message: safeMessage,
      code: typeof maybeCode === 'string' ? maybeCode.slice(0, 64) : null,
    };
  }

  if (error && typeof error === 'object') {
    const maybeCode = (error as {code?: unknown}).code;
    return {
      name: null,
      message: safeMessage,
      code: typeof maybeCode === 'string' ? maybeCode.slice(0, 64) : null,
    };
  }

  return {
    name: null,
    message: safeMessage,
    code: null,
  };
}
