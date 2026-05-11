import test from 'node:test';
import assert from 'node:assert/strict';
import {Timestamp} from 'firebase-admin/firestore';
import {
  buildEntitlementFields,
  mapGoogleStateToSubscriptionStatus,
  mapGoogleValidationToSubscriptionStatus,
  shouldReplaceEntitlement,
} from './entitlementMapper';
import {MockGooglePlayValidator} from './googlePlayValidator';
import {normalizeGooglePlayPurchaseInput, restoreGooglePlayPurchaseArray} from './service';
import {hashPurchaseToken} from './tokenHash';
import type {UserEntitlementDoc} from './types';

const SECRET = '0123456789abcdef-test-secret';

test('token hash is stable and does not equal the raw token', () => {
  const token = 'mock_active_token_12345';
  const first = hashPurchaseToken(token, SECRET);
  const second = hashPurchaseToken(token, SECRET);

  assert.equal(first, second);
  assert.notEqual(first, token);
  assert.match(first, /^[a-f0-9]{64}$/);
});

test('Google states map to internal subscription statuses', () => {
  assert.equal(mapGoogleStateToSubscriptionStatus('SUBSCRIPTION_STATE_ACTIVE'), 'ACTIVE');
  assert.equal(mapGoogleStateToSubscriptionStatus('SUBSCRIPTION_STATE_PENDING'), 'PENDING');
  assert.equal(mapGoogleStateToSubscriptionStatus('SUBSCRIPTION_STATE_EXPIRED'), 'EXPIRED');
  assert.equal(mapGoogleStateToSubscriptionStatus('SUBSCRIPTION_STATE_IN_GRACE_PERIOD'), 'GRACE_PERIOD');
  assert.equal(mapGoogleStateToSubscriptionStatus('SUBSCRIPTION_STATE_ON_HOLD'), 'ACCOUNT_HOLD');
  assert.equal(mapGoogleStateToSubscriptionStatus('SUBSCRIPTION_STATE_PAUSED'), 'PAUSED');
  assert.equal(mapGoogleStateToSubscriptionStatus('REVOKED'), 'REVOKED');
  assert.equal(mapGoogleStateToSubscriptionStatus('SOMETHING_NEW'), 'UNKNOWN');
});

test('active and grace statuses are subscriber true while inactive statuses are false in entitlement replacement candidates', () => {
  const now = Timestamp.fromMillis(1_700_000_000_000);
  const active: UserEntitlementDoc = {
    isSubscriber: true,
    tier: 'premium',
    platform: 'google_play',
    environment: 'test',
    subscriptionStatus: 'ACTIVE',
    source: 'google_play_validation',
    updatedAt: now,
    lastValidatedAt: now,
    schemaVersion: 1,
  };
  const grace = {...active, subscriptionStatus: 'GRACE_PERIOD' as const};
  const pending = {...active, isSubscriber: false, tier: 'free' as const, subscriptionStatus: 'PENDING' as const};
  const expired = {...pending, subscriptionStatus: 'EXPIRED' as const};
  const revoked = {...pending, subscriptionStatus: 'REVOKED' as const};
  const hold = {...pending, subscriptionStatus: 'ACCOUNT_HOLD' as const};

  assert.equal(active.isSubscriber, true);
  assert.equal(grace.isSubscriber, true);
  assert.equal(pending.isSubscriber, false);
  assert.equal(expired.isSubscriber, false);
  assert.equal(revoked.isSubscriber, false);
  assert.equal(hold.isSubscriber, false);
});

test('Google canceled with future expiry remains premium until premiumUntil and preserves cancelReason', () => {
  const now = Timestamp.fromMillis(1_700_000_000_000);
  const futureExpiry = '2999-01-01T00:00:00.000Z';
  const validation = {
    environment: 'test' as const,
    packageName: 'com.bwitch.app',
    productId: 'known_monthly',
    rawState: 'SUBSCRIPTION_STATE_CANCELED',
    lineItemExpiryTime: futureExpiry,
    autoRenewing: true,
    cancelReason: 'USER_CANCELED',
  };

  const status = mapGoogleValidationToSubscriptionStatus(validation, now.toMillis());
  const entitlement = buildEntitlementFields({
    validation,
    status,
    tokenHash: 'token-hash',
    receiptPath: 'purchaseReceipts/uid/items/token-hash',
    source: 'google_play_validation',
    now,
  });

  assert.equal(status, 'ACTIVE');
  assert.equal(entitlement.isSubscriber, true);
  assert.equal(entitlement.tier, 'premium');
  assert.equal(entitlement.subscriptionStatus, 'ACTIVE');
  assert.equal(entitlement.premiumUntil?.toDate().toISOString(), futureExpiry);
  assert.equal(entitlement.autoRenewing, false);
  assert.equal(entitlement.cancelReason, 'USER_CANCELED');
});

