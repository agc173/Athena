import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {RequestType} from '../oracle/types';
import {
  economyBalanceRef,
  economyLedgerRef,
  economyRequestRef,
  economyUsageDailyRef,
  economyUsageWeeklyRef,
  weekKeyMadrid,
} from './firestorePaths';
import {getPremiumStatus} from './premiumStatus';
import {getEconomyModuleRule} from './rulesCatalog';
import type {
  EconomyBalanceDoc,
  EconomyDailyUsageDoc,
  EconomyDecisionSource,
  EconomyRequestDoc,
  EconomyWeeklyUsageDoc,
} from './types';

type TarotRequestType = RequestType.TAROT_1 | RequestType.TAROT_3;

type TarotUsageCounter =
  | 'tarot1FreeUsed'
  | 'tarot1PremiumUsed'
  | 'tarot1MoonUsed'
  | 'tarot3PremiumUsed'
  | 'tarot3MoonUsed'
  | 'tarot3FreeUsed';

type UsageApplied = {
  dailyCounter?: TarotUsageCounter;
  weeklyCounter?: TarotUsageCounter;
};

type TarotDecision = {
  source: EconomyDecisionSource;
  moonCost: number;
  reason?: string;
  usageApplied: UsageApplied;
};

function stripUndefinedDeep<T>(value: T): T {
  if (Array.isArray(value)) return value.map((item) => stripUndefinedDeep(item)) as T;
  if (value && typeof value === 'object') {
    const out: Record<string, unknown> = {};
    for (const [key, nested] of Object.entries(value as Record<string, unknown>)) {
      if (nested !== undefined) out[key] = stripUndefinedDeep(nested);
    }
    return out as T;
  }
  return value;
}

export type TarotEconomyReservationResult =
  | {type: 'completed'; payload: unknown}
  | {type: 'in-progress'}
  | {
    type: 'reserved';
    source: EconomyDecisionSource;
    moonCost: number;
  };

