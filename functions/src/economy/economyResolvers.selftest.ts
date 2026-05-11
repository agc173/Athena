import test from 'node:test';
import assert from 'node:assert/strict';
import {resolveBirthEssenceDecision} from './birthEssenceEconomy';
import {resolveHoroscopeUnlockDecision} from './horoscopeEconomy';
import {resolveOracleDecision} from './oracleEconomy';
import {getEconomyModuleRule, REWARDED_AD_DAILY_MAX, REWARDED_AD_REWARD} from './rulesCatalog';
import {resolveTarotDecision} from './tarotEconomy';
import {resolveSynastryDecision} from './synastryEconomy';
import {RequestType} from '../oracle/types';

test('horoscope weekly/monthly costs are 3/5 moons', () => {
  assert.equal(getEconomyModuleRule('HOROSCOPE_WEEKLY').moonCostPerUse, 3);
  assert.equal(getEconomyModuleRule('HOROSCOPE_MONTHLY').moonCostPerUse, 5);
});

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
    dailyUsage: {tarot1FreeUsed: 1, tarot1MoonUsed: 3},
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

test('birth essence extra costs 5 moons for free and premium users', () => {
  const freeDecision = resolveBirthEssenceDecision({
    isPremium: false,
    balance: 5,
    dailyUsage: {birthEssenceTotalUsed: 1},
    monthlyUsage: {},
    lifetimeUsage: {birthEssenceFreeClaimed: true},
  });
  assert.equal(freeDecision.source, 'MOON');
  assert.equal(freeDecision.moonCost, 5);

  const premiumDecision = resolveBirthEssenceDecision({
    isPremium: true,
    balance: 5,
    dailyUsage: {birthEssenceTotalUsed: 1},
    monthlyUsage: {birthEssencePremiumIncludedUsed: 1},
    lifetimeUsage: {birthEssenceFreeClaimed: true},
  });
  assert.equal(premiumDecision.source, 'MOON');
  assert.equal(premiumDecision.moonCost, 5);
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
    moonCost: 5,
  });

  assert.equal(decision.source, 'PREMIUM_INCLUDED');
  assert.equal(decision.moonCost, 0);
});

test('horoscope free charges moon when balance is enough', () => {
  const decision = resolveHoroscopeUnlockDecision({
    isPremium: false,
    balance: 5,
    moonCost: getEconomyModuleRule('HOROSCOPE_MONTHLY').moonCostPerUse ?? 0,
  });

  assert.equal(decision.source, 'MOON');
  assert.equal(decision.moonCost, 5);
});

test('rewarded ads keep +1 reward and max 3 per day', () => {
  assert.equal(REWARDED_AD_REWARD, 1);
  assert.equal(REWARDED_AD_DAILY_MAX, 3);
});


test('ads only grant moons and do not directly unlock tarot/oracle content', () => {
  const tarotAfterFreeWithoutMoons = resolveTarotDecision({
    requestType: RequestType.TAROT_1,
    isPremium: false,
    balance: 0,
    dailyUsage: {tarot1FreeUsed: 1},
    weeklyUsage: {},
  });
  assert.equal(tarotAfterFreeWithoutMoons.source, 'REJECT');
  assert.equal(tarotAfterFreeWithoutMoons.reason, 'INSUFFICIENT_MOON_BALANCE');

  const oracleAfterFreeWithoutMoons = resolveOracleDecision({
    isPremium: false,
    balance: 0,
    dailyUsage: {oracleFreeUsed: 1},
  });
  assert.equal(oracleAfterFreeWithoutMoons.source, 'REJECT');
  assert.equal(oracleAfterFreeWithoutMoons.reason, 'INSUFFICIENT_MOON_BALANCE');
});


test('oracle free first call works without rewarded proof semantics', () => {
  const decision = resolveOracleDecision({
    isPremium: false,
    balance: 0,
    dailyUsage: {},
  });

  assert.equal(decision.source, 'FREE');
});

test('oracle free allows 1 included plus 10 moon extras', () => {
  const tenthExtra = resolveOracleDecision({
    isPremium: false,
    balance: 3,
    dailyUsage: {oracleFreeUsed: 1, oracleMoonUsed: 9},
  });
  assert.equal(tenthExtra.source, 'MOON');
  assert.equal(tenthExtra.moonCost, 3);

  const eleventhExtra = resolveOracleDecision({
    isPremium: false,
    balance: 3,
    dailyUsage: {oracleFreeUsed: 1, oracleMoonUsed: 10},
  });
  assert.equal(eleventhExtra.source, 'REJECT');
  assert.equal(eleventhExtra.reason, 'ORACLE_MOON_DAILY_LIMIT_REACHED');
});

