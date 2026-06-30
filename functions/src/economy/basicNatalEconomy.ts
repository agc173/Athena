import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
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
import type {EconomyBalanceDoc, EconomyDailyUsageDoc, EconomyDecisionSource, EconomyRequestDoc, EconomyWeeklyUsageDoc} from './types';

type BasicNatalDailyCounter = 'basicNatalPremiumUsed' | 'basicNatalMoonUsed';
type BasicNatalWeeklyCounter = 'basicNatalFreeUsed';

type UsageApplied = {
  dailyCounter?: BasicNatalDailyCounter;
  weeklyCounter?: BasicNatalWeeklyCounter;
};

type BasicNatalDecision = {
  source: EconomyDecisionSource;
  moonCost: number;
  reason?: string;
  usageApplied: UsageApplied;
};

export type BasicNatalEconomyReservationResult =
  | {type: 'completed'; payload: unknown}
  | {type: 'in-progress'}
  | {type: 'reserved'; source: EconomyDecisionSource; moonCost: number};

function intValue(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export function resolveBasicNatalDecision(params: {
  isPremium: boolean;
  balance: number;
  dailyUsage: EconomyDailyUsageDoc;
  weeklyUsage: EconomyWeeklyUsageDoc;
}): BasicNatalDecision {
  const {isPremium, balance, dailyUsage, weeklyUsage} = params;
  const rule = getEconomyModuleRule('BASIC_NATAL_CHART');

  const freeWeeklyUsed = intValue(weeklyUsage.basicNatalFreeUsed);
  if (freeWeeklyUsed < (rule.freeWeekly ?? 0)) {
    return {source: 'FREE', moonCost: 0, usageApplied: {weeklyCounter: 'basicNatalFreeUsed'}};
  }

  const premiumUsed = intValue(dailyUsage.basicNatalPremiumUsed);
  const moonUsed = intValue(dailyUsage.basicNatalMoonUsed);
  const premiumDailyMax = rule.premiumDailyMax ?? rule.premiumIncludedDaily ?? 0;
  const totalPremiumDayUsed = premiumUsed + moonUsed;

  if (isPremium && premiumUsed < (rule.premiumIncludedDaily ?? 0)) {
    if (premiumDailyMax === 0 || totalPremiumDayUsed < premiumDailyMax) {
      return {source: 'PREMIUM_INCLUDED', moonCost: 0, usageApplied: {dailyCounter: 'basicNatalPremiumUsed'}};
    }
    return {source: 'REJECT', moonCost: 0, reason: 'BASIC_NATAL_DAILY_LIMIT_REACHED', usageApplied: {}};
  }

  if (isPremium && premiumDailyMax > 0 && totalPremiumDayUsed >= premiumDailyMax) {
    return {source: 'REJECT', moonCost: 0, reason: 'BASIC_NATAL_DAILY_LIMIT_REACHED', usageApplied: {}};
  }

  const moonCost = rule.moonCostPerUse ?? 0;
  if (balance < moonCost) {
    return {source: 'REJECT', moonCost: 0, reason: 'INSUFFICIENT_MOON_BALANCE', usageApplied: {}};
  }

  return {source: 'MOON', moonCost, usageApplied: {dailyCounter: 'basicNatalMoonUsed'}};
}

export async function reserveBasicNatalEconomyAccess(params: {
  uid: string;
  requestId: string;
  dateIso: string;
  lang?: string;
}): Promise<BasicNatalEconomyReservationResult> {
  const premium = await getPremiumStatus(params.uid);
  const db = getFirestore();
  const weekKey = weekKeyMadrid();

  return db.runTransaction(async (tx) => {
    const balanceRef = economyBalanceRef(params.uid);
    const dailyRef = economyUsageDailyRef(params.dateIso, params.uid);
    const weeklyRef = economyUsageWeeklyRef(weekKey, params.uid);
    const reqRef = economyRequestRef(params.uid, params.requestId);

    const [requestSnap, balanceSnap, dailySnap, weeklySnap] = await Promise.all([
      tx.get(reqRef), tx.get(balanceRef), tx.get(dailyRef), tx.get(weeklyRef),
    ]);

    if (requestSnap.exists) {
      const existing = requestSnap.data() as EconomyRequestDoc;
      if (existing.status === 'COMPLETED_SUCCESS' && existing.responsePayload) return {type: 'completed' as const, payload: existing.responsePayload};
      if (existing.status === 'PROCESSING') return {type: 'in-progress' as const};
      throw new HttpsError('aborted', 'requestId already failed; retry with a new requestId');
    }

    const balanceData = (balanceSnap.data() as EconomyBalanceDoc | undefined) ?? {};
    const dailyData = (dailySnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
    const weeklyData = (weeklySnap.data() as EconomyWeeklyUsageDoc | undefined) ?? {};
    const decision = resolveBasicNatalDecision({isPremium: premium.isPremium, balance: intValue(balanceData.balance), dailyUsage: dailyData, weeklyUsage: weeklyData});

    if (decision.source === 'REJECT') throw new HttpsError('resource-exhausted', decision.reason ?? 'BASIC_NATAL_UNAVAILABLE');

    const now = Timestamp.now();
    if (decision.usageApplied.dailyCounter) {
      tx.set(dailyRef, {[decision.usageApplied.dailyCounter]: FieldValue.increment(1), updatedAt: now}, {merge: true});
    }
    if (decision.usageApplied.weeklyCounter) {
      tx.set(weeklyRef, {[decision.usageApplied.weeklyCounter]: FieldValue.increment(1)}, {merge: true});
    }
    if (decision.source === 'MOON' && decision.moonCost > 0) {
      tx.set(balanceRef, {balance: intValue(balanceData.balance) - decision.moonCost, updatedAt: now}, {merge: true});
      tx.set(economyLedgerRef(params.uid, `${params.requestId}:spend`), {
        type: 'BASIC_NATAL_CHART_MOON_SPEND', amount: -decision.moonCost, requestId: params.requestId, dateIso: params.dateIso, createdAt: now,
      }, {merge: true});
    }

    tx.create(reqRef, {
      requestId: params.requestId,
      type: 'BASIC_NATAL_CHART',
      result: 'RESERVED',
      status: 'PROCESSING',
      decisionSource: decision.source,
      moonCostCharged: decision.source === 'MOON' ? decision.moonCost : 0,
      usageApplied: decision.usageApplied,
      dateIso: params.dateIso,
      weekKey,
      lang: params.lang,
      createdAt: now,
      updatedAt: now,
    } as EconomyRequestDoc);

    return {type: 'reserved' as const, source: decision.source, moonCost: decision.source === 'MOON' ? decision.moonCost : 0};
  });
}

export async function completeBasicNatalEconomyRequest(params: {uid: string; requestId: string; responsePayload: unknown}) {
  const db = getFirestore();
  await db.doc(`economyRequests/${params.uid}/requests/${params.requestId}`).set({
    status: 'COMPLETED_SUCCESS', result: 'COMPLETED_SUCCESS', responsePayload: params.responsePayload, updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});
  await db.doc(`economyRequests/${params.uid}`).set({updatedAt: FieldValue.serverTimestamp()}, {merge: true});
}
