import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {RequestType} from '../oracle/types';
import {
  economyBalanceRef,
  economyLedgerRef,
  economyRequestRef,
  economyUsageDailyRef,
} from './firestorePaths';
import {getPremiumStatus} from './premiumStatus';
import {getEconomyModuleRule} from './rulesCatalog';
import type {
  EconomyBalanceDoc,
  EconomyDailyUsageDoc,
  EconomyDecisionSource,
  EconomyRequestDoc,
} from './types';
import {applyDeckProgressPlan, deckCardUnlockRewardsFromPlan, planDeckProgressFromMoonSpend} from './deckProgress';

type OracleUsageCounter =
  | 'oracleFreeUsed'
  | 'oraclePremiumUsed'
  | 'oracleMoonUsed';

type UsageApplied = {
  dailyCounter?: OracleUsageCounter;
};

type OracleDecision = {
  source: EconomyDecisionSource;
  moonCost: number;
  reason?: string;
  usageApplied: UsageApplied;
};

export type OracleEconomyReservationResult =
  | {type: 'completed'; payload: unknown}
  | {type: 'in-progress'}
  | {
    type: 'reserved';
    source: EconomyDecisionSource;
    moonCost: number;
    deckCardUnlockRewards: {deckId: string; trackId: string; rewardPoolId: string; cardId: string}[];
  };

function intValue(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export function resolveOracleDecision(params: {
  isPremium: boolean;
  balance: number;
  dailyUsage: EconomyDailyUsageDoc;
}): OracleDecision {
  const {isPremium, balance, dailyUsage} = params;
  const rule = getEconomyModuleRule('ORACLE_1Q');

  const freeUsed = intValue(dailyUsage.oracleFreeUsed);
  const premiumUsed = intValue(dailyUsage.oraclePremiumUsed);
  const moonUsed = intValue(dailyUsage.oracleMoonUsed);


  if (isPremium && premiumUsed < (rule.premiumIncludedDaily ?? 0)) {
    return {
      source: 'PREMIUM_INCLUDED',
      moonCost: 0,
      usageApplied: {dailyCounter: 'oraclePremiumUsed'},
    };
  }

  if (freeUsed < (rule.freeDaily ?? 0)) {
    return {
      source: 'FREE',
      moonCost: 0,
      usageApplied: {dailyCounter: 'oracleFreeUsed'},
    };
  }

  if (moonUsed >= (rule.moonExtraDailyMax ?? 0)) {
    return {
      source: 'REJECT',
      moonCost: 0,
      reason: 'ORACLE_MOON_DAILY_LIMIT_REACHED',
      usageApplied: {},
    };
  }

  if (isPremium) {
    const premiumDailyMax = rule.premiumDailyMax ?? 0;
    const totalPremiumDayUsed = premiumUsed + moonUsed;
    if (premiumDailyMax > 0 && totalPremiumDayUsed >= premiumDailyMax) {
      return {
        source: 'REJECT',
        moonCost: 0,
        reason: 'ORACLE_PREMIUM_DAILY_LIMIT_REACHED',
        usageApplied: {},
      };
    }
  }

  const moonCost = rule.moonCostPerUse ?? 0;
  if (balance < moonCost) {
    return {
      source: 'REJECT',
      moonCost: 0,
      reason: 'INSUFFICIENT_MOON_BALANCE',
      usageApplied: {},
    };
  }

  return {
    source: 'MOON',
    moonCost,
    usageApplied: {dailyCounter: 'oracleMoonUsed'},
  };
}

export async function reserveOracleEconomyAccess(params: {
  uid: string;
  requestId: string;
  dateIso: string;
  lang: string;
  question: string;
}): Promise<OracleEconomyReservationResult> {
  const premium = await getPremiumStatus(params.uid);

  const db = getFirestore();

  return db.runTransaction(async (tx) => {
    const balanceRef = economyBalanceRef(params.uid);
    const dailyRef = economyUsageDailyRef(params.dateIso, params.uid);
    const reqRef = economyRequestRef(params.uid, params.requestId);

    const [requestSnap, balanceSnap, dailySnap] = await Promise.all([
      tx.get(reqRef),
      tx.get(balanceRef),
      tx.get(dailyRef),
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

    const decision = resolveOracleDecision({
      isPremium: premium.isPremium,
      balance: intValue(balanceData.balance),
      dailyUsage: dailyData,
    });

    if (decision.source === 'REJECT') {
      throw new HttpsError('resource-exhausted', decision.reason ?? 'ORACLE_UNAVAILABLE');
    }
    const deckProgressPlan = await planDeckProgressFromMoonSpend({
      tx,
      uid: params.uid,
      requestId: params.requestId,
      moonCostCharged: decision.source === 'MOON' ? decision.moonCost : 0,
      source: decision.source,
    });

    const now = Timestamp.now();

    if (decision.usageApplied.dailyCounter) {
      tx.set(dailyRef, {
        [decision.usageApplied.dailyCounter]: FieldValue.increment(1),
        updatedAt: now,
      }, {merge: true});
    }

    if (decision.source === 'MOON' && decision.moonCost > 0) {
      const currentBalance = intValue(balanceData.balance);
      const nextBalance = currentBalance - decision.moonCost;

      tx.set(balanceRef, {
        balance: nextBalance,
        updatedAt: now,
      }, {merge: true});

      tx.set(economyLedgerRef(params.uid, `${params.requestId}:spend`), {
        type: 'ORACLE_1Q_MOON_SPEND',
        amount: -decision.moonCost,
        requestId: params.requestId,
        dateIso: params.dateIso,
        createdAt: now,
      }, {merge: true});
    }

    applyDeckProgressPlan({tx, uid: params.uid, now, plan: deckProgressPlan});

    const requestDoc: EconomyRequestDoc = {
      requestId: params.requestId,
      type: RequestType.ORACLE_1Q,
      result: 'RESERVED',
      status: 'PROCESSING',
      decisionSource: decision.source,
      moonCostCharged: decision.source === 'MOON' ? decision.moonCost : 0,
      usageApplied: decision.usageApplied,
      dateIso: params.dateIso,
      lang: params.lang,
      question: params.question,
      createdAt: now,
      updatedAt: now,
    };

    tx.create(reqRef, requestDoc);

    return {
      type: 'reserved' as const,
      source: decision.source,
      moonCost: decision.source === 'MOON' ? decision.moonCost : 0,
      deckCardUnlockRewards: deckCardUnlockRewardsFromPlan(deckProgressPlan),
    };
  });
}

export async function completeOracleEconomyRequest(params: {
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

export async function refundOracleEconomyRequest(params: {
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
