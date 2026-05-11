import {Timestamp} from 'firebase-admin/firestore';
import type {
  GooglePlayValidationResult,
  PremiumEntitlementDto,
  SubscriptionStatus,
  UserEntitlementDoc,
} from './types';

const ACTIVE_STATUSES = new Set<SubscriptionStatus>(['ACTIVE', 'GRACE_PERIOD']);

export function isSubscriberStatus(status: SubscriptionStatus): boolean {
  return ACTIVE_STATUSES.has(status);
}

function isFutureIso(value: string | undefined, nowMillis: number): boolean {
  if (!value) return false;
  const expiryMillis = Date.parse(value);
  return Number.isFinite(expiryMillis) && expiryMillis > nowMillis;
}

export function mapGoogleStateToSubscriptionStatus(rawState: string | undefined): SubscriptionStatus {
  switch ((rawState ?? '').toUpperCase()) {
  case 'SUBSCRIPTION_STATE_ACTIVE':
  case 'ACTIVE':
    return 'ACTIVE';
  case 'SUBSCRIPTION_STATE_PENDING':
  case 'PENDING':
    return 'PENDING';
  case 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD':
  case 'GRACE_PERIOD':
    return 'GRACE_PERIOD';
  case 'SUBSCRIPTION_STATE_ON_HOLD':
  case 'ACCOUNT_HOLD':
    return 'ACCOUNT_HOLD';
  case 'SUBSCRIPTION_STATE_PAUSED':
  case 'PAUSED':
    return 'PAUSED';
  case 'SUBSCRIPTION_STATE_CANCELED':
  case 'CANCELED':
    return 'CANCELED';
  case 'SUBSCRIPTION_STATE_EXPIRED':
  case 'EXPIRED':
    return 'EXPIRED';
  case 'REVOKED':
    return 'REVOKED';
  case 'SUBSCRIPTION_STATE_UNSPECIFIED':
  case 'UNKNOWN':
    return 'UNKNOWN';
  default:
    return rawState ? 'UNKNOWN' : 'NONE';
  }
}


export function mapGoogleValidationToSubscriptionStatus(
    validation: GooglePlayValidationResult,
    nowMillis = Date.now()
): SubscriptionStatus {
  const rawState = (validation.rawState ?? '').toUpperCase();
  if (rawState === 'SUBSCRIPTION_STATE_CANCELED' || rawState === 'CANCELED') {
    return isFutureIso(validation.lineItemExpiryTime, nowMillis) ? 'ACTIVE' : 'EXPIRED';
  }

  return mapGoogleStateToSubscriptionStatus(validation.rawState);
}

function isGoogleCanceled(validation: GooglePlayValidationResult): boolean {
  const rawState = (validation.rawState ?? '').toUpperCase();
  return rawState === 'SUBSCRIPTION_STATE_CANCELED' || rawState === 'CANCELED';
}

export function entitlementPriority(doc: Pick<UserEntitlementDoc, 'isSubscriber' | 'premiumUntil' | 'lastValidatedAt'>): number {
  const premiumUntilMillis = doc.premiumUntil?.toMillis() ?? 0;
  const validatedMillis = doc.lastValidatedAt?.toMillis() ?? 0;
  return (doc.isSubscriber ? 9_000_000_000_000_000 : 0) + premiumUntilMillis + Math.floor(validatedMillis / 1000);
}

export function shouldReplaceEntitlement(
    current: UserEntitlementDoc | undefined,
    candidate: UserEntitlementDoc
): boolean {
  if (!current) return true;
  if (!current.isSubscriber && candidate.isSubscriber) return true;
  if (current.isSubscriber && !candidate.isSubscriber) return false;
  return entitlementPriority(candidate) >= entitlementPriority(current);
}

export function timestampFromIso(value: string | undefined): Timestamp | undefined {
  if (!value) return undefined;
  const millis = Date.parse(value);
  if (!Number.isFinite(millis)) return undefined;
  return Timestamp.fromMillis(millis);
}

export function timestampFromMillis(value: number | undefined): Timestamp | undefined {
  if (value == null || !Number.isFinite(value)) return undefined;
  return Timestamp.fromMillis(value);
}

function timestampToIso(value: Timestamp | undefined): string | undefined {
  return value?.toDate().toISOString();
}

export function toPremiumEntitlementDto(
    doc: UserEntitlementDoc | undefined,
    needsRestore = false
): PremiumEntitlementDto {
  if (!doc) {
    return {
      isSubscriber: false,
      tier: 'free',
      status: 'NONE',
      platform: 'none',
      needsRestore,
    };
  }

  return {
    isSubscriber: doc.isSubscriber === true,
    tier: doc.tier,
    status: doc.subscriptionStatus,
    productId: doc.productId,
    basePlanId: doc.basePlanId,
    platform: doc.platform,
    environment: doc.environment,
    premiumUntilIso: timestampToIso(doc.premiumUntil),
    autoRenewing: doc.autoRenewing,
    lastValidatedAtIso: timestampToIso(doc.lastValidatedAt),
    gracePeriodUntilIso: timestampToIso(doc.gracePeriodUntil),
    needsRestore,
    updatedAtIso: timestampToIso(doc.updatedAt),
  };
}

export function buildEntitlementFields(params: {
  validation: GooglePlayValidationResult;
  status: SubscriptionStatus;
  tokenHash: string;
  receiptPath: string;
  source: UserEntitlementDoc['source'];
  now: Timestamp;
  createdAt?: Timestamp;
}): UserEntitlementDoc {
  const premiumUntil = timestampFromIso(params.validation.lineItemExpiryTime);
  const startedAt = timestampFromMillis(params.validation.startedAtMillis);
  const isSubscriber = isSubscriberStatus(params.status);

  return {
    isSubscriber,
    tier: isSubscriber ? 'premium' : 'free',
    platform: 'google_play',
    environment: params.validation.environment,
    productId: params.validation.productId,
    basePlanId: params.validation.basePlanId,
    offerId: params.validation.offerId,
    purchaseTokenHash: params.tokenHash,
    activeReceiptPath: params.receiptPath,
    subscriptionStatus: params.status,
    premiumUntil,
    autoRenewing: params.status === 'ACTIVE' && isGoogleCanceled(params.validation) ? false : params.validation.autoRenewing,
    startedAt,
    lastValidatedAt: params.now,
    gracePeriodUntil: params.status === 'GRACE_PERIOD' ? premiumUntil : undefined,
    cancelReason: params.validation.cancelReason,
    source: params.source,
    updatedAt: params.now,
    createdAt: params.createdAt,
    schemaVersion: 1,
  };
}
