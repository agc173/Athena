import {GoogleAuth} from 'google-auth-library';
import type {AndroidPublisherProvider, GooglePlayValidationResult} from './types';

const ANDROID_PUBLISHER_SCOPE = 'https://www.googleapis.com/auth/androidpublisher';

type SubscriptionLineItem = {
  productId?: string;
  expiryTime?: string;
  offerDetails?: {
    basePlanId?: string;
  };
};

type SubscriptionPurchaseV2 = {
  subscriptionState?: string;
  lineItems?: SubscriptionLineItem[];
};

function toMillis(value: string | undefined): number | null {
  if (!value) return null;
  const millis = Date.parse(value);
  return Number.isFinite(millis) ? millis : null;
}

function pickLineItem(
    purchase: SubscriptionPurchaseV2,
    productId: string,
    basePlanId: string | null
): SubscriptionLineItem | null {
  const lineItems = purchase.lineItems ?? [];
  return lineItems.find((item) => {
    if (item.productId !== productId) return false;
    if (basePlanId != null && item.offerDetails?.basePlanId !== basePlanId) return false;
    return true;
  }) ?? null;
}

export class GooglePlayAndroidPublisherProvider implements AndroidPublisherProvider {
  private readonly auth = new GoogleAuth({scopes: [ANDROID_PUBLISHER_SCOPE]});

  async validateSubscription(input: {
    packageName: string;
    productId: string;
    basePlanId: string | null;
    purchaseToken: string;
  }): Promise<GooglePlayValidationResult> {
    const client = await this.auth.getClient();
    const encodedPackageName = encodeURIComponent(input.packageName);
    const encodedToken = encodeURIComponent(input.purchaseToken);
    const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodedPackageName}/purchases/subscriptionsv2/tokens/${encodedToken}`;

    let purchase: SubscriptionPurchaseV2;
    try {
      const response = await client.request<SubscriptionPurchaseV2>({url, method: 'GET'});
      purchase = response.data;
    } catch (error) {
      const status = typeof error === 'object' && error != null && 'response' in error ?
        (error as {response?: {status?: unknown}}).response?.status : undefined;
      if (status === 400 || status === 404 || status === 410) {
        return {
          active: false,
          status: 'GOOGLE_PLAY_TOKEN_INVALID',
          productId: null,
          basePlanId: null,
          expiresAtMillis: null,
        };
      }
      throw error;
    }

    const lineItem = pickLineItem(purchase, input.productId, input.basePlanId);
    const expiresAtMillis = toMillis(lineItem?.expiryTime);
    const isUnexpired = expiresAtMillis != null && expiresAtMillis > Date.now();
    const isActive = purchase.subscriptionState === 'SUBSCRIPTION_STATE_ACTIVE' && lineItem != null && isUnexpired;

    return {
      active: isActive,
      status: purchase.subscriptionState ?? 'SUBSCRIPTION_STATE_UNSPECIFIED',
      productId: lineItem?.productId ?? null,
      basePlanId: lineItem?.offerDetails?.basePlanId ?? null,
      expiresAtMillis,
    };
  }
}

export const googlePlayAndroidPublisherProvider = new GooglePlayAndroidPublisherProvider();
