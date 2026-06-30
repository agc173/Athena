import type {EconomyModule, EconomyModuleStatus, EconomyRulesSnapshot} from './types';

export const DAILY_LOGIN_REWARD = 1;
export const REWARDED_AD_REWARD = 1;
export const REWARDED_AD_DAILY_MAX = 3;

type EconomyModuleRule = {
  module: EconomyModule;
  freeDaily?: number;
  freeWeekly?: number;
  freeMonthly?: number;
  freeLifetime?: number;
  premiumIncludedDaily?: number;
  premiumIncludedMonthly?: number;
  premiumDailyMax?: number;
  moonCostPerUse?: number;
  moonPackUsesPerMoon?: number;
  moonExtraDailyMax?: number;
  maxTotalDaily?: number;
  notes?: string;
};

const ECONOMY_RULES: Record<EconomyModule, EconomyModuleRule> = {
  TAROT_1: {
    module: 'TAROT_1',
    freeDaily: 1,
    premiumIncludedDaily: 5,
    premiumDailyMax: 5,
    moonCostPerUse: 1,
    moonExtraDailyMax: 3,
  },
  TAROT_3: {
    module: 'TAROT_3',
    freeWeekly: 1,
    premiumIncludedDaily: 1,
    premiumDailyMax: 3,
    moonCostPerUse: 3,
    moonExtraDailyMax: 2,
  },
  ORACLE_1Q: {
    module: 'ORACLE_1Q',
    freeDaily: 1,
    premiumIncludedDaily: 3,
    premiumDailyMax: 15,
    moonCostPerUse: 3,
    moonExtraDailyMax: 10,
  },
  BIRTH_ESSENCE: {
    module: 'BIRTH_ESSENCE',
    freeLifetime: 1,
    premiumIncludedMonthly: 1,
    moonCostPerUse: 5,
    moonExtraDailyMax: 2,
    maxTotalDaily: 2,
    notes: 'Premium extras beyond included monthly cost 3 moons',
  },
  BASIC_NATAL_CHART: {
    module: 'BASIC_NATAL_CHART',
    freeWeekly: 1,
    premiumIncludedDaily: 10,
    premiumDailyMax: 10,
    moonCostPerUse: 1,
    notes: 'Basic natal chart calculates Sun, Moon, and Ascendant only',
  },
  SYNASTRY: {
    module: 'SYNASTRY',
    freeDaily: 2,
    moonCostPerUse: 1,
    moonPackUsesPerMoon: 3,
    maxTotalDaily: 30,
    premiumDailyMax: 30,
  },
  PENDULUM: {
    module: 'PENDULUM',
    freeDaily: 8,
    moonCostPerUse: 1,
    moonPackUsesPerMoon: 10,
    maxTotalDaily: 50,
    premiumDailyMax: 50,
  },
  HOROSCOPE_FUTURE_DAY: {
    module: 'HOROSCOPE_FUTURE_DAY',
    moonCostPerUse: 1,
    notes: 'Horoscope today remains free for all users',
  },
  HOROSCOPE_WEEKLY: {
    module: 'HOROSCOPE_WEEKLY',
    moonCostPerUse: 2,
    notes: 'Premium unlocks all horoscope variants',
  },
  HOROSCOPE_MONTHLY: {
    module: 'HOROSCOPE_MONTHLY',
    moonCostPerUse: 3,
    notes: 'Premium unlocks all horoscope variants',
  },
};

export function getEconomyModuleRule(module: EconomyModule): EconomyModuleRule {
  return ECONOMY_RULES[module];
}

export function getEconomyRulesSnapshot(modules: EconomyModuleStatus[]): EconomyRulesSnapshot {
  return {
    rewards: {
      dailyLoginReward: DAILY_LOGIN_REWARD,
      rewardedAdReward: REWARDED_AD_REWARD,
      rewardedAdDailyMax: REWARDED_AD_DAILY_MAX,
    },
    horoscope: {
      todayFree: true,
      costs: {
        futureDay: ECONOMY_RULES.HOROSCOPE_FUTURE_DAY.moonCostPerUse ?? 1,
        weekly: ECONOMY_RULES.HOROSCOPE_WEEKLY.moonCostPerUse ?? 2,
        monthly: ECONOMY_RULES.HOROSCOPE_MONTHLY.moonCostPerUse ?? 3,
      },
      premiumUnlockAll: true,
    },
    modules,
  };
}
