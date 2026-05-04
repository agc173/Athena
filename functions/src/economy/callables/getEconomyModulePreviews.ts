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
import {resolveBirthEssenceDecision} from '../birthEssenceEconomy';
import {getPremiumStatus} from '../premiumStatus';
import {getEconomyModuleRule} from '../rulesCatalog';
import {resolveTarotDecision} from '../tarotEconomy';
import {resolveOracleDecision} from '../oracleEconomy';
import {RequestType} from '../../oracle/types';
import type {
  EconomyBalanceDoc,
  EconomyDailyUsageDoc,
  EconomyLifetimeDoc,
  EconomyModule,
  EconomyMonthlyUsageDoc,
  EconomyWeeklyUsageDoc,
} from '../types';

export type EconomyNextSource =
  | 'FREE'
  | 'PREMIUM'
  | 'MOON'
  | 'REJECTED'
  | 'NOT_CONFIGURED'
  | 'COMING_SOON'
  | 'UNKNOWN'
  | 'RULE_CONFIGURED_NOT_WIRED';

export type EconomyModulePreview = {
  module: EconomyModule;
  nextSource: EconomyNextSource;
  cost: number;
  balance: number;
  canExecute: boolean;
  reasonIfRejected?: string;
  labelKey?: string;
  uiHint?: string;
  freeRemaining?: number;
  premiumRemaining?: number;
  moonRemaining?: number;
  moonPackUsesPerMoon?: number;
  dailyCap?: number;
};

type GetEconomyModulePreviewsData = { modules?: unknown };

const ALL_MODULES: EconomyModule[] = [
  'HOROSCOPE_FUTURE_DAY',
  'HOROSCOPE_WEEKLY',
  'HOROSCOPE_MONTHLY',
  'ORACLE_1Q',
  'TAROT_1',
  'TAROT_3',
  'BIRTH_ESSENCE',
  'SYNASTRY',
  'PENDULUM',
];

