import {test} from 'node:test';
import assert from 'node:assert/strict';
import {buildBasicNatalPreview} from './callables/getEconomyModulePreviews';

test('basic natal preview shows weekly free while unused', () => {
  const preview = buildBasicNatalPreview({
    isPremium: false,
    balance: 0,
    dailyUsage: {},
    weeklyUsage: {basicNatalFreeUsed: 0},
  });
  assert.equal(preview.module, 'BASIC_NATAL_CHART');
  assert.equal(preview.nextSource, 'FREE');
  assert.equal(preview.cost, 0);
  assert.equal(preview.canExecute, true);
  assert.equal(preview.freeRemaining, 1);
});

test('basic natal preview shows one moon after weekly free with balance', () => {
  const preview = buildBasicNatalPreview({
    isPremium: false,
    balance: 1,
    dailyUsage: {},
    weeklyUsage: {basicNatalFreeUsed: 1},
  });
  assert.equal(preview.module, 'BASIC_NATAL_CHART');
  assert.equal(preview.nextSource, 'MOON');
  assert.equal(preview.cost, 1);
  assert.equal(preview.canExecute, true);
  assert.equal(preview.freeRemaining, 0);
});

test('basic natal preview rejects insufficient moons after weekly free without balance', () => {
  const preview = buildBasicNatalPreview({
    isPremium: false,
    balance: 0,
    dailyUsage: {},
    weeklyUsage: {basicNatalFreeUsed: 1},
  });
  assert.equal(preview.module, 'BASIC_NATAL_CHART');
  assert.equal(preview.nextSource, 'REJECTED');
  assert.equal(preview.cost, 1);
  assert.equal(preview.canExecute, false);
  assert.equal(preview.reasonIfRejected, 'insufficient_moons');
});


test('basic natal preview shows premium included while daily allowance remains', () => {
  const preview = buildBasicNatalPreview({
    isPremium: true,
    balance: 0,
    dailyUsage: {basicNatalPremiumUsed: 9},
    weeklyUsage: {basicNatalFreeUsed: 1},
  });
  assert.equal(preview.nextSource, 'PREMIUM');
  assert.equal(preview.cost, 0);
  assert.equal(preview.canExecute, true);
  assert.equal(preview.premiumRemaining, 1);
  assert.equal(preview.dailyCap, undefined);
});

test('basic natal preview shows one moon after premium daily included with balance', () => {
  const preview = buildBasicNatalPreview({
    isPremium: true,
    balance: 1,
    dailyUsage: {basicNatalPremiumUsed: 10},
    weeklyUsage: {basicNatalFreeUsed: 1},
  });
  assert.equal(preview.nextSource, 'MOON');
  assert.equal(preview.cost, 1);
  assert.equal(preview.canExecute, true);
  assert.equal(preview.reasonIfRejected, undefined);
  assert.equal(preview.dailyCap, undefined);
});

test('basic natal preview rejects insufficient moons after premium daily included without daily limit', () => {
  const preview = buildBasicNatalPreview({
    isPremium: true,
    balance: 0,
    dailyUsage: {basicNatalPremiumUsed: 10},
    weeklyUsage: {basicNatalFreeUsed: 1},
  });
  assert.equal(preview.nextSource, 'REJECTED');
  assert.equal(preview.cost, 1);
  assert.equal(preview.canExecute, false);
  assert.equal(preview.reasonIfRejected, 'insufficient_moons');
  assert.equal(preview.dailyCap, undefined);
});
