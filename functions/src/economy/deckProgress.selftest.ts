import test from 'node:test';
import assert from 'node:assert/strict';
import {computeUnlockDelta, pickCardsDeterministically, shouldMarkCompletedAt} from './deckProgress';

test('5 moons grants 1 unlock with threshold 5', () => {
  const result = computeUnlockDelta({carryOverMoons: 0, moonCostCharged: 5, moonsPerUnlock: 5});
  assert.equal(result.newlyGrantedUnlocks, 1);
  assert.equal(result.carryOverMoons, 0);
});

test('10 moons grants 2 unlocks with threshold 5', () => {
  const result = computeUnlockDelta({carryOverMoons: 0, moonCostCharged: 10, moonsPerUnlock: 5});
  assert.equal(result.newlyGrantedUnlocks, 2);
  assert.equal(result.carryOverMoons, 0);
});

test('3 moons + 2 moons in separate requests grants 1 unlock and leaves 0 carry', () => {
  const first = computeUnlockDelta({carryOverMoons: 0, moonCostCharged: 3, moonsPerUnlock: 5});
  assert.equal(first.newlyGrantedUnlocks, 0);
  assert.equal(first.carryOverMoons, 3);

  const second = computeUnlockDelta({carryOverMoons: first.carryOverMoons, moonCostCharged: 2, moonsPerUnlock: 5});
  assert.equal(second.newlyGrantedUnlocks, 1);
  assert.equal(second.carryOverMoons, 0);
});

test('4 moons carry + 5 moons grants 1 unlock and leaves carry 4', () => {
  const result = computeUnlockDelta({carryOverMoons: 4, moonCostCharged: 5, moonsPerUnlock: 5});
  assert.equal(result.newlyGrantedUnlocks, 1);
  assert.equal(result.carryOverMoons, 4);
});

test('cross-module spends on the same track accumulate via carryOverMoons', () => {
  const moduleA = computeUnlockDelta({carryOverMoons: 0, moonCostCharged: 1, moonsPerUnlock: 5});
  const moduleB = computeUnlockDelta({carryOverMoons: moduleA.carryOverMoons, moonCostCharged: 2, moonsPerUnlock: 5});
  const moduleC = computeUnlockDelta({carryOverMoons: moduleB.carryOverMoons, moonCostCharged: 2, moonsPerUnlock: 5});

  assert.equal(moduleA.newlyGrantedUnlocks + moduleB.newlyGrantedUnlocks + moduleC.newlyGrantedUnlocks, 1);
  assert.equal(moduleC.carryOverMoons, 0);
});

test('invalid moonsPerUnlock grants 0 unlocks and keeps normalized carry', () => {
  const result = computeUnlockDelta({carryOverMoons: 2, moonCostCharged: 5, moonsPerUnlock: 0});
  assert.equal(result.newlyGrantedUnlocks, 0);
  assert.equal(result.carryOverMoons, 7);
});

test('deterministic picks have no duplicates', () => {
  const picks = pickCardsDeterministically({
    availableCardIds: ['c1', 'c2', 'c3', 'c4', 'c5'],
    picks: 3,
    seed: 'uid:req:track:1',
  });
  assert.equal(picks.length, 3);
  assert.equal(new Set(picks).size, 3);
});

test('deck complete does not exceed total cards', () => {
  const picks = pickCardsDeterministically({
    availableCardIds: [],
    picks: 5,
    seed: 'uid:req:track:2',
  });
  assert.deepEqual(picks, []);
});

test('same seed yields stable picks for retries/idempotency', () => {
  const available = ['c1', 'c2', 'c3', 'c4', 'c5'];
  const first = pickCardsDeterministically({availableCardIds: available, picks: 2, seed: 'same'});
  const second = pickCardsDeterministically({availableCardIds: available, picks: 2, seed: 'same'});
  assert.deepEqual(first, second);
});

test('pickCards never exceeds available cards (deck cap protection)', () => {
  const picks = pickCardsDeterministically({
    availableCardIds: ['c1', 'c2'],
    picks: 10,
    seed: 'cap',
  });
  assert.equal(picks.length, 2);
  assert.equal(new Set(picks).size, 2);
});

test('completedAt is only marked on first completion', () => {
  assert.equal(shouldMarkCompletedAt({alreadyCompleted: false, unlockedCardCount: 4, totalCards: 4}), true);
  assert.equal(shouldMarkCompletedAt({alreadyCompleted: true, unlockedCardCount: 4, totalCards: 4}), false);
});
