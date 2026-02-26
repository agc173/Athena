import {logger} from './logger';

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function isRetryableError(e: unknown): boolean {
  const anyE: any = e;
  const message = String(anyE?.message ?? '').toLowerCase();
  const code = anyE?.code ?? anyE?.cause?.code;
  const status = anyE?.status ?? anyE?.response?.status;

  if ((anyE?.name ?? "") === "AbortError") return true;

  // Network transient errors
  if (code === 'ETIMEDOUT' || code === 'ECONNRESET' || code === 'EAI_AGAIN') {
    return true;
  }

  // HTTP status-based retry
  if (typeof status === 'number') {
    if (status === 429) return true;
    if (status >= 500 && status <= 599) return true;
  }

  // Text-based fallback (por si provider no expone status)
  if (message.includes(' 429') || message.includes('rate limit')) return true;
  if (
    message.includes(' 500') ||
    message.includes(' 502') ||
    message.includes(' 503') ||
    message.includes(' 504')
  ) return true;

  return false;
}

export async function withRetry<T>(
  fn: () => Promise<T>,
  maxRetries: number
): Promise<T> {
  let attempt = 0;

  while (true) {
    try {
      return await fn();
    } catch (e) {
      const retryable = isRetryableError(e);

      if (!retryable || attempt >= maxRetries) {
        logger.error('withRetry giving up', {
          attempt,
          maxRetries,
          retryable,
          error: String(e),
        });
        throw e;
      }

      attempt++;

      // Exponential backoff + jitter
      const baseDelay = 500; // 0.5s
      const backoff = baseDelay * Math.pow(2, attempt - 1);
      const jitter = Math.floor(Math.random() * 250);

      logger.warn('withRetry retrying', {
        attempt,
        maxRetries,
        backoffMs: backoff + jitter,
        error: String(e),
      });

      await sleep(backoff + jitter);
    }
  }
}
