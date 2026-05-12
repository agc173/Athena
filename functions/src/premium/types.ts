import type {Timestamp} from 'firebase-admin/firestore';

export type GooglePlayPurchasePayload = {
  productId?: unknown;
  basePlanId?: unknown;
  purchaseToken?: unknown;
  purchaseState?: unknown;
  isAcknowledged?: unknown;
  orderId?: unknown;
  packageName?: unknown;
};

export type PremiumStatus = 'active' | 'inactive';

export type PremiumEntitlementResponse = {
  isActive: boolean;
  active: boolean;
  status: PremiumStatus;
  productId: string | null;
  planType: 'monthly' | null;
};

export type GooglePlayValidationResult = {
  active: boolean;
  status: string;
  productId: string | null;
  basePlanId: string | null;
  expiresAtMillis: number | null;
};

export type AndroidPublisherProvider = {
  validateSubscription(input: {
    packageName: string;
    productId: string;
    basePlanId: string | null;
    purchaseToken: string;
  }): Promise<GooglePlayValidationResult>;
};

export type UserEntitlementDoc = {
  isSubscriber?: boolean;
  status?: string;
  productId?: string;
  basePlanId?: string;
  source?: string;
  purchaseTokenHash?: string;
  packageName?: string;
  updatedAt?: Timestamp;
  expiresAt?: Timestamp;
};

export type PurchaseTokenIndexDoc = {
  uid?: string;
  productId?: string;
  packageName?: string;
  updatedAt?: Timestamp;
};