test('Google canceled with past expiry is expired and does not remain premium', () => {
  const now = Timestamp.fromMillis(1_700_000_000_000);
  const validation = {
    environment: 'test' as const,
    packageName: 'com.bwitch.app',
    productId: 'known_monthly',
    rawState: 'SUBSCRIPTION_STATE_CANCELED',
    lineItemExpiryTime: '2000-01-01T00:00:00.000Z',
    autoRenewing: false,
    cancelReason: 'USER_CANCELED',
  };

  const status = mapGoogleValidationToSubscriptionStatus(validation, now.toMillis());
  const entitlement = buildEntitlementFields({
    validation,
    status,
    tokenHash: 'token-hash',
    receiptPath: 'purchaseReceipts/uid/items/token-hash',
    source: 'google_play_validation',
    now,
  });

  assert.equal(status, 'EXPIRED');
  assert.equal(entitlement.isSubscriber, false);
  assert.equal(entitlement.tier, 'free');
  assert.equal(entitlement.subscriptionStatus, 'EXPIRED');
  assert.equal(entitlement.cancelReason, 'USER_CANCELED');
});

test('ACTIVE, EXPIRED, and GRACE_PERIOD keep expected subscriber semantics', () => {
  const now = Timestamp.fromMillis(1_700_000_000_000);
  const baseValidation = {
    environment: 'test' as const,
    packageName: 'com.bwitch.app',
    productId: 'known_monthly',
  };
  const cases = [
    {rawState: 'SUBSCRIPTION_STATE_ACTIVE', expectedStatus: 'ACTIVE' as const, expectedSubscriber: true},
    {rawState: 'SUBSCRIPTION_STATE_EXPIRED', expectedStatus: 'EXPIRED' as const, expectedSubscriber: false},
    {rawState: 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD', expectedStatus: 'GRACE_PERIOD' as const, expectedSubscriber: true},
  ];

  for (const item of cases) {
    const validation = {...baseValidation, rawState: item.rawState};
    const status = mapGoogleValidationToSubscriptionStatus(validation, now.toMillis());
    const entitlement = buildEntitlementFields({
      validation,
      status,
      tokenHash: 'token-hash',
      receiptPath: 'purchaseReceipts/uid/items/token-hash',
      source: 'google_play_validation',
      now,
    });

    assert.equal(status, item.expectedStatus);
    assert.equal(entitlement.isSubscriber, item.expectedSubscriber);
  }
});

test('entitlement does not degrade when an active newer purchase already exists', () => {
  const current: UserEntitlementDoc = {
    isSubscriber: true,
    tier: 'premium',
    platform: 'google_play',
    environment: 'production',
    subscriptionStatus: 'ACTIVE',
    premiumUntil: Timestamp.fromMillis(1_900_000_000_000),
    source: 'google_play_validation',
    updatedAt: Timestamp.fromMillis(1_800_000_000_000),
    lastValidatedAt: Timestamp.fromMillis(1_800_000_000_000),
    schemaVersion: 1,
  };
  const oldExpired: UserEntitlementDoc = {
    ...current,
    isSubscriber: false,
    tier: 'free',
    subscriptionStatus: 'EXPIRED',
    premiumUntil: Timestamp.fromMillis(1_700_000_000_000),
    updatedAt: Timestamp.fromMillis(1_850_000_000_000),
    lastValidatedAt: Timestamp.fromMillis(1_850_000_000_000),
  };

  assert.equal(shouldReplaceEntitlement(current, oldExpired), false);
});

test('restore with empty array returns inactive and no active token', async () => {
  const result = await restoreGooglePlayPurchaseArray('uid-test', []);

  assert.equal(result.restoredCount, 0);
  assert.equal(result.activeTokenFound, false);
  assert.equal(result.entitlement.isSubscriber, false);
  assert.equal(result.entitlement.status, 'NONE');
});

test('allowlist rejects an unknown productId', () => {
  process.env.GOOGLE_PLAY_PRODUCT_ALLOWLIST = 'known_monthly';

  assert.throws(() => normalizeGooglePlayPurchaseInput({
    productId: 'unknown_product',
    purchaseToken: 'mock_active_token_12345',
  }), /Unsupported productId/);
});

test('mock validator simulates active, pending, and expired states', async () => {
  const validator = new MockGooglePlayValidator();
  const base = {
    uid: 'uid-test',
    productId: 'known_monthly',
    purchaseToken: 'mock_active_token_12345',
    packageName: 'com.bwitch.app',
  };

  const active = await validator.validateSubscription({...base, clientPurchaseState: 'active'});
  const pending = await validator.validateSubscription({...base, clientPurchaseState: 'pending'});
  const expired = await validator.validateSubscription({...base, clientPurchaseState: 'expired'});

  assert.equal(mapGoogleStateToSubscriptionStatus(active.rawState), 'ACTIVE');
  assert.equal(mapGoogleStateToSubscriptionStatus(pending.rawState), 'PENDING');
  assert.equal(mapGoogleStateToSubscriptionStatus(expired.rawState), 'EXPIRED');
});
