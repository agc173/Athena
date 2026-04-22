import type {Timestamp} from 'firebase-admin/firestore';

export type EconomyModule =
  | 'TAROT_1'
  | 'TAROT_3'
  | 'ORACLE_1Q'
  | 'BIRTH_ESSENCE'
  | 'SYNASTRY'
  | 'PENDULUM'
  | 'HOROSCOPE_FUTURE_DAY'
  | 'HOROSCOPE_WEEKLY'
  | 'HOROSCOPE_MONTHLY';

export type EconomyDecisionSource =
  | 'FREE'
  | 'PREMIUM_INCLUDED'
  | 'MOON'
  | 'REJECT';

export type PremiumStatus = {
  isPremium: boolean;
  source: 'userEntitlements';
  updatedAt?: Timestamp;
};

export type EconomyBalanceDoc = {
  balance?: number;
  updatedAt?: Timestamp;
};

export type EconomyLedgerEntryDoc = {
  type: 'DAILY_LOGIN_CLAIM';
  amount: number;
  createdAt: Timestamp;
  requestId: string;
  dateIso: string;
};

export type EconomyRequestResult = 'CLAIMED' | 'ALREADY_CLAIMED';

export type EconomyRequestDoc = {
  requestId: string;
  type: 'CLAIM_DAILY_LOGIN';
  result: EconomyRequestResult;
  response: ClaimDailyLoginResponse;
  createdAt: Timestamp;
  updatedAt: Timestamp;
};

export type EconomyDailyUsageDoc = {
  dailyLoginClaimed?: boolean;
  dailyLoginClaimedAt?: Timestamp;
  rewardedAdsClaimed?: number;

  tarot1FreeUsed?: number;
  tarot1PremiumUsed?: number;
  tarot1MoonUsed?: number;

  tarot3FreeUsed?: number;
  tarot3PremiumUsed?: number;
  tarot3MoonUsed?: number;

  oracleFreeUsed?: number;
  oraclePremiumUsed?: number;
  oracleMoonUsed?: number;

  birthEssenceMoonUsed?: number;
  birthEssencePremiumIncludedUsed?: number;
  birthEssencePremiumExtraMoonUsed?: number;

  synastryFreeUsed?: number;
  synastryMoonPacksUsed?: number;
  synastryPremiumUsed?: number;

  pendulumFreeUsed?: number;
  pendulumMoonPacksUsed?: number;
  pendulumPremiumUsed?: number;

  updatedAt?: Timestamp;
};

export type EconomyWeeklyUsageDoc = {
  tarot3FreeUsed?: number;
};

export type EconomyMonthlyUsageDoc = {
  birthEssencePremiumIncludedUsed?: number;
};

export type EconomyLifetimeDoc = {
  birthEssenceFreeClaimed?: boolean;
};

export type EconomyModuleStatus = {
  module: EconomyModule;
  freeRemaining?: number;
  premiumIncludedRemaining?: number;
  moonExtraRemaining?: number;
  moonCostPerUse?: number;
  moonPackUsesPerMoon?: number;
  maxTotalRemaining?: number;
  notes?: string;
};

export type EconomyRulesSnapshot = {
  rewards: {
    dailyLoginReward: number;
    rewardedAdReward: number;
    rewardedAdDailyMax: number;
  };
  horoscope: {
    todayFree: boolean;
    costs: {
      futureDay: number;
      weekly: number;
      monthly: number;
    };
    premiumUnlockAll: boolean;
  };
  modules: EconomyModuleStatus[];
};

export type GetEconomyBalanceResponse = {
  balance: number;
  updatedAt?: Timestamp;
  dailyLoginClaimed: boolean;
  rewardedAdsClaimed: number;
  rewardedAdsRemaining: number;
};

export type GetEconomyStatusResponse = {
  balance: number;
  updatedAt?: Timestamp;
  premium: PremiumStatus;
  todayDateIso: string;
  weekKey: string;
  monthKey: string;
  rules: EconomyRulesSnapshot;
};

export type ClaimDailyLoginData = {
  requestId?: unknown;
};

export type ClaimDailyLoginResponse = {
  result: EconomyRequestResult;
  balance: number;
  dailyLoginClaimed: boolean;
  rewardedAdsClaimed: number;
  rewardedAdsRemaining: number;
};
