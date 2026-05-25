import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {
  economyBalanceRef,
  economyLedgerRef,
  economyRequestRef,
  economyUsageDailyRef,
} from './firestorePaths';
import {getPremiumStatus} from './premiumStatus';
import {getEconomyModuleRule} from './rulesCatalog';
import type {EconomyBalanceDoc, EconomyDailyUsageDoc, EconomyDecisionSource, EconomyRequestDoc} from './types';
import {applyDeckProgressPlan, deckCardUnlockRewardsFromPlan, planDeckProgressFromMoonSpend} from './deckProgress';

type SynastryCounter = 'synastryFreeUsed' | 'synastryPremiumUsed' | 'synastryMoonPacksPurchased' | 'synastryMoonUsesUsed';
type SynastryDecision = {
  source: EconomyDecisionSource;
  moonCost: number;
  reason?: string;
  usageCounters?: SynastryCounter[];
  packUsesRemainingAfter?: number;
};
export type SynastryEconomyReservationResult =
  | {type: 'completed'; payload: unknown}
  | {type: 'in-progress'}
  | {type: 'reserved'; source: EconomyDecisionSource; moonCost: number; deckCardUnlockRewards: {deckId: string; trackId: string; rewardPoolId: string; cardId: string}[]};

function intValue(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export function resolveSynastryDecision(params: {isPremium: boolean; balance: number; dailyUsage: EconomyDailyUsageDoc}): SynastryDecision {
  const rule = getEconomyModuleRule('SYNASTRY');
  const freeUsed = intValue(params.dailyUsage.synastryFreeUsed);
  const premiumUsed = intValue(params.dailyUsage.synastryPremiumUsed);
  const moonPacksPurchased = intValue(params.dailyUsage.synastryMoonPacksPurchased);
  const moonUsesUsed = intValue(params.dailyUsage.synastryMoonUsesUsed);
  const packUses = Math.max(1, intValue(rule.moonPackUsesPerMoon));
  const freeDaily = intValue(rule.freeDaily);
  const dailyCap = intValue(rule.maxTotalDaily);
  const premiumDailyMax = intValue(rule.premiumDailyMax);
  const totalUsed = freeUsed + premiumUsed + moonUsesUsed;
  const packUsesTotal = moonPacksPurchased * packUses;
  const packUsesRemaining = Math.max(0, packUsesTotal - moonUsesUsed);

  if (dailyCap > 0 && totalUsed >= dailyCap) return {source: 'REJECT', moonCost: 0, reason: 'SYNASTRY_DAILY_LIMIT_REACHED'};
  if (params.isPremium && premiumDailyMax > 0 && premiumUsed >= premiumDailyMax) {
    return {source: 'REJECT', moonCost: 0, reason: 'SYNASTRY_PREMIUM_DAILY_LIMIT_REACHED'};
  }
  if (params.isPremium) return {source: 'PREMIUM_INCLUDED', moonCost: 0, usageCounters: ['synastryPremiumUsed']};
  if (freeUsed < freeDaily) return {source: 'FREE', moonCost: 0, usageCounters: ['synastryFreeUsed']};

  const remainingForCap = dailyCap > 0 ? dailyCap - totalUsed : 1;
  if (remainingForCap <= 0) return {source: 'REJECT', moonCost: 0, reason: 'SYNASTRY_DAILY_LIMIT_REACHED'};
  if (packUsesRemaining > 0) {
    return {source: 'MOON', moonCost: 0, usageCounters: ['synastryMoonUsesUsed'], packUsesRemainingAfter: packUsesRemaining - 1};
  }
  const cost = intValue(rule.moonCostPerUse);
  if (params.balance < cost) return {source: 'REJECT', moonCost: 0, reason: 'INSUFFICIENT_MOON_BALANCE'};
  return {
    source: 'MOON',
    moonCost: cost,
    usageCounters: ['synastryMoonPacksPurchased', 'synastryMoonUsesUsed'],
    packUsesRemainingAfter: Math.max(0, packUses - 1),
  };
}

export async function reserveSynastryEconomyAccess(params: {uid: string; requestId: string; dateIso: string; lang?: string}): Promise<SynastryEconomyReservationResult> {
  const premium = await getPremiumStatus(params.uid);
  const db = getFirestore();
  return db.runTransaction(async (tx) => {
    const balanceRef = economyBalanceRef(params.uid);
    const dailyRef = economyUsageDailyRef(params.dateIso, params.uid);
    const reqRef = economyRequestRef(params.uid, params.requestId);
    const [requestSnap, balanceSnap, dailySnap] = await Promise.all([tx.get(reqRef), tx.get(balanceRef), tx.get(dailyRef)]);
    if (requestSnap.exists) {
      const existing = requestSnap.data() as EconomyRequestDoc;
      if (existing.status === 'COMPLETED_SUCCESS' && existing.responsePayload) return {type: 'completed', payload: existing.responsePayload};
      if (existing.status === 'PROCESSING') return {type: 'in-progress'};
      throw new HttpsError('aborted', 'requestId already failed; retry with a new requestId');
    }
    const balanceData = (balanceSnap.data() as EconomyBalanceDoc | undefined) ?? {};
    const dailyData = (dailySnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
    const decision = resolveSynastryDecision({isPremium: premium.isPremium, balance: intValue(balanceData.balance), dailyUsage: dailyData});
    if (decision.source === 'REJECT') throw new HttpsError('resource-exhausted', decision.reason ?? 'SYNASTRY_UNAVAILABLE');
    const deckProgressPlan = await planDeckProgressFromMoonSpend({
      tx,
      uid: params.uid,
      requestId: params.requestId,
      moonCostCharged: decision.source === 'MOON' ? decision.moonCost : 0,
      source: decision.source,
    });
    const now = Timestamp.now();
    if (decision.usageCounters && decision.usageCounters.length > 0) {
      const patch: Record<string, unknown> = {updatedAt: now};
      for (const counter of decision.usageCounters) patch[counter] = FieldValue.increment(1);
      tx.set(dailyRef, patch, {merge: true});
    }
    if (decision.source === 'MOON' && decision.moonCost > 0) {
      tx.set(balanceRef, {balance: intValue(balanceData.balance) - decision.moonCost, updatedAt: now}, {merge: true});
      tx.set(economyLedgerRef(params.uid, `${params.requestId}:spend`), {
        type: 'SYNASTRY_MOON_SPEND', amount: -decision.moonCost, requestId: params.requestId, dateIso: params.dateIso, createdAt: now,
      }, {merge: true});
    }
    applyDeckProgressPlan({tx, uid: params.uid, now, plan: deckProgressPlan});
    tx.create(reqRef, {
      requestId: params.requestId,
      type: 'SYNASTRY',
      result: 'RESERVED',
      status: 'PROCESSING',
      decisionSource: decision.source,
      moonCostCharged: decision.source === 'MOON' ? decision.moonCost : 0,
      usageApplied: decision.usageCounters ? {dailyCounters: decision.usageCounters} : {},
      dateIso: params.dateIso,
      lang: params.lang,
      createdAt: now,
      updatedAt: now,
    } as EconomyRequestDoc);
    return {
      type: 'reserved',
      source: decision.source,
      moonCost: decision.source === 'MOON' ? decision.moonCost : 0,
      deckCardUnlockRewards: deckCardUnlockRewardsFromPlan(deckProgressPlan),
    };
  });
}

export async function completeSynastryEconomyRequest(params: {uid: string; requestId: string; responsePayload: unknown}): Promise<void> {
  const requestRef = economyRequestRef(params.uid, params.requestId);
  await requestRef.set({
    result: 'COMPLETED_SUCCESS',
    status: 'COMPLETED_SUCCESS',
    responsePayload: params.responsePayload,
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});
}

export async function refundSynastryEconomyRequest(params: {uid: string; requestId: string; dateIso: string; errorMessage: string}): Promise<void> {
  const db = getFirestore();
  await db.runTransaction(async (tx) => {
    const requestRef = economyRequestRef(params.uid, params.requestId);
    const requestSnap = await tx.get(requestRef);
    if (!requestSnap.exists) return;
    const requestData = requestSnap.data() as EconomyRequestDoc;
    if (requestData.status === 'COMPLETED_SUCCESS' || requestData.result === 'REFUNDED') return;
    if (requestData.decisionSource === 'MOON' && (requestData.moonCostCharged ?? 0) > 0) {
      tx.set(economyBalanceRef(params.uid), {balance: FieldValue.increment(requestData.moonCostCharged ?? 0), updatedAt: FieldValue.serverTimestamp()}, {merge: true});
      tx.set(economyLedgerRef(params.uid, `${params.requestId}:refund`), {
        type: 'SYNASTRY_MOON_SPEND', amount: requestData.moonCostCharged ?? 0, requestId: params.requestId, dateIso: params.dateIso, createdAt: FieldValue.serverTimestamp(),
      }, {merge: true});
    }
    if (requestData.dateIso && requestData.usageApplied?.dailyCounters?.length) {
      const dailyPatch: Record<string, unknown> = {updatedAt: FieldValue.serverTimestamp()};
      for (const counter of requestData.usageApplied.dailyCounters) dailyPatch[counter] = FieldValue.increment(-1);
      tx.set(economyUsageDailyRef(requestData.dateIso, params.uid), dailyPatch, {merge: true});
    }
    tx.set(requestRef, {result: 'REFUNDED', status: 'FAILED', error: {message: params.errorMessage}, refundedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()}, {merge: true});
  });
}
