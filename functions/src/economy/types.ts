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
  type:
  | 'DAILY_LOGIN_CLAIM'
  | 'REWARDED_AD_CLAIM'
  | 'TAROT_1_MOON_SPEND'
  | 'TAROT_3_MOON_SPEND'
  | 'ORACLE_1Q_MOON_SPEND'
  | 'BIRTH_ESSENCE_MOON_SPEND'
  | 'REFUND'
  | 'HOROSCOPE_FUTURE_DAY_MOON_SPEND'
  | 'HOROSCOPE_WEEKLY_MOON_SPEND'
  | 'HOROSCOPE_MONTHLY_MOON_SPEND'
  | 'SYNASTRY_MOON_SPEND'
  | 'PENDULUM_MOON_SPEND'
  | 'MOON_PACK_PURCHASE';
  amount: number;
  createdAt: Timestamp;
  requestId: string;
  dateIso: string;
  placement?: string;
  targetDateIso?: string;
  weekKey?: string;
  monthKey?: string;
  unlockKey?: string;
  module?: EconomyModule | string;
  source?: string;
};

export type EconomyRequestResult =
  | 'CLAIMED'
  | 'DAILY_LIMIT_REACHED'
  | 'ALREADY_CLAIMED'
  | 'RESERVED'
  | 'COMPLETED_SUCCESS'
  | 'REFUNDED'
  | 'FAILED';

export type EconomyRequestType =
  | 'CLAIM_DAILY_LOGIN'
  | 'CLAIM_REWARDED_AD'
  | 'TAROT_1'
  | 'TAROT_3'
  | 'ORACLE_1Q'
  | 'BIRTH_ESSENCE'
  | 'SYNASTRY'
  | 'PENDULUM'
  | 'HOROSCOPE_UNLOCK_DAY'
  | 'HOROSCOPE_UNLOCK_WEEKLY'
  | 'HOROSCOPE_UNLOCK_MONTHLY'
  | 'CLAIM_MOON_PACK_PURCHASE';

export type EconomyRequestDoc = {
  requestId: string;
  type: EconomyRequestType;
  result: EconomyRequestResult;
  response?:
  | ClaimDailyLoginResponse
  | ClaimRewardedAdResponse
  | UnlockHoroscopeDayResponse
  | UnlockHoroscopeWeeklyResponse
  | UnlockHoroscopeMonthlyResponse;
  responsePayload?: unknown;
  status?: 'PROCESSING' | 'FAILED' | 'COMPLETED_SUCCESS';
  decisionSource?: EconomyDecisionSource;
  moonCostCharged?: number;
  usageApplied?: {
    dailyCounter?: string;
    dailyCounters?: string[];
    weeklyCounter?: string;
    monthlyCounter?: string;
    lifetimeFlag?: string;
  };
  dateIso?: string;
  weekKey?: string;
  monthKey?: string;
  lang?: string;
  question?: string;
  llmMeta?: unknown;
  error?: unknown;
  refundedAt?: Timestamp;
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
  birthEssenceTotalUsed?: number;

  synastryFreeUsed?: number;
  synastryMoonPacksPurchased?: number;
  synastryMoonUsesUsed?: number;
  synastryPremiumUsed?: number;

  pendulumFreeUsed?: number;
  pendulumMoonPacksPurchased?: number;
  pendulumMoonUsesUsed?: number;
  pendulumPremiumUsed?: number;

  horoscopeFutureDayMoonUsed?: number;
  horoscopeWeeklyMoonUsed?: number;
  horoscopeMonthlyMoonUsed?: number;

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

export type ClaimRewardedAdData = {
  requestId?: unknown;
  adProof?: unknown;
  placement?: unknown;
};

export type ClaimRewardedAdResponse = {
  result: 'CLAIMED' | 'DAILY_LIMIT_REACHED';
  balance: number;
  dailyLoginClaimed: boolean;
  rewardedAdsClaimed: number;
  rewardedAdsRemaining: number;
};


export type UnlockHoroscopeDayData = {
  requestId?: unknown;
  dateIso?: unknown;
  sign?: unknown;
};

export type DeckCardUnlockReward = {
  deckId: string;
  trackId: string;
  rewardPoolId: string;
  cardId: string;
};

export type UnlockHoroscopeDayResponse = {
  result: EconomyRequestResult;
  unlocked: boolean;
  alreadyUnlocked: boolean;
  balance: number;
  costCharged: number;
  deckCardUnlockRewards?: DeckCardUnlockReward[];
};

export type GetHoroscopeDailyUnlocksData = {
  dateIsoList?: unknown;
};

export type GetHoroscopeDailyUnlocksResponse = {
  unlockedDateIsoList: string[];
};

export type UnlockHoroscopeWeeklyData = {
  requestId?: unknown;
  weekKey?: unknown;
  sign?: unknown;
};

export type UnlockHoroscopeWeeklyResponse = {
  result: EconomyRequestResult;
  unlocked: boolean;
  alreadyUnlocked: boolean;
  balance: number;
  costCharged: number;
  deckCardUnlockRewards?: DeckCardUnlockReward[];
};

export type UnlockHoroscopeMonthlyData = {
  requestId?: unknown;
  monthKey?: unknown;
  sign?: unknown;
};

export type UnlockHoroscopeMonthlyResponse = {
  result: EconomyRequestResult;
  unlocked: boolean;
  alreadyUnlocked: boolean;
  balance: number;
  costCharged: number;
  deckCardUnlockRewards?: DeckCardUnlockReward[];
};

export type GetHoroscopeWeeklyUnlocksData = {
  weekKeyList?: unknown;
};

export type GetHoroscopeWeeklyUnlocksResponse = {
  unlockedWeekKeyList: string[];
};

export type GetHoroscopeMonthlyUnlocksData = {
  monthKeyList?: unknown;
};

export type GetHoroscopeMonthlyUnlocksResponse = {
  unlockedMonthKeyList: string[];
};
