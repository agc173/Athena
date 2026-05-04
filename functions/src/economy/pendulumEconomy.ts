import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import {
  economyBalanceRef,
  economyLedgerRef,
  economyRequestRef,
  economyUsageDailyRef,
} from "./firestorePaths";
import { getPremiumStatus } from "./premiumStatus";
import { getEconomyModuleRule } from "./rulesCatalog";
import type {
  EconomyBalanceDoc,
  EconomyDailyUsageDoc,
  EconomyDecisionSource,
  EconomyRequestDoc,
} from "./types";

type PendulumCounter =
  | "pendulumFreeUsed"
  | "pendulumPremiumUsed"
  | "pendulumMoonPacksPurchased"
  | "pendulumMoonUsesUsed";
type PendulumDecision = {
  source: EconomyDecisionSource;
  moonCost: number;
  reason?: string;
  usageCounters?: PendulumCounter[];
};
export type PendulumEconomyReservationResult =
  | { type: "completed"; payload: unknown }
  | { type: "in-progress" }
  | { type: "reserved"; source: EconomyDecisionSource; moonCost: number };

function intValue(value: unknown): number {
  if (typeof value !== "number" || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export function resolvePendulumDecision(params: {
  isPremium: boolean;
  balance: number;
  dailyUsage: EconomyDailyUsageDoc;
}): PendulumDecision {
  const rule = getEconomyModuleRule("PENDULUM");
  const freeUsed = intValue(params.dailyUsage.pendulumFreeUsed);
  const premiumUsed = intValue(params.dailyUsage.pendulumPremiumUsed);
  const moonPacksPurchased = intValue(
    params.dailyUsage.pendulumMoonPacksPurchased,
  );
  const moonUsesUsed = intValue(params.dailyUsage.pendulumMoonUsesUsed);
  const packUses = Math.max(1, intValue(rule.moonPackUsesPerMoon));
  const totalUsed = freeUsed + premiumUsed + moonUsesUsed;
  if (
    intValue(rule.maxTotalDaily) > 0 &&
    totalUsed >= intValue(rule.maxTotalDaily)
  )
    return {
      source: "REJECT",
      moonCost: 0,
      reason: "PENDULUM_DAILY_LIMIT_REACHED",
    };
  if (params.isPremium)
    return {
      source: "PREMIUM_INCLUDED",
      moonCost: 0,
      usageCounters: ["pendulumPremiumUsed"],
    };
  if (freeUsed < intValue(rule.freeDaily))
    return { source: "FREE", moonCost: 0, usageCounters: ["pendulumFreeUsed"] };
  const packRemaining = Math.max(
    0,
    moonPacksPurchased * packUses - moonUsesUsed,
  );
  if (packRemaining > 0)
    return {
      source: "MOON",
      moonCost: 0,
      usageCounters: ["pendulumMoonUsesUsed"],
    };
  const cost = intValue(rule.moonCostPerUse);
  if (params.balance < cost)
    return {
      source: "REJECT",
      moonCost: 0,
      reason: "INSUFFICIENT_MOON_BALANCE",
    };
  return {
    source: "MOON",
    moonCost: cost,
    usageCounters: ["pendulumMoonPacksPurchased", "pendulumMoonUsesUsed"],
  };
}

export async function reservePendulumEconomyAccess(params: {
  uid: string;
  requestId: string;
  dateIso: string;
  lang?: string;
}): Promise<PendulumEconomyReservationResult> {
  const premium = await getPremiumStatus(params.uid);
  const db = getFirestore();
  return db.runTransaction(async (tx) => {
    const [requestSnap, balanceSnap, dailySnap] = await Promise.all([
      tx.get(economyRequestRef(params.uid, params.requestId)),
      tx.get(economyBalanceRef(params.uid)),
      tx.get(economyUsageDailyRef(params.dateIso, params.uid)),
    ]);
    if (requestSnap.exists) {
      const existing = requestSnap.data() as EconomyRequestDoc;
      if (existing.status === "COMPLETED_SUCCESS" && existing.responsePayload)
        return { type: "completed", payload: existing.responsePayload };
      if (existing.status === "PROCESSING") return { type: "in-progress" };
      throw new HttpsError(
        "aborted",
        "requestId already failed; retry with a new requestId",
      );
    }
    const balanceData =
      (balanceSnap.data() as EconomyBalanceDoc | undefined) ?? {};
    const dailyData =
      (dailySnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
    const decision = resolvePendulumDecision({
      isPremium: premium.isPremium,
      balance: intValue(balanceData.balance),
      dailyUsage: dailyData,
    });
    if (decision.source === "REJECT")
      throw new HttpsError(
        "resource-exhausted",
        decision.reason ?? "PENDULUM_UNAVAILABLE",
      );
    const now = Timestamp.now();
    if (decision.usageCounters?.length) {
      const patch: Record<string, unknown> = { updatedAt: now };
      for (const c of decision.usageCounters)
        patch[c] = FieldValue.increment(1);
      tx.set(economyUsageDailyRef(params.dateIso, params.uid), patch, {
        merge: true,
      });
    }
    if (decision.source === "MOON" && decision.moonCost > 0) {
      tx.set(
        economyBalanceRef(params.uid),
        {
          balance: intValue(balanceData.balance) - decision.moonCost,
          updatedAt: now,
        },
        { merge: true },
      );
      tx.set(
        economyLedgerRef(params.uid, `${params.requestId}:spend`),
        {
          type: "PENDULUM_MOON_SPEND",
          amount: -decision.moonCost,
          requestId: params.requestId,
          dateIso: params.dateIso,
          createdAt: now,
        },
        { merge: true },
      );
    }
    tx.create(economyRequestRef(params.uid, params.requestId), {
      requestId: params.requestId,
      type: "PENDULUM",
      result: "RESERVED",
      status: "PROCESSING",
      decisionSource: decision.source,
      moonCostCharged: decision.source === "MOON" ? decision.moonCost : 0,
      usageApplied: decision.usageCounters
        ? { dailyCounters: decision.usageCounters }
        : {},
      dateIso: params.dateIso,
      lang: params.lang,
      createdAt: now,
      updatedAt: now,
    } as EconomyRequestDoc);
    return {
      type: "reserved",
      source: decision.source,
      moonCost: decision.source === "MOON" ? decision.moonCost : 0,
    };
  });
}

export async function completePendulumEconomyRequest(params: {
  uid: string;
  requestId: string;
  responsePayload: unknown;
}): Promise<void> {
  await economyRequestRef(params.uid, params.requestId).set(
    {
      result: "COMPLETED_SUCCESS",
      status: "COMPLETED_SUCCESS",
      responsePayload: params.responsePayload,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
}

export async function refundPendulumEconomyRequest(params: {
  uid: string;
  requestId: string;
  dateIso: string;
  errorMessage: string;
}): Promise<void> {
  await getFirestore().runTransaction(async (tx) => {
    const requestRef = economyRequestRef(params.uid, params.requestId);
    const snap = await tx.get(requestRef);
    if (!snap.exists) return;
    const data = snap.data() as EconomyRequestDoc;
    if (data.status === "COMPLETED_SUCCESS" || data.result === "REFUNDED")
      return;
    if (data.decisionSource === "MOON" && (data.moonCostCharged ?? 0) > 0) {
      tx.set(
        economyBalanceRef(params.uid),
        {
          balance: FieldValue.increment(data.moonCostCharged ?? 0),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      tx.set(
        economyLedgerRef(params.uid, `${params.requestId}:refund`),
        {
          type: "PENDULUM_MOON_SPEND",
          amount: data.moonCostCharged ?? 0,
          requestId: params.requestId,
          dateIso: params.dateIso,
          createdAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
    }
    if (data.dateIso && data.usageApplied?.dailyCounters?.length) {
      const p: Record<string, unknown> = {
        updatedAt: FieldValue.serverTimestamp(),
      };
      for (const c of data.usageApplied.dailyCounters)
        p[c] = FieldValue.increment(-1);
      tx.set(economyUsageDailyRef(data.dateIso, params.uid), p, {
        merge: true,
      });
    }
    tx.set(
      requestRef,
      {
        result: "REFUNDED",
        status: "FAILED",
        error: { message: params.errorMessage },
        refundedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
  });
}
