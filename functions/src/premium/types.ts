import type {Timestamp} from 'firebase-admin/firestore';

export type PremiumTier = 'free' | 'premium';
export type EntitlementPlatform = 'google_play' | 'app_store' | 'admin' | 'none';
export type EntitlementEnvironment = 'production' | 'sandbox' | 'test';
export type EntitlementSource = 'google_play_validation' | 'restore' | 'refresh' | 'admin';

export type SubscriptionStatus =
  | 'NONE'
  | 'ACTIVE'
  | 'PENDING'
  | 'EXPIRED'
  | 'CANCELED'
  | 'GRACE_PERIOD'
  | 'ACCOUNT_HOLD'
  | 'PAUSED'
  | 'REVOKED'
  | 'UNKNOWN';

export type PremiumEntitlementDto = {
  isSubscriber: boolean;
  tier: PremiumTier;
  status: SubscriptionStatus;
  productId?: string;
  basePlanId?: string;
  platform?: EntitlementPlatform;
  environment?: EntitlementEnvironment;
  premiumUntilIso?: string;
  autoRenewing?: boolean;
  lastValidatedAtIso?: string;
  gracePeriodUntilIso?: string;
  needsRestore?: boolean;
  updatedAtIso?: string;
};

export type UserEntitlementDoc = {
  isSubscriber: boolean;
  tier: PremiumTier;
  platform: EntitlementPlatform;
  environment: EntitlementEnvironment;
  productId?: string;
  basePlanId?: string;
  offerId?: string;
  purchaseTokenHash?: string;
  activeReceiptPath?: string;
  subscriptionStatus: SubscriptionStatus;
  premiumUntil?: Timestamp;
  autoRenewing?: boolean;
  startedAt?: Timestamp;
  lastValidatedAt?: Timestamp;
  gracePeriodUntil?: Timestamp;
  cancelReason?: string;
  source: EntitlementSource;
  updatedAt: Timestamp;
  createdAt?: Timestamp;
  schemaVersion: 1;
};

export type PurchaseReceiptDoc = {
  uid: string;
  purchaseTokenHash: string;
  platform: 'google_play';
  environment: EntitlementEnvironment;
  packageName: string;
  productId: string;
  basePlanId?: string;
  offerId?: string;
  subscriptionStatus: SubscriptionStatus;
  linkedPurchaseTokenHash?: string;
  google: {
    rawState?: string;
    acknowledgementState?: string;
    lineItemExpiryTime?: string;
    autoRenewing?: boolean;
    cancelReason?: string;
    orderId?: string;
    latestOrderId?: string;
    regionCode?: string;
    testPurchase?: boolean;
  };
  acknowledged: boolean;
  acknowledgedAt?: Timestamp;
  acknowledgementError?: string;
  premiumUntil?: Timestamp;
  startedAt?: Timestamp;
  lastValidatedAt: Timestamp;
  firstSeenAt: Timestamp;
  updatedAt: Timestamp;
  validationCount: number;
  lastValidationRequestId?: string;
  schemaVersion: 1;
};

export type PurchaseTokenIndexDoc = {
  uid: string;
  receiptPath: string;
  productId: string;
  platform: 'google_play';
  firstSeenAt: Timestamp;
  updatedAt: Timestamp;
};

export type GooglePlayValidationInput = {
  uid: string;
  productId: string;
  purchaseToken: string;
  packageName: string;
  basePlanId?: string;
  offerId?: string;
  clientPurchaseState?: string;
  clientAcknowledged?: boolean;
};

export type GooglePlayValidationResult = {
  environment: EntitlementEnvironment;
  packageName: string;
  productId: string;
  basePlanId?: string;
  offerId?: string;
  rawState?: string;
  acknowledgementState?: string;
  lineItemExpiryTime?: string;
  autoRenewing?: boolean;
  cancelReason?: string;
  orderId?: string;
  latestOrderId?: string;
  regionCode?: string;
  testPurchase?: boolean;
  linkedPurchaseToken?: string;
  startedAtMillis?: number;
};
