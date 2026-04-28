import test from 'node:test';
import assert from 'node:assert/strict';
import {resolveBirthEssenceDecision} from './birthEssenceEconomy';
import {resolveHoroscopeUnlockDecision} from './horoscopeEconomy';
import {resolveOracleDecision} from './oracleEconomy';
import {REWARDED_AD_DAILY_MAX, REWARDED_AD_REWARD} from './rulesCatalog';
import {resolveTarotDecision} from './tarotEconomy';
import {RequestType} from '../oracle/types';

test('tarot resolver uses FREE first', () => {
  const decision = resolveTarotDecision({
    requestType: RequestType.TAROT_1,
    isPremium: false,
    balance: 0,
    dailyUsage: {},
    weeklyUsage: {},
  });

  assert.equal(decision.source, 'FREE');
});

test('tarot resolver uses PREMIUM_INCLUDED before MOON', () => {
  const decision = resolveTarotDecision({
    requestType: RequestType.TAROT_1,
    isPremium: true,
    balance: 0,
    dailyUsage: {tarot1FreeUsed: 1},
    weeklyUsage: {},
  });

  assert.equal(decision.source, 'PREMIUM_INCLUDED');
  assert.equal(decision.moonCost, 0);
});

test('tarot_1 premium after included can continue with MOON', () => {
  const decision = resolveTarotDecision({
    requestType: RequestType.TAROT_1,
    isPremium: true,
    balance: 5,
    dailyUsage: {
      tarot1FreeUsed: 1,
      tarot1PremiumUsed: 5,
      tarot1MoonUsed: 0,
    },
    weeklyUsage: {},
  });

  assert.equal(decision.source, 'MOON');
  assert.equal(decision.moonCost, 1);
});

test('tarot resolver uses MOON after free for non premium', () => {
  const decision = resolveTarotDecision({
    requestType: RequestType.TAROT_1,
    isPremium: false,
    balance: 5,
    dailyUsage: {tarot1FreeUsed: 1},
    weeklyUsage: {},
  });

  assert.equal(decision.source, 'MOON');
  assert.equal(decision.moonCost, 1);
});

test('resolver rejects when over limit or insufficient balance', () => {
  const dailyLimitReached = resolveTarotDecision({
    requestType: RequestType.TAROT_1,
    isPremium: false,
    balance: 10,
    dailyUsage: {tarot1FreeUsed: 1, tarot1MoonUsed: 2},
    weeklyUsage: {},
  });
  assert.equal(dailyLimitReached.source, 'REJECT');

  const insufficient = resolveOracleDecision({
    isPremium: false,
    balance: 0,
    dailyUsage: {oracleFreeUsed: 1, oracleMoonUsed: 0},
  });
  assert.equal(insufficient.source, 'REJECT');
});

test('oracle premium rejects when premium hard cap is reached', () => {
  const decision = resolveOracleDecision({
    isPremium: true,
    balance: 50,
    dailyUsage: {
      oracleFreeUsed: 1,
      oraclePremiumUsed: 3,
      oracleMoonUsed: 12,
    },
  });

  assert.equal(decision.source, 'REJECT');
});

test('birth essence maxTotalDaily blocks mixed premium plus moon path', () => {
  const decision = resolveBirthEssenceDecision({
    isPremium: true,
    balance: 50,
    dailyUsage: {
      birthEssenceTotalUsed: 2,
      birthEssenceMoonUsed: 1,
    },
    monthlyUsage: {
      birthEssencePremiumIncludedUsed: 1,
    },
    lifetimeUsage: {
      birthEssenceFreeClaimed: true,
    },
  });

  assert.equal(decision.source, 'REJECT');
});

test('premium included paths do not depend on moon balance', () => {
  const birthEssencePremium = resolveBirthEssenceDecision({
    isPremium: true,
    balance: 0,
    dailyUsage: {birthEssenceTotalUsed: 1},
    monthlyUsage: {birthEssencePremiumIncludedUsed: 0},
    lifetimeUsage: {birthEssenceFreeClaimed: true},
  });
  assert.equal(birthEssencePremium.source, 'PREMIUM_INCLUDED');

  const oraclePremium = resolveOracleDecision({
    isPremium: true,
    balance: 0,
    dailyUsage: {oracleFreeUsed: 1, oraclePremiumUsed: 0},
  });
  assert.equal(oraclePremium.source, 'PREMIUM_INCLUDED');
});

test('horoscope premium unlock does not charge moons', () => {
  const decision = resolveHoroscopeUnlockDecision({
    isPremium: true,
    balance: 0,
    moonCost: 3,
  });

  assert.equal(decision.source, 'PREMIUM_INCLUDED');
  assert.equal(decision.moonCost, 0);
});

test('horoscope free charges moon when balance is enough', () => {
  const decision = resolveHoroscopeUnlockDecision({
    isPremium: false,
    balance: 3,
    moonCost: 2,
  });

  assert.equal(decision.source, 'MOON');
  assert.equal(decision.moonCost, 2);
});

test('rewarded ads keep +1 reward and max 3 per day', () => {
  assert.equal(REWARDED_AD_REWARD, 1);
  assert.equal(REWARDED_AD_DAILY_MAX, 3);
});
