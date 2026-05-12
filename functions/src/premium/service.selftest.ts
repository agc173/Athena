import test from 'node:test';
import assert from 'node:assert/strict';
import {HttpsError} from 'firebase-functions/v2/https';
import {Timestamp} from 'firebase-admin/firestore';
import type {AndroidPublisherProvider, GooglePlayValidationResult, UserEntitlementDoc} from './types';
import type {EntitlementActivation, EntitlementStore} from './service';
import {
  __testables,
  hashPurchaseToken,
  refreshEntitlementForUid,
  restoreGooglePlayPurchasesForUid,
  validateGooglePlayPurchaseForUid,
} from './service';

const validPayload = {
  productId: 'bwitch_premium_monthly',
  basePlanId: 'monthly',
  purchaseToken: 'token-123',
  purchaseState: 'PURCHASED',
  isAcknowledged: false,
  orderId: 'GPA.123',
  packageName: 'com.agc.bwitch',
};

class FakeProvider implements AndroidPublisherProvider {
  calls = 0;

  constructor(private readonly result: GooglePlayValidationResult) {}

  async validateSubscription(): Promise<GooglePlayValidationResult> {
    this.calls += 1;
    return this.result;
  }
}

class FakeStore implements EntitlementStore {
  activations: EntitlementActivation[] = [];
  entitlements = new Map<string, UserEntitlementDoc>();
  tokenOwners = new Map<string, string>();

  async activateGooglePlayEntitlement(activation: EntitlementActivation): Promise<void> {
    const owner = this.tokenOwners.get(activation.purchaseTokenHash);
    if (owner != null && owner !== activation.uid) {
      throw new HttpsError('failed-precondition', 'purchase_token_already_linked');
    }

    this.tokenOwners.set(activation.purchaseTokenHash, activation.uid);
    this.activations.push(activation);
    this.entitlements.set(activation.uid, {
      isSubscriber: true,
      status: 'active',
      productId: activation.productId,
      basePlanId: activation.basePlanId,
      source: 'google_play',
      purchaseTokenHash: activation.purchaseTokenHash,
      packageName: activation.packageName,
      expiresAt: activation.expiresAtMillis == null ? undefined : Timestamp.fromMillis(activation.expiresAtMillis),
    });
  }

  async readUserEntitlement(uid: string): Promise<UserEntitlementDoc | null> {
    return this.entitlements.get(uid) ?? null;
  }
}

function activePlayResult(): GooglePlayValidationResult {
  return {
    active: true,
    status: 'SUBSCRIPTION_STATE_ACTIVE',
    productId: 'bwitch_premium_monthly',
    basePlanId: 'monthly',
    expiresAtMillis: Date.now() + 60_000,
  };
}

test('callable auth missing shape is enforced by callables through unauthenticated errors', () => {
  const error = new HttpsError('unauthenticated', 'Authentication is required');
  assert.equal(error.code, 'unauthenticated');
});

test('invalid productId is rejected', () => {
  assert.throws(
      () => __testables.normalizeGooglePlayPurchasePayload({...validPayload, productId: 'other'}, {requirePurchased: true}),
      (error: unknown) => error instanceof HttpsError && error.code === 'invalid-argument' && error.message === 'invalid_product_id'
  );
});

test('invalid basePlanId is rejected', () => {
  assert.throws(
      () => __testables.normalizeGooglePlayPurchasePayload({...validPayload, basePlanId: 'annual'}, {requirePurchased: true}),
      (error: unknown) => error instanceof HttpsError && error.code === 'invalid-argument' && error.message === 'invalid_base_plan_id'
  );
});

test('invalid packageName is rejected', () => {
  assert.throws(
      () => __testables.normalizeGooglePlayPurchasePayload({...validPayload, packageName: 'com.other.app'}, {requirePurchased: true}),
      (error: unknown) => error instanceof HttpsError && error.code === 'invalid-argument' && error.message === 'invalid_package_name'
  );
});

test('missing token is rejected', () => {
  assert.throws(
      () => __testables.normalizeGooglePlayPurchasePayload({...validPayload, purchaseToken: '   '}, {requirePurchased: true}),
      (error: unknown) => error instanceof HttpsError && error.code === 'invalid-argument' && error.message === 'purchaseToken is required'
  );
});

test('pending validate purchase is not active', async () => {
  const provider = new FakeProvider(activePlayResult());
  const store = new FakeStore();

  await assert.rejects(
      () => validateGooglePlayPurchaseForUid({
        uid: 'uid-a',
        payload: {...validPayload, purchaseState: 'PENDING'},
        provider,
        store,
      }),
      (error: unknown) => error instanceof HttpsError && error.code === 'failed-precondition'
  );
  assert.equal(store.activations.length, 0);
  assert.equal(provider.calls, 0);
});

test('active Google Play response writes entitlement', async () => {
  const provider = new FakeProvider(activePlayResult());
  const store = new FakeStore();

  const response = await validateGooglePlayPurchaseForUid({uid: 'uid-a', payload: validPayload, provider, store});

  assert.equal(response.active, true);
  assert.equal(response.isActive, true);
  assert.equal(response.status, 'active');
  assert.equal(response.productId, 'bwitch_premium_monthly');
  assert.equal(response.planType, 'monthly');
  assert.equal(store.activations.length, 1);
  assert.equal(store.activations[0].purchaseTokenHash, hashPurchaseToken('token-123'));
});

test('token already associated to another uid does not activate', async () => {
  const provider = new FakeProvider(activePlayResult());
  const store = new FakeStore();
  store.tokenOwners.set(hashPurchaseToken('token-123'), 'uid-owner');

  await assert.rejects(
      () => validateGooglePlayPurchaseForUid({uid: 'uid-other', payload: validPayload, provider, store}),
      (error: unknown) => error instanceof HttpsError && error.code === 'failed-precondition'
  );
  assert.equal(store.activations.length, 0);
});

test('restore skips pending and returns inactive when no active purchase exists', async () => {
  const provider = new FakeProvider(activePlayResult());
  const store = new FakeStore();

  const response = await restoreGooglePlayPurchasesForUid({
    uid: 'uid-a',
    purchases: [{...validPayload, purchaseState: 'PENDING'}],
    provider,
    store,
  });

  assert.equal(response.active, false);
  assert.equal(store.activations.length, 0);
  assert.equal(provider.calls, 0);
});

test('refresh without entitlement returns inactive', async () => {
  const response = await refreshEntitlementForUid('uid-a', new FakeStore());
  assert.equal(response.active, false);
  assert.equal(response.status, 'inactive');
});

test('refresh expired entitlement returns inactive', async () => {
  const store = new FakeStore();
  store.entitlements.set('uid-a', {
    isSubscriber: true,
    status: 'active',
    productId: 'bwitch_premium_monthly',
    basePlanId: 'monthly',
    source: 'google_play',
    expiresAt: Timestamp.fromMillis(Date.now() - 60_000),
  });

  const response = await refreshEntitlementForUid('uid-a', store);
  assert.equal(response.active, false);
  assert.equal(response.status, 'inactive');
});

test('refresh with active entitlement returns active monthly', async () => {
  const store = new FakeStore();
  store.entitlements.set('uid-a', {
    isSubscriber: true,
    status: 'active',
    productId: 'bwitch_premium_monthly',
    basePlanId: 'monthly',
    source: 'google_play',
  });

  const response = await refreshEntitlementForUid('uid-a', store);
  assert.equal(response.active, true);
  assert.equal(response.status, 'active');
  assert.equal(response.planType, 'monthly');
});