test('oracle premium allows 3 included plus 12 moon extras and total 15', () => {
  const included = resolveOracleDecision({
    isPremium: true,
    balance: 0,
    dailyUsage: {oraclePremiumUsed: 2},
  });
  assert.equal(included.source, 'PREMIUM_INCLUDED');

  const twelfthExtra = resolveOracleDecision({
    isPremium: true,
    balance: 3,
    dailyUsage: {oraclePremiumUsed: 3, oracleMoonUsed: 11},
  });
  assert.equal(twelfthExtra.source, 'MOON');
  assert.equal(twelfthExtra.moonCost, 3);

  const overTotal = resolveOracleDecision({
    isPremium: true,
    balance: 3,
    dailyUsage: {oraclePremiumUsed: 3, oracleMoonUsed: 12},
  });
  assert.equal(overTotal.source, 'REJECT');
  assert.equal(overTotal.reason, 'ORACLE_MOON_DAILY_LIMIT_REACHED');
});

test('synastry free allows 2 included plus max 5 moon packs', () => {
  const freeIncluded = resolveSynastryDecision({
    isPremium: false,
    balance: 0,
    dailyUsage: {synastryFreeUsed: 1},
  });
  assert.equal(freeIncluded.source, 'FREE');

  const fifthPack = resolveSynastryDecision({
    isPremium: false,
    balance: 1,
    dailyUsage: {synastryFreeUsed: 2, synastryMoonPacksPurchased: 4, synastryMoonUsesUsed: 12},
  });
  assert.equal(fifthPack.source, 'MOON');
  assert.equal(fifthPack.moonCost, 1);

  const sixthPack = resolveSynastryDecision({
    isPremium: false,
    balance: 1,
    dailyUsage: {synastryFreeUsed: 2, synastryMoonPacksPurchased: 5, synastryMoonUsesUsed: 15},
  });
  assert.equal(sixthPack.source, 'REJECT');
  assert.equal(sixthPack.reason, 'SYNASTRY_MOON_PACK_DAILY_LIMIT_REACHED');
});

test('synastry premium allows 10 included plus max 10 moon packs', () => {
  const tenthIncluded = resolveSynastryDecision({
    isPremium: true,
    balance: 0,
    dailyUsage: {synastryPremiumUsed: 9},
  });
  assert.equal(tenthIncluded.source, 'PREMIUM_INCLUDED');

  const tenthPack = resolveSynastryDecision({
    isPremium: true,
    balance: 1,
    dailyUsage: {synastryPremiumUsed: 10, synastryMoonPacksPurchased: 9, synastryMoonUsesUsed: 27},
  });
  assert.equal(tenthPack.source, 'MOON');
  assert.equal(tenthPack.moonCost, 1);

  const eleventhPack = resolveSynastryDecision({
    isPremium: true,
    balance: 1,
    dailyUsage: {synastryPremiumUsed: 10, synastryMoonPacksPurchased: 10, synastryMoonUsesUsed: 30},
  });
  assert.equal(eleventhPack.source, 'REJECT');
  assert.equal(eleventhPack.reason, 'SYNASTRY_MOON_PACK_DAILY_LIMIT_REACHED');
});

test('synastry consumes existing pack use without charging moon', () => {
  const decision = resolveSynastryDecision({
    isPremium: false,
    balance: 0,
    dailyUsage: {synastryFreeUsed: 2, synastryMoonPacksPurchased: 1, synastryMoonUsesUsed: 1},
  });
  assert.equal(decision.source, 'MOON');
  assert.equal(decision.moonCost, 0);
});

test('synastry rejects insufficient moons when pack exhausted', () => {
  const decision = resolveSynastryDecision({
    isPremium: false,
    balance: 0,
    dailyUsage: {synastryFreeUsed: 2, synastryMoonPacksPurchased: 1, synastryMoonUsesUsed: 3},
  });
  assert.equal(decision.source, 'REJECT');
  assert.equal(decision.reason, 'INSUFFICIENT_MOON_BALANCE');
});