function intValue(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

function normalizeModule(module: unknown): EconomyModule | null {
  if (typeof module !== 'string') return null;
  const normalized = module === 'NATAL_ESSENCE' ? 'BIRTH_ESSENCE' : module;
  return ALL_MODULES.includes(normalized as EconomyModule) ? normalized as EconomyModule : null;
}

function normalizeRequestedModules(input: unknown): EconomyModule[] {
  if (!Array.isArray(input) || input.length === 0) return ALL_MODULES;
  const unique = new Set<EconomyModule>();
  for (const raw of input) {
    const module = normalizeModule(raw);
    if (module) unique.add(module);
  }
  return unique.size > 0 ? Array.from(unique) : ALL_MODULES;
}


function normalizeReason(reason: string | undefined): string | undefined {
  if (!reason) return undefined;
  if (reason === 'INSUFFICIENT_MOON_BALANCE') return 'insufficient_moons';
  if (reason === 'SYNASTRY_DAILY_LIMIT_REACHED') return 'daily_limit';
  if (reason === 'MODULE_NOT_CONFIGURED') return 'module_not_configured';
  if (reason === 'RULE_CONFIGURED_NOT_WIRED') return 'rule_configured_not_wired';
  return reason.toLowerCase();
}

function toNextSource(source: 'FREE' | 'PREMIUM_INCLUDED' | 'MOON' | 'REJECT'): EconomyNextSource {
  if (source === 'PREMIUM_INCLUDED') return 'PREMIUM';
  if (source === 'REJECT') return 'REJECTED';
  return source;
}

function buildHoroscopePreview(module: 'HOROSCOPE_FUTURE_DAY' | 'HOROSCOPE_WEEKLY' | 'HOROSCOPE_MONTHLY', isPremium: boolean, balance: number): EconomyModulePreview {
  const rule = getEconomyModuleRule(module);
  const cost = rule.moonCostPerUse ?? 0;
  if (isPremium) {
    return {
      module,
      nextSource: 'PREMIUM',
      cost: 0,
      balance,
      canExecute: true,
      premiumRemaining: undefined,
      uiHint: 'Premium includes horoscope unlock',
    };
  }

  return {
    module,
    nextSource: 'MOON',
    cost,
    balance,
    canExecute: balance >= cost,
    reasonIfRejected: balance >= cost ? undefined : 'insufficient_moons',
  };
}

export async function getEconomyModulePreviewsCore(uid: string, modulesInput?: unknown): Promise<{ previews: EconomyModulePreview[] }> {
  const todayDateIso = dateIsoMadrid();
  const monthKey = monthKeyMadrid();
  const weekKey = weekKeyMadrid();

  const [balanceSnap, dailySnap, weeklySnap, monthlySnap, lifetimeSnap, premium] = await Promise.all([
    economyBalanceRef(uid).get(),
    economyUsageDailyRef(todayDateIso, uid).get(),
    economyUsageWeeklyRef(weekKey, uid).get(),
    economyUsageMonthlyRef(monthKey, uid).get(),
    economyLifetimeRef(uid).get(),
    getPremiumStatus(uid),
  ]);

  const balance = intValue((balanceSnap.data() as EconomyBalanceDoc | undefined)?.balance);
  const daily = (dailySnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
  const weekly = (weeklySnap.data() as EconomyWeeklyUsageDoc | undefined) ?? {};
  const monthly = (monthlySnap.data() as EconomyMonthlyUsageDoc | undefined) ?? {};
  const lifetime = (lifetimeSnap.data() as EconomyLifetimeDoc | undefined) ?? {};
  const requestedModules = normalizeRequestedModules(modulesInput);

  const previews = requestedModules.map((module): EconomyModulePreview => {
    if (module === 'HOROSCOPE_FUTURE_DAY' || module === 'HOROSCOPE_WEEKLY' || module === 'HOROSCOPE_MONTHLY') {
      return buildHoroscopePreview(module, premium.isPremium, balance);
    }

    if (module === 'ORACLE_1Q') {
      const decision = resolveOracleDecision({isPremium: premium.isPremium, balance, dailyUsage: daily});
      return {
        module,
        nextSource: toNextSource(decision.source),
        cost: decision.source === 'MOON' ? decision.moonCost : 0,
        balance,
        canExecute: decision.source !== 'REJECT',
        reasonIfRejected: normalizeReason(decision.reason),
      };
    }

    if (module === 'TAROT_1' || module === 'TAROT_3') {
      const decision = resolveTarotDecision({
        requestType: module === 'TAROT_1' ? RequestType.TAROT_1 : RequestType.TAROT_3,
        isPremium: premium.isPremium,
        balance,
        dailyUsage: daily,
        weeklyUsage: weekly,
      });
      const ruleCost = getEconomyModuleRule(module).moonCostPerUse ?? 0;
      const isInsufficientMoonsReject = decision.source === 'REJECT' && decision.reason === 'INSUFFICIENT_MOON_BALANCE';
      return {
        module,
        nextSource: toNextSource(decision.source),
        cost: decision.source === 'MOON' ? decision.moonCost : (isInsufficientMoonsReject ? ruleCost : 0),
        balance,
        canExecute: decision.source !== 'REJECT',
        reasonIfRejected: normalizeReason(decision.reason),
      };
    }

    if (module === 'BIRTH_ESSENCE') {
      const decision = resolveBirthEssenceDecision({
        isPremium: premium.isPremium,
        balance,
        dailyUsage: daily,
        monthlyUsage: monthly,
        lifetimeUsage: lifetime,
      });
      return {
        module,
        nextSource: toNextSource(decision.source),
        cost: decision.source === 'MOON' ? decision.moonCost : 0,
        balance,
        canExecute: decision.source !== 'REJECT',
        reasonIfRejected: normalizeReason(decision.reason),
      };
    }

    if (module === 'SYNASTRY' || module === 'PENDULUM') {
      const rule = getEconomyModuleRule(module);
      const freeUsed = module === 'SYNASTRY'
        ? intValue(daily.synastryFreeUsed)
        : intValue(daily.pendulumFreeUsed);
      const freeDaily = intValue(rule.freeDaily);
      const freeRemaining = Math.max(0, freeDaily - freeUsed);
      return {
        module,
        nextSource: 'RULE_CONFIGURED_NOT_WIRED',
        cost: rule.moonCostPerUse ?? 0,
        balance,
        canExecute: false,
        uiHint: 'Rules configured in backend; runtime consumption wiring pending',
        reasonIfRejected: 'rule_configured_not_wired',
        freeRemaining,
        moonPackUsesPerMoon: rule.moonPackUsesPerMoon,
        dailyCap: rule.maxTotalDaily,
      };
    }

    return {
      module,
      nextSource: 'NOT_CONFIGURED',
      cost: 0,
      balance,
      canExecute: false,
      reasonIfRejected: 'module_not_configured',
    };
  });

  return {previews};
}

export const getEconomyModulePreviews = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<{ previews: EconomyModulePreview[] }> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');
      const data = (request.data ?? {}) as GetEconomyModulePreviewsData;
      return getEconomyModulePreviewsCore(uid, data.modules);
    }
);