function intValue(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

function assertTarotRequestType(requestType: RequestType): TarotRequestType {
  if (requestType !== RequestType.TAROT_1 && requestType !== RequestType.TAROT_3) {
    throw new HttpsError('invalid-argument', 'requestType must be TAROT_1 or TAROT_3');
  }
  return requestType;
}

export function resolveTarotDecision(params: {
  requestType: TarotRequestType;
  isPremium: boolean;
  balance: number;
  dailyUsage: EconomyDailyUsageDoc;
  weeklyUsage: EconomyWeeklyUsageDoc;
}): TarotDecision {
  const {requestType, isPremium, balance, dailyUsage, weeklyUsage} = params;

  if (requestType === RequestType.TAROT_1) {
    const rule = getEconomyModuleRule('TAROT_1');
    const freeUsed = intValue(dailyUsage.tarot1FreeUsed);
    const premiumUsed = intValue(dailyUsage.tarot1PremiumUsed);
    const moonUsed = intValue(dailyUsage.tarot1MoonUsed);

    if (freeUsed < (rule.freeDaily ?? 0)) {
      return {source: 'FREE', moonCost: 0, usageApplied: {dailyCounter: 'tarot1FreeUsed'}};
    }

    if (isPremium && premiumUsed < (rule.premiumIncludedDaily ?? 0)) {
      return {
        source: 'PREMIUM_INCLUDED',
        moonCost: 0,
        usageApplied: {dailyCounter: 'tarot1PremiumUsed'},
      };
    }

    if (moonUsed < (rule.moonExtraDailyMax ?? 0)) {
      const moonCost = rule.moonCostPerUse ?? 0;
      if (balance >= moonCost) {
        return {source: 'MOON', moonCost, usageApplied: {dailyCounter: 'tarot1MoonUsed'}};
      }
      return {source: 'REJECT', moonCost: 0, reason: 'INSUFFICIENT_MOON_BALANCE', usageApplied: {}};
    }

    return {source: 'REJECT', moonCost: 0, reason: 'TAROT_1_DAILY_LIMIT_REACHED', usageApplied: {}};
  }

  const rule = getEconomyModuleRule('TAROT_3');
  const freeWeeklyUsed = intValue(weeklyUsage.tarot3FreeUsed);
  const premiumUsed = intValue(dailyUsage.tarot3PremiumUsed);
  const moonUsed = intValue(dailyUsage.tarot3MoonUsed);

  if (freeWeeklyUsed < (rule.freeWeekly ?? 0)) {
    return {
      source: 'FREE',
      moonCost: 0,
      usageApplied: {weeklyCounter: 'tarot3FreeUsed'},
    };
  }

  const premiumDailyMax = rule.premiumDailyMax ?? 0;
  const totalPremiumDayUsed = premiumUsed + moonUsed;

  if (isPremium && premiumUsed < (rule.premiumIncludedDaily ?? 0)) {
    if (premiumDailyMax === 0 || totalPremiumDayUsed < premiumDailyMax) {
      return {
        source: 'PREMIUM_INCLUDED',
        moonCost: 0,
        usageApplied: {dailyCounter: 'tarot3PremiumUsed'},
      };
    }
    return {source: 'REJECT', moonCost: 0, reason: 'TAROT_3_PREMIUM_DAILY_LIMIT_REACHED', usageApplied: {}};
  }

  if (moonUsed >= (rule.moonExtraDailyMax ?? 0)) {
    return {source: 'REJECT', moonCost: 0, reason: 'TAROT_3_MOON_DAILY_LIMIT_REACHED', usageApplied: {}};
  }

  if (isPremium && premiumDailyMax > 0 && totalPremiumDayUsed >= premiumDailyMax) {
    return {source: 'REJECT', moonCost: 0, reason: 'TAROT_3_PREMIUM_DAILY_LIMIT_REACHED', usageApplied: {}};
  }

  const moonCost = rule.moonCostPerUse ?? 0;
  if (balance < moonCost) {
    return {source: 'REJECT', moonCost: 0, reason: 'INSUFFICIENT_MOON_BALANCE', usageApplied: {}};
  }

  return {
    source: 'MOON',
    moonCost,
    usageApplied: {dailyCounter: 'tarot3MoonUsed'},
  };
}

export async function reserveTarotEconomyAccess(params: {
  uid: string;
  requestId: string;
  requestType: RequestType;
  dateIso: string;
  lang: string;
  question?: string;
}): Promise<TarotEconomyReservationResult> {
  const requestType = assertTarotRequestType(params.requestType);
  const premium = await getPremiumStatus(params.uid);

  const db = getFirestore();
  const weekKey = weekKeyMadrid();

  return db.runTransaction(async (tx) => {
    const balanceRef = economyBalanceRef(params.uid);
    const dailyRef = economyUsageDailyRef(params.dateIso, params.uid);
    const weeklyRef = economyUsageWeeklyRef(weekKey, params.uid);
    const reqRef = economyRequestRef(params.uid, params.requestId);

    const [requestSnap, balanceSnap, dailySnap, weeklySnap] = await Promise.all([
      tx.get(reqRef),
      tx.get(balanceRef),
      tx.get(dailyRef),
      tx.get(weeklyRef),
    ]);

    if (requestSnap.exists) {
      const existing = requestSnap.data() as EconomyRequestDoc;
      if (existing.status === 'COMPLETED_SUCCESS' && existing.responsePayload) {
        return {type: 'completed' as const, payload: existing.responsePayload};
      }

      if (existing.status === 'PROCESSING') {
        return {type: 'in-progress' as const};
      }

      throw new HttpsError('aborted', 'requestId already failed; retry with a new requestId');
    }

    const balanceData = (balanceSnap.data() as EconomyBalanceDoc | undefined) ?? {};
    const dailyData = (dailySnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
    const weeklyData = (weeklySnap.data() as EconomyWeeklyUsageDoc | undefined) ?? {};

    const decision = resolveTarotDecision({
      requestType,
      isPremium: premium.isPremium,
      balance: intValue(balanceData.balance),
      dailyUsage: dailyData,
      weeklyUsage: weeklyData,
    });

    if (decision.source === 'REJECT') {
      throw new HttpsError('resource-exhausted', decision.reason ?? 'TAROT_UNAVAILABLE');
    }

    const now = Timestamp.now();

    if (decision.usageApplied.dailyCounter) {
      tx.set(dailyRef, {
        [decision.usageApplied.dailyCounter]: FieldValue.increment(1),
        updatedAt: now,
      }, {merge: true});
    }

    if (decision.usageApplied.weeklyCounter) {
      tx.set(weeklyRef, {
        [decision.usageApplied.weeklyCounter]: FieldValue.increment(1),
      }, {merge: true});
    }

    if (decision.source === 'MOON' && decision.moonCost > 0) {
      const currentBalance = intValue(balanceData.balance);
      const nextBalance = currentBalance - decision.moonCost;

      tx.set(balanceRef, {
        balance: nextBalance,
        updatedAt: now,
      }, {merge: true});

      const spendType = requestType === RequestType.TAROT_1 ?
        'TAROT_1_MOON_SPEND' :
        'TAROT_3_MOON_SPEND';
      tx.set(economyLedgerRef(params.uid, `${params.requestId}:spend`), {
        type: spendType,
        amount: -decision.moonCost,
        requestId: params.requestId,
        dateIso: params.dateIso,
        createdAt: now,
      }, {merge: true});
    }

    const requestDoc = stripUndefinedDeep<EconomyRequestDoc>({
      requestId: params.requestId,
      type: requestType,
      result: 'RESERVED',
      status: 'PROCESSING',
      decisionSource: decision.source,
      moonCostCharged: decision.source === 'MOON' ? decision.moonCost : 0,
      usageApplied: decision.usageApplied,
      dateIso: params.dateIso,
      weekKey,
      lang: params.lang,
      question: params.question,
      createdAt: now,
      updatedAt: now,
    });

    tx.create(reqRef, requestDoc);

    return {
      type: 'reserved' as const,
      source: decision.source,
      moonCost: decision.source === 'MOON' ? decision.moonCost : 0,
    };
  });
}

export async function completeTarotEconomyRequest(params: {
  uid: string;
  requestId: string;
  responsePayload: unknown;
  llmMeta?: unknown;
}) {
  const db = getFirestore();
  const requestRef = economyRequestRef(params.uid, params.requestId);

  await requestRef.set({
    status: 'COMPLETED_SUCCESS',
    result: 'COMPLETED_SUCCESS',
    responsePayload: params.responsePayload,
    llmMeta: params.llmMeta,
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});

  await db.doc(`economyRequests/${params.uid}`).set({
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});
}

export async function refundTarotEconomyRequest(params: {
  uid: string;
  requestId: string;
  dateIso: string;
  errorMessage: string;
}) {
  const db = getFirestore();

  await db.runTransaction(async (tx) => {
    const requestRef = economyRequestRef(params.uid, params.requestId);
    const requestSnap = await tx.get(requestRef);
    if (!requestSnap.exists) return;

    const requestData = requestSnap.data() as EconomyRequestDoc;
    if (requestData.status !== 'PROCESSING') {
      return;
    }

    const now = Timestamp.now();
    const balanceRef = economyBalanceRef(params.uid);

    if (!requestData.refundedAt && (requestData.moonCostCharged ?? 0) > 0) {
      tx.set(balanceRef, {
        balance: FieldValue.increment(requestData.moonCostCharged ?? 0),
        updatedAt: now,
      }, {merge: true});

      tx.set(economyLedgerRef(params.uid, `${params.requestId}:refund`), {
        type: 'REFUND',
        amount: requestData.moonCostCharged,
        requestId: params.requestId,
        dateIso: params.dateIso,
        createdAt: now,
      }, {merge: true});
    }

    if (requestData.dateIso && requestData.usageApplied?.dailyCounter) {
      tx.set(economyUsageDailyRef(requestData.dateIso, params.uid), {
        [requestData.usageApplied.dailyCounter]: FieldValue.increment(-1),
        updatedAt: now,
      }, {merge: true});
    }

    if (requestData.weekKey && requestData.usageApplied?.weeklyCounter) {
      tx.set(economyUsageWeeklyRef(requestData.weekKey, params.uid), {
        [requestData.usageApplied.weeklyCounter]: FieldValue.increment(-1),
      }, {merge: true});
    }

    tx.set(requestRef, {
      status: 'FAILED',
      result: requestData.refundedAt ? 'FAILED' : 'REFUNDED',
      refundedAt: requestData.refundedAt ?? now,
      error: {
        message: params.errorMessage,
      },
      updatedAt: now,
    }, {merge: true});
  });
}
