import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {logger} from 'firebase-functions';
import {type ZodiacSign} from '../../firestore/paths';
import {
  dateIsoMadrid,
  economyBalanceRef,
  economyHoroscopeUnlockRef,
  economyLedgerRef,
  economyRequestRef,
  economyUsageDailyRef,
} from '../firestorePaths';
import {getPremiumStatus} from '../premiumStatus';
import {getEconomyModuleRule} from '../rulesCatalog';
import type {
  EconomyBalanceDoc,
  EconomyDailyUsageDoc,
  EconomyLedgerEntryDoc,
  EconomyRequestDoc,
  UnlockHoroscopeMonthlyResponse,
  UnlockHoroscopeWeeklyResponse,
} from '../types';

type UnlockHoroscopePeriodResponse =
  | UnlockHoroscopeWeeklyResponse
  | UnlockHoroscopeMonthlyResponse;

type UnlockHoroscopePeriodConfig = {
  periodType: 'weekly' | 'monthly';
  requestType: 'HOROSCOPE_UNLOCK_WEEKLY' | 'HOROSCOPE_UNLOCK_MONTHLY';
  module: 'HOROSCOPE_WEEKLY' | 'HOROSCOPE_MONTHLY';
  ledgerType: 'HOROSCOPE_WEEKLY_MOON_SPEND' | 'HOROSCOPE_MONTHLY_MOON_SPEND';
  keyField: 'weekKey' | 'monthKey';
  unlockKeyPrefix: 'weekly' | 'monthly';
  usageCounter: 'horoscopeWeeklyMoonUsed' | 'horoscopeMonthlyMoonUsed';
  normalizePeriodKey: (value: unknown) => string;
  assertAllowedPeriodKey: (periodKey: string) => void;
  source: 'unlockHoroscopeWeekly' | 'unlockHoroscopeMonthly';
};

type UnlockHoroscopePeriodInput = {
  requestId?: unknown;
  sign?: unknown;
  weekKey?: unknown;
  monthKey?: unknown;
};

const VALID_SIGNS: ZodiacSign[] = ['aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo', 'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces'];

function normalizeRequestId(value: unknown): string {
  if (typeof value !== 'string' || !value.trim()) throw new HttpsError('invalid-argument', 'requestId is required');
  const trimmed = value.trim();
  if (trimmed.length > 100) throw new HttpsError('invalid-argument', 'requestId is too long');
  return trimmed;
}

function normalizeSign(value: unknown): ZodiacSign {
  if (typeof value !== 'string') throw new HttpsError('invalid-argument', 'sign is required');
  const normalized = value.trim().toLowerCase() as ZodiacSign;
  if (!VALID_SIGNS.includes(normalized)) throw new HttpsError('invalid-argument', 'Invalid sign');
  return normalized;
}

function asCount(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export async function unlockHoroscopePeriod<TResponse extends UnlockHoroscopePeriodResponse>(
    uid: string,
    rawData: UnlockHoroscopePeriodInput,
    config: UnlockHoroscopePeriodConfig
): Promise<TResponse> {
  const requestId = normalizeRequestId(rawData.requestId);
  const periodKey = config.normalizePeriodKey(rawData[config.keyField]);
  const sign = normalizeSign(rawData.sign);

  config.assertAllowedPeriodKey(periodKey);

  const premium = await getPremiumStatus(uid);
  const db = getFirestore();

  return db.runTransaction(async (tx) => {
    const todayIso = dateIsoMadrid();
    const reqRef = economyRequestRef(uid, requestId);
    const unlockKey = `${config.unlockKeyPrefix}:${periodKey}`;
    const unlockRef = economyHoroscopeUnlockRef(uid, unlockKey);
    const balanceRef = economyBalanceRef(uid);
    const usageRef = economyUsageDailyRef(todayIso, uid);

    const [reqSnap, unlockSnap, balanceSnap, usageSnap] = await Promise.all([
      tx.get(reqRef),
      tx.get(unlockRef),
      tx.get(balanceRef),
      tx.get(usageRef),
    ]);

    if (reqSnap.exists) {
      const existing = reqSnap.data() as EconomyRequestDoc;
      if (existing.response) return existing.response as TResponse;
      throw new HttpsError('internal', 'Stored economy request is missing response payload');
    }

    const currentBalance = asCount((balanceSnap.data() as EconomyBalanceDoc | undefined)?.balance);

    if (unlockSnap.exists) {
      const stableResponse: UnlockHoroscopePeriodResponse = {
        result: 'COMPLETED_SUCCESS',
        unlocked: true,
        alreadyUnlocked: true,
        balance: currentBalance,
        costCharged: 0,
      };
      const now = Timestamp.now();

      tx.set(reqRef, {
        requestId,
        type: config.requestType,
        result: stableResponse.result,
        response: stableResponse,
        responsePayload: {[config.keyField]: periodKey, sign, unlockKey},
        dateIso: todayIso,
        [config.keyField]: periodKey,
        createdAt: now,
        updatedAt: now,
      } as EconomyRequestDoc, {merge: true});

      return stableResponse as TResponse;
    }

    const isPremium = premium.isPremium;
    const ruleCost = getEconomyModuleRule(config.module).moonCostPerUse ?? 1;
    const shouldCharge = !isPremium;

    if (shouldCharge && currentBalance < ruleCost) {
      throw new HttpsError('failed-precondition', 'insufficient_moons');
    }

    const usageData = (usageSnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
    const now = Timestamp.now();
    const nextBalance = shouldCharge ? currentBalance - ruleCost : currentBalance;
    const costCharged = shouldCharge ? ruleCost : 0;
    const nextUsage = asCount(usageData[config.usageCounter]) + (shouldCharge ? 1 : 0);

    tx.set(unlockRef, {
      unlockKey,
      type: config.periodType,
      [config.keyField]: periodKey,
      createdAt: now,
      requestId,
      costCharged,
      premiumIncluded: isPremium,
      contextSign: sign,
    }, {merge: true});

    if (shouldCharge) {
      tx.set(balanceRef, {balance: nextBalance, updatedAt: now}, {merge: true});
      tx.set(usageRef, {[config.usageCounter]: nextUsage, updatedAt: now}, {merge: true});
      tx.set(economyLedgerRef(uid, `${todayIso}:horoscope-${config.unlockKeyPrefix}:${requestId}`), {
        type: config.ledgerType,
        amount: -ruleCost,
        requestId,
        dateIso: todayIso,
        [config.keyField]: periodKey,
        unlockKey,
        module: config.module,
        source: config.source,
        createdAt: now,
      } as EconomyLedgerEntryDoc, {merge: true});
    }

    logger.info(`${config.source} charged`, {
      uid,
      unlockKey,
      [config.keyField]: periodKey,
      costCharged,
    });

    const response: UnlockHoroscopePeriodResponse = {
      result: 'COMPLETED_SUCCESS',
      unlocked: true,
      alreadyUnlocked: false,
      balance: nextBalance,
      costCharged,
    };

    tx.set(reqRef, {
      requestId,
      type: config.requestType,
      result: response.result,
      response,
      responsePayload: {
        [config.keyField]: periodKey,
        sign,
        unlockKey,
        costCharged,
        premiumIncluded: isPremium,
      },
      dateIso: todayIso,
      [config.keyField]: periodKey,
      createdAt: now,
      updatedAt: now,
    } as EconomyRequestDoc, {merge: true});

    return response as TResponse;
  });
}
