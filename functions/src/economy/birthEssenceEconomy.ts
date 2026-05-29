import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {
  economyBalanceRef,
  economyLedgerRef,
  economyLifetimeRef,
  economyRequestRef,
  economyUsageDailyRef,
  economyUsageMonthlyRef,
  monthKeyMadrid,
} from './firestorePaths';
import {getPremiumStatus} from './premiumStatus';
import {getEconomyModuleRule} from './rulesCatalog';
import {applyDeckProgressPlan, deckCardUnlockRewardsFromPlan, planDeckProgressFromMoonSpend} from './deckProgress';
import type {
  EconomyBalanceDoc,
  EconomyDailyUsageDoc,
  EconomyDecisionSource,
  EconomyLifetimeDoc,
  EconomyMonthlyUsageDoc,
  EconomyRequestDoc,
} from './types';

type BirthEssenceDailyCounter =
  | 'birthEssenceTotalUsed'
  | 'birthEssenceMoonUsed'
  | 'birthEssencePremiumExtraMoonUsed';

type UsageApplied = {
  dailyCounters?: BirthEssenceDailyCounter[];
  monthlyCounter?: 'birthEssencePremiumIncludedUsed';
  lifetimeFlag?: 'birthEssenceFreeClaimed';
};

type BirthEssenceDecision = {
  source: EconomyDecisionSource;
  moonCost: number;
  reason?: string;
  usageApplied: UsageApplied;
};

export type BirthEssenceEconomyReservationResult =
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

export function resolveBirthEssenceDecision(params: {
  isPremium: boolean;
  balance: number;
  dailyUsage: EconomyDailyUsageDoc;
  monthlyUsage: EconomyMonthlyUsageDoc;
  lifetimeUsage: EconomyLifetimeDoc;
}): BirthEssenceDecision {
  const {isPremium, balance, dailyUsage, monthlyUsage, lifetimeUsage} = params;
  const rule = getEconomyModuleRule('BIRTH_ESSENCE');

  const totalUsed = intValue(dailyUsage.birthEssenceTotalUsed);
  if (totalUsed >= (rule.maxTotalDaily ?? 0)) {
    return {
      source: 'REJECT',
      moonCost: 0,
      reason: 'BIRTH_ESSENCE_DAILY_TOTAL_LIMIT_REACHED',
      usageApplied: {},
    };
  }

  if (lifetimeUsage.birthEssenceFreeClaimed !== true) {
    return {
      source: 'FREE',
      moonCost: 0,
      usageApplied: {
        dailyCounters: ['birthEssenceTotalUsed'],
        lifetimeFlag: 'birthEssenceFreeClaimed',
      },
    };
  }

  const premiumIncludedUsed = intValue(monthlyUsage.birthEssencePremiumIncludedUsed);
  if (isPremium && premiumIncludedUsed < (rule.premiumIncludedMonthly ?? 0)) {
    return {
      source: 'PREMIUM_INCLUDED',
      moonCost: 0,
      usageApplied: {
        dailyCounters: ['birthEssenceTotalUsed'],
        monthlyCounter: 'birthEssencePremiumIncludedUsed',
      },
    };
  }

  const moonUsed = intValue(dailyUsage.birthEssenceMoonUsed);
  if (moonUsed >= (rule.moonExtraDailyMax ?? 0)) {
    return {
      source: 'REJECT',
      moonCost: 0,
      reason: 'BIRTH_ESSENCE_MOON_DAILY_LIMIT_REACHED',
      usageApplied: {},
    };
  }

  const moonCost = isPremium ? 3 : (rule.moonCostPerUse ?? 0);
  if (balance < moonCost) {
    return {
      source: 'REJECT',
      moonCost: 0,
      reason: 'INSUFFICIENT_MOON_BALANCE',
      usageApplied: {},
    };
  }

  const dailyCounters: BirthEssenceDailyCounter[] = [
    'birthEssenceTotalUsed',
    'birthEssenceMoonUsed',
  ];

  if (isPremium) {
    dailyCounters.push('birthEssencePremiumExtraMoonUsed');
  }

  return {
    source: 'MOON',
    moonCost,
    usageApplied: {dailyCounters},
  };
}

