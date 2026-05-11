import {createHmac, timingSafeEqual} from 'node:crypto';

const MIN_TOKEN_LENGTH = 12;
const MAX_TOKEN_LENGTH = 4096;

export function assertValidPurchaseToken(purchaseToken: unknown): string {
  if (typeof purchaseToken !== 'string') {
    throw new Error('purchaseToken is required');
  }

  const token = purchaseToken.trim();
  if (token.length < MIN_TOKEN_LENGTH) {
    throw new Error('purchaseToken is too short');
  }
  if (token.length > MAX_TOKEN_LENGTH) {
    throw new Error('purchaseToken is too long');
  }
  if (!/^[A-Za-z0-9._\-:]+$/.test(token)) {
    throw new Error('purchaseToken has invalid characters');
  }
  return token;
}

export function hashPurchaseToken(purchaseToken: string, secret: string): string {
  if (!secret || secret.trim().length < 16) {
    throw new Error('PURCHASE_TOKEN_HASH_SECRET must be at least 16 characters');
  }

  return createHmac('sha256', secret).update(purchaseToken).digest('hex');
}

export function safeHashEquals(a: string, b: string): boolean {
  const left = Buffer.from(a, 'hex');
  const right = Buffer.from(b, 'hex');
  if (left.length !== right.length) return false;
  return timingSafeEqual(left, right);
}
