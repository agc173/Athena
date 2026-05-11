import {HttpsError} from 'firebase-functions/v2/https';
import type {GooglePlayValidationInput, GooglePlayValidationResult} from './types';

export type GooglePlayValidationMode = 'mock' | 'real';

export type GooglePlayValidator = {
  validateSubscription(input: GooglePlayValidationInput): Promise<GooglePlayValidationResult>;
  acknowledgeSubscription?(input: GooglePlayValidationInput): Promise<{acknowledged: boolean; error?: string}>;
};

function nowPlus(days: number): string {
  return new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString();
}

function normalizeMockState(input: GooglePlayValidationInput): string {
  const requested = (input.clientPurchaseState ?? '').trim().toLowerCase();
  if (requested) return requested;

  const token = input.purchaseToken.toLowerCase();
  if (token.includes('pending')) return 'pending';
  if (token.includes('expired')) return 'expired';
  if (token.includes('grace')) return 'grace_period';
  if (token.includes('hold')) return 'account_hold';
  if (token.includes('revoked')) return 'revoked';
  return 'active';
}

function mockStateToRawState(state: string): string {
  switch (state) {
  case 'active':
    return 'SUBSCRIPTION_STATE_ACTIVE';
  case 'pending':
    return 'SUBSCRIPTION_STATE_PENDING';
  case 'expired':
    return 'SUBSCRIPTION_STATE_EXPIRED';
  case 'grace_period':
  case 'grace':
    return 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD';
  case 'account_hold':
  case 'hold':
    return 'SUBSCRIPTION_STATE_ON_HOLD';
  case 'paused':
    return 'SUBSCRIPTION_STATE_PAUSED';
  case 'canceled':
  case 'cancelled':
    return 'SUBSCRIPTION_STATE_CANCELED';
  case 'revoked':
    return 'REVOKED';
  default:
    return 'SUBSCRIPTION_STATE_UNSPECIFIED';
  }
}

export class MockGooglePlayValidator implements GooglePlayValidator {
  async validateSubscription(input: GooglePlayValidationInput): Promise<GooglePlayValidationResult> {
    const mockState = normalizeMockState(input);
    const rawState = mockStateToRawState(mockState);
    const activeLike = rawState === 'SUBSCRIPTION_STATE_ACTIVE' || rawState === 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD';

    return {
      environment: 'test',
      packageName: input.packageName,
      productId: input.productId,
      basePlanId: input.basePlanId,
      offerId: input.offerId,
      rawState,
      acknowledgementState: input.clientAcknowledged === false ? 'ACKNOWLEDGEMENT_STATE_PENDING' : 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED',
      lineItemExpiryTime: activeLike ? nowPlus(rawState === 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD' ? 3 : 30) : nowPlus(-1),
      autoRenewing: rawState === 'SUBSCRIPTION_STATE_ACTIVE',
      cancelReason: rawState === 'SUBSCRIPTION_STATE_CANCELED' ? 'USER_CANCELED' : undefined,
      orderId: 'mock-order-redacted',
      latestOrderId: 'mock-latest-order-redacted',
      regionCode: 'ZZ',
      testPurchase: true,
      startedAtMillis: Date.now() - 60 * 60 * 1000,
    };
  }

  async acknowledgeSubscription(input: GooglePlayValidationInput): Promise<{acknowledged: boolean}> {
    return {acknowledged: input.clientAcknowledged !== false};
  }
}

type AndroidPublisherSubscriptionV2 = {
  subscriptionState?: string;
  acknowledgementState?: string;
  latestOrderId?: string;
  regionCode?: string;
  testPurchase?: Record<string, unknown>;
  startTime?: string;
  linkedPurchaseToken?: string;
  canceledStateContext?: {
    userInitiatedCancellation?: {cancelSurveyResult?: {reason?: string}};
    systemInitiatedCancellation?: Record<string, unknown>;
    developerInitiatedCancellation?: Record<string, unknown>;
    replacementCancellation?: Record<string, unknown>;
  };
  lineItems?: Array<{
    productId?: string;
    expiryTime?: string;
    autoRenewingPlan?: {autoRenewEnabled?: boolean};
    offerDetails?: {basePlanId?: string; offerId?: string};
  }>;
};

export class RealGooglePlayValidator implements GooglePlayValidator {
  constructor(private readonly accessToken: string | undefined) {}

  async validateSubscription(input: GooglePlayValidationInput): Promise<GooglePlayValidationResult> {
    if (!this.accessToken) {
      throw new HttpsError(
          'failed-precondition',
          'Google Play validation is not configured. Set GOOGLE_PLAY_ACCESS_TOKEN or switch GOOGLE_PLAY_VALIDATION_MODE=mock for emulator/dev.'
      );
    }

    const url = new URL(
        `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(input.packageName)}` +
        `/purchases/subscriptionsv2/tokens/${encodeURIComponent(input.purchaseToken)}`
    );

    const response = await fetch(url, {
      method: 'GET',
      headers: {Authorization: `Bearer ${this.accessToken}`},
    });

    if (!response.ok) {
      throw new HttpsError('failed-precondition', `Google Play validation failed with HTTP ${response.status}`);
    }

    const body = await response.json() as AndroidPublisherSubscriptionV2;
    const lineItem = body.lineItems?.find((item) => item.productId === input.productId) ?? body.lineItems?.[0];

    return {
      environment: body.testPurchase ? 'sandbox' : 'production',
      packageName: input.packageName,
      productId: lineItem?.productId ?? input.productId,
      basePlanId: lineItem?.offerDetails?.basePlanId ?? input.basePlanId,
      offerId: lineItem?.offerDetails?.offerId ?? input.offerId,
      rawState: body.subscriptionState,
      acknowledgementState: body.acknowledgementState,
      lineItemExpiryTime: lineItem?.expiryTime,
      autoRenewing: lineItem?.autoRenewingPlan?.autoRenewEnabled,
      cancelReason: deriveCancelReason(body),
      latestOrderId: body.latestOrderId,
      regionCode: body.regionCode,
      testPurchase: Boolean(body.testPurchase),
      linkedPurchaseToken: body.linkedPurchaseToken,
      startedAtMillis: body.startTime ? Date.parse(body.startTime) : undefined,
    };
  }
}

function deriveCancelReason(body: AndroidPublisherSubscriptionV2): string | undefined {
  const context = body.canceledStateContext;
  if (!context) return undefined;
  if (context.userInitiatedCancellation) {
    return context.userInitiatedCancellation.cancelSurveyResult?.reason ?? 'USER_CANCELED';
  }
  if (context.systemInitiatedCancellation) return 'SYSTEM_CANCELED';
  if (context.developerInitiatedCancellation) return 'DEVELOPER_CANCELED';
  if (context.replacementCancellation) return 'REPLACED';
  return 'UNKNOWN_CANCELLATION';
}

export function buildGooglePlayValidator(params?: {
  mode?: string;
  accessToken?: string;
}): GooglePlayValidator {
  const mode = (params?.mode ?? process.env.GOOGLE_PLAY_VALIDATION_MODE ?? 'real').toLowerCase();
  if (mode === 'mock') return new MockGooglePlayValidator();
  return new RealGooglePlayValidator(params?.accessToken ?? process.env.GOOGLE_PLAY_ACCESS_TOKEN);
}