export async function reserveBirthEssenceEconomyAccess(params: {
  uid: string;
  requestId: string;
  dateIso: string;
  lang: string;
}): Promise<BirthEssenceEconomyReservationResult> {
  const premium = await getPremiumStatus(params.uid);
  const monthKey = monthKeyMadrid();

  const db = getFirestore();

  return db.runTransaction(async (tx) => {
    const balanceRef = economyBalanceRef(params.uid);
    const dailyRef = economyUsageDailyRef(params.dateIso, params.uid);
    const monthlyRef = economyUsageMonthlyRef(monthKey, params.uid);
    const lifetimeRef = economyLifetimeRef(params.uid);
    const reqRef = economyRequestRef(params.uid, params.requestId);

    const [requestSnap, balanceSnap, dailySnap, monthlySnap, lifetimeSnap] = await Promise.all([
      tx.get(reqRef),
      tx.get(balanceRef),
      tx.get(dailyRef),
      tx.get(monthlyRef),
      tx.get(lifetimeRef),
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
    const monthlyData = (monthlySnap.data() as EconomyMonthlyUsageDoc | undefined) ?? {};
    const lifetimeData = (lifetimeSnap.data() as EconomyLifetimeDoc | undefined) ?? {};

    const decision = resolveBirthEssenceDecision({
      isPremium: premium.isPremium,
      balance: intValue(balanceData.balance),
      dailyUsage: dailyData,
      monthlyUsage: monthlyData,
      lifetimeUsage: lifetimeData,
    });

    if (decision.source === 'REJECT') {
      throw new HttpsError('resource-exhausted', decision.reason ?? 'BIRTH_ESSENCE_UNAVAILABLE');
    }

    const deckProgressPlan = await planDeckProgressFromMoonSpend({
      tx,
      uid: params.uid,
      requestId: params.requestId,
      moonCostCharged: decision.source === 'MOON' ? decision.moonCost : 0,
      source: decision.source,
    });

    const now = Timestamp.now();

    if (decision.usageApplied.dailyCounters && decision.usageApplied.dailyCounters.length > 0) {
      const patch: Record<string, unknown> = {
        updatedAt: now,
      };

      for (const counter of decision.usageApplied.dailyCounters) {
        patch[counter] = FieldValue.increment(1);
      }

      tx.set(dailyRef, patch, {merge: true});
    }

    if (decision.usageApplied.monthlyCounter) {
      tx.set(monthlyRef, {
        [decision.usageApplied.monthlyCounter]: FieldValue.increment(1),
      }, {merge: true});
    }

    if (decision.usageApplied.lifetimeFlag) {
      tx.set(lifetimeRef, {
        [decision.usageApplied.lifetimeFlag]: true,
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
        type: 'BIRTH_ESSENCE_MOON_SPEND',
        amount: -decision.moonCost,
        requestId: params.requestId,
        dateIso: params.dateIso,
        createdAt: now,
      }, {merge: true});
    }

    applyDeckProgressPlan({tx, uid: params.uid, now, plan: deckProgressPlan});

    const requestDoc: EconomyRequestDoc = {
      requestId: params.requestId,
      type: 'BIRTH_ESSENCE',
      result: 'RESERVED',
      status: 'PROCESSING',
      decisionSource: decision.source,
      moonCostCharged: decision.source === 'MOON' ? decision.moonCost : 0,
      usageApplied: decision.usageApplied,
      dateIso: params.dateIso,
      monthKey,
      lang: params.lang,
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

export async function completeBirthEssenceEconomyRequest(params: {
  uid: string;
  requestId: string;
  responsePayload: unknown;
  llmMeta?: unknown;
}) {
  const db = getFirestore();
  const requestRef = economyRequestRef(params.uid, params.requestId);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(requestRef);
    const requestData = snap.data() as EconomyRequestDoc | undefined;
    if (requestData?.status && requestData.status !== 'PROCESSING' && requestData.status !== 'COMPLETED_SUCCESS') {
      console.warn('ECONOMY_COMPLETE_SKIPPED_NON_PROCESSING', {
        uidTag: params.uid.slice(0, 8),
        requestId: params.requestId,
        status: requestData.status,
        result: requestData.result,
      });
      return;
    }

    tx.set(requestRef, {
      status: 'COMPLETED_SUCCESS',
      result: 'COMPLETED_SUCCESS',
      responsePayload: params.responsePayload,
      llmMeta: params.llmMeta,
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  });

  await db.doc(`economyRequests/${params.uid}`).set({
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});
}

export async function refundBirthEssenceEconomyRequest(params: {
  uid: string;
  requestId: string;
  dateIso: string;
  errorMessage: string;
  recoveredResult?: 'FAILED_TIMEOUT' | 'REFUNDED';
}) {
  const db = getFirestore();

  return db.runTransaction(async (tx) => {
    const requestRef = economyRequestRef(params.uid, params.requestId);
    const requestSnap = await tx.get(requestRef);
    if (!requestSnap.exists) return false;

    const requestData = requestSnap.data() as EconomyRequestDoc;
    if (requestData.status !== 'PROCESSING') {
      return false;
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
        dateIso: requestData.dateIso ?? params.dateIso,
        createdAt: now,
      }, {merge: true});
    }

    if (requestData.dateIso && requestData.usageApplied?.dailyCounters) {
      const dailyPatch: Record<string, unknown> = {
        updatedAt: now,
      };

      for (const counter of requestData.usageApplied.dailyCounters) {
        dailyPatch[counter] = FieldValue.increment(-1);
      }

      tx.set(economyUsageDailyRef(requestData.dateIso, params.uid), dailyPatch, {merge: true});
    }

    if (requestData.monthKey && requestData.usageApplied?.monthlyCounter) {
      tx.set(economyUsageMonthlyRef(requestData.monthKey, params.uid), {
        [requestData.usageApplied.monthlyCounter]: FieldValue.increment(-1),
      }, {merge: true});
    }

    if (requestData.usageApplied?.lifetimeFlag) {
      tx.set(economyLifetimeRef(params.uid), {
        [requestData.usageApplied.lifetimeFlag]: false,
      }, {merge: true});
    }

    tx.set(requestRef, {
      status: 'FAILED',
      result: requestData.refundedAt ? 'FAILED' : (params.recoveredResult ?? 'REFUNDED'),
      refundedAt: requestData.refundedAt ?? now,
      error: {
        message: params.errorMessage,
      },
      updatedAt: now,
    }, {merge: true});

    return true;
  });
}
