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
