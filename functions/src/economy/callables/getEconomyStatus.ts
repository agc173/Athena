import {Timestamp} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {
  dateIsoMadrid,
  economyBalanceRef,
  economyLifetimeRef,
  economyUsageDailyRef,
  economyUsageMonthlyRef,
  economyUsageWeeklyRef,
  monthKeyMadrid,
  weekKeyMadrid,
} from '../firestorePaths';
import {getPremiumStatus} from '../premiumStatus';
import {getEconomyModuleRule, getEconomyRulesSnapshot} from '../rulesCatalog';
import type {
  EconomyBalanceDoc,
  EconomyDailyUsageDoc,
  EconomyLifetimeDoc,
  EconomyModuleStatus,
  EconomyMonthlyUsageDoc,
  EconomyWeeklyUsageDoc,
  GetEconomyStatusResponse,
} from '../types';

function asCount(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

function asOptionalCount(value: unknown): number | undefined {
  if (typeof value !== 'number' || !Number.isFinite(value)) return undefined;
  return Math.max(0, Math.floor(value));
}

function remainingFromRule(limit: number | undefined, used: unknown): number | undefined {
  if (limit == null) return undefined;
  const safeUsed = asOptionalCount(used);
  if (safeUsed == null) return undefined;
  return Math.max(0, limit - safeUsed);
}

function totalUsedIfKnown(...values: unknown[]): number | undefined {
  const normalized = values.map(asOptionalCount);
  if (normalized.some((value) => value == null)) return undefined;

  let total = 0;
  for (const value of normalized) {
    total += value ?? 0;
  }
  return total;
}

function pickModuleStatuses(params: {
  isPremium: boolean;
  daily: EconomyDailyUsageDoc;
  weekly: EconomyWeeklyUsageDoc;
  monthly: EconomyMonthlyUsageDoc;
  lifetime: EconomyLifetimeDoc;
}): EconomyModuleStatus[] {
  const daily = params.daily;
  const weekly = params.weekly;
  const monthly = params.monthly;
  const lifetime = params.lifetime;

  const tarot1Rule = getEconomyModuleRule('TAROT_1');
  const tarot3Rule = getEconomyModuleRule('TAROT_3');
  const oracleRule = getEconomyModuleRule('ORACLE_1Q');
  const birthEssenceRule = getEconomyModuleRule('BIRTH_ESSENCE');
  const synastryRule = getEconomyModuleRule('SYNASTRY');
  const pendulumRule = getEconomyModuleRule('PENDULUM');
  const horoscopeFutureDayRule = getEconomyModuleRule('HOROSCOPE_FUTURE_DAY');
  const horoscopeWeeklyRule = getEconomyModuleRule('HOROSCOPE_WEEKLY');
  const horoscopeMonthlyRule = getEconomyModuleRule('HOROSCOPE_MONTHLY');

  const birthEssenceLifetimeClaimed = lifetime.birthEssenceFreeClaimed === true;

  const tarot1PremiumIncludedRemaining = remainingFromRule(
      params.isPremium ? tarot1Rule.premiumIncludedDaily : undefined,
      daily.tarot1PremiumUsed
  );

  return [
    {
      module: 'TAROT_1',
      freeRemaining: remainingFromRule(tarot1Rule.freeDaily, daily.tarot1FreeUsed),
      premiumIncludedRemaining: tarot1PremiumIncludedRemaining,
      moonExtraRemaining: remainingFromRule(tarot1Rule.moonExtraDailyMax, daily.tarot1MoonUsed),
      moonCostPerUse: tarot1Rule.moonCostPerUse,
      notes: params.isPremium && tarot1PremiumIncludedRemaining == null ?
        'premiumIncludedRemaining requires tarot1PremiumUsed counter' : undefined,
    },
    {
      module: 'TAROT_3',
      freeRemaining: remainingFromRule(tarot3Rule.freeWeekly, weekly.tarot3FreeUsed),
      premiumIncludedRemaining: remainingFromRule(
          params.isPremium ? tarot3Rule.premiumIncludedDaily : undefined,
          daily.tarot3PremiumUsed
      ),
      moonExtraRemaining: remainingFromRule(tarot3Rule.moonExtraDailyMax, daily.tarot3MoonUsed),
      moonCostPerUse: tarot3Rule.moonCostPerUse,
      maxTotalRemaining: remainingFromRule(
          params.isPremium ? tarot3Rule.premiumDailyMax : undefined,
          totalUsedIfKnown(daily.tarot3PremiumUsed, daily.tarot3MoonUsed)
      ),
      notes: params.isPremium && totalUsedIfKnown(daily.tarot3PremiumUsed, daily.tarot3MoonUsed) == null ?
        'maxTotalRemaining requires tarot3PremiumUsed and tarot3MoonUsed counters' : undefined,
    },
    {
      module: 'ORACLE_1Q',
      freeRemaining: params.isPremium ? undefined : remainingFromRule(oracleRule.freeDaily, daily.oracleFreeUsed),
      premiumIncludedRemaining: remainingFromRule(
          params.isPremium ? oracleRule.premiumIncludedDaily : undefined,
          daily.oraclePremiumUsed
      ),
      moonExtraRemaining: remainingFromRule(
          params.isPremium ? oracleRule.premiumMoonExtraDailyMax : oracleRule.moonExtraDailyMax,
          daily.oracleMoonUsed
      ),
      moonCostPerUse: oracleRule.moonCostPerUse,
      maxTotalRemaining: remainingFromRule(
          params.isPremium ? oracleRule.premiumDailyMax : undefined,
          totalUsedIfKnown(daily.oraclePremiumUsed, daily.oracleMoonUsed)
      ),
      notes: params.isPremium && totalUsedIfKnown(daily.oraclePremiumUsed, daily.oracleMoonUsed) == null ?
        'maxTotalRemaining requires oraclePremiumUsed and oracleMoonUsed counters' : undefined,
    },
    {
      module: 'BIRTH_ESSENCE',
      freeRemaining: birthEssenceLifetimeClaimed ? 0 : birthEssenceRule.freeLifetime,
      premiumIncludedRemaining: remainingFromRule(
          params.isPremium ? birthEssenceRule.premiumIncludedMonthly : undefined,
          monthly.birthEssencePremiumIncludedUsed
      ),
      moonExtraRemaining: remainingFromRule(birthEssenceRule.moonExtraDailyMax, daily.birthEssenceMoonUsed),
      moonCostPerUse: birthEssenceRule.moonCostPerUse,
      notes: params.isPremium ?
        'premium extras beyond monthly included cost 5 moons; Premium is not unlimited; total daily max not exposed in phase 1' :
        'total daily max not exposed in phase 1 due partial counters',
    },
    {
      module: 'SYNASTRY',
      freeRemaining: params.isPremium ? undefined : remainingFromRule(synastryRule.freeDaily, daily.synastryFreeUsed),
      premiumIncludedRemaining: remainingFromRule(
          params.isPremium ? synastryRule.premiumIncludedDaily : undefined,
          daily.synastryPremiumUsed
      ),
      moonCostPerUse: synastryRule.moonCostPerUse,
      moonPackUsesPerMoon: synastryRule.moonPackUsesPerMoon,
      moonExtraRemaining: remainingFromRule(
          params.isPremium ? synastryRule.premiumMoonExtraDailyMax : synastryRule.moonExtraDailyMax,
          daily.synastryMoonPacksPurchased
      ),
      notes: params.isPremium ?
        'Premium is not unlimited: 10/day included, plus max 10 moon packs/day' :
        'Free has 2/day included, plus max 5 moon packs/day',
    },
    {
      module: 'PENDULUM',
      freeRemaining: remainingFromRule(pendulumRule.freeDaily, daily.pendulumFreeUsed),
      premiumIncludedRemaining: remainingFromRule(
          params.isPremium ? pendulumRule.premiumDailyMax : undefined,
          daily.pendulumPremiumUsed
      ),
      moonCostPerUse: pendulumRule.moonCostPerUse,
      moonPackUsesPerMoon: pendulumRule.moonPackUsesPerMoon,
      maxTotalRemaining: remainingFromRule(
          params.isPremium ? pendulumRule.premiumDailyMax : undefined,
          daily.pendulumPremiumUsed
      ),
      notes: params.isPremium ?
        undefined :
        'free total cap is 50/day but precise remaining requires per-use counter in phase 1',
    },
    {
      module: 'HOROSCOPE_FUTURE_DAY',
      moonCostPerUse: horoscopeFutureDayRule.moonCostPerUse,
      notes: 'today is free, premium unlocks all',
    },
    {
      module: 'HOROSCOPE_WEEKLY',
      moonCostPerUse: horoscopeWeeklyRule.moonCostPerUse,
      notes: 'premium unlocks all',
    },
    {
      module: 'HOROSCOPE_MONTHLY',
      moonCostPerUse: horoscopeMonthlyRule.moonCostPerUse,
      notes: 'premium unlocks all',
    },
  ];
}

export const getEconomyStatus = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<GetEconomyStatusResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const todayDateIso = dateIsoMadrid();
      const weekKey = weekKeyMadrid();
      const monthKey = monthKeyMadrid();

      const [balanceSnap, dailySnap, weeklySnap, monthlySnap, lifetimeSnap, premium] = await Promise.all([
        economyBalanceRef(uid).get(),
        economyUsageDailyRef(todayDateIso, uid).get(),
        economyUsageWeeklyRef(weekKey, uid).get(),
        economyUsageMonthlyRef(monthKey, uid).get(),
        economyLifetimeRef(uid).get(),
        getPremiumStatus(uid),
      ]);

      const balanceData = balanceSnap.data() as EconomyBalanceDoc | undefined;
      const dailyData = (dailySnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
      const weeklyData = (weeklySnap.data() as EconomyWeeklyUsageDoc | undefined) ?? {};
      const monthlyData = (monthlySnap.data() as EconomyMonthlyUsageDoc | undefined) ?? {};
      const lifetimeData = (lifetimeSnap.data() as EconomyLifetimeDoc | undefined) ?? {};

      const modules = pickModuleStatuses({
        isPremium: premium.isPremium,
        daily: dailyData,
        weekly: weeklyData,
        monthly: monthlyData,
        lifetime: lifetimeData,
      });

      return {
        balance: asCount(balanceData?.balance),
        updatedAt: balanceData?.updatedAt instanceof Timestamp ? balanceData.updatedAt : undefined,
        premium,
        todayDateIso,
        weekKey,
        monthKey,
        rules: getEconomyRulesSnapshot(modules),
      };
    }
);
