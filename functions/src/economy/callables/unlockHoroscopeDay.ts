import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {logger} from 'firebase-functions';
import {DateTime} from 'luxon';
import {ENV} from '../../config/env';
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
  UnlockHoroscopeDayData,
  UnlockHoroscopeDayResponse,
} from '../types';

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

function normalizeDateIso(value: unknown): string {
  if (typeof value !== 'string') throw new HttpsError('invalid-argument', 'dateIso is required');
  const trimmed = value.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) throw new HttpsError('invalid-argument', 'dateIso format must be YYYY-MM-DD');
  return trimmed;
}

function asCount(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export const unlockHoroscopeDay = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<UnlockHoroscopeDayResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as UnlockHoroscopeDayData;
      const requestId = normalizeRequestId(data.requestId);
      const dateIso = normalizeDateIso(data.dateIso);
      const sign = normalizeSign(data.sign);

      const todayIso = dateIsoMadrid();
      if (dateIso <= todayIso) throw new HttpsError('failed-precondition', 'Only future days can be unlocked');

      const today = DateTime.fromISO(todayIso, {zone: 'Europe/Madrid'}).startOf('day');
      const target = DateTime.fromISO(dateIso, {zone: 'Europe/Madrid'}).startOf('day');
      const daysDiff = Math.floor(target.diff(today, 'days').days);
      if (daysDiff < 1 || daysDiff > 6) throw new HttpsError('failed-precondition', 'dateIso must be between today+1 and today+6');

      const premium = await getPremiumStatus(uid);
      const db = getFirestore();
      return db.runTransaction(async (tx) => {
        const reqRef = economyRequestRef(uid, requestId);
        const unlockKey = `daily:${dateIso}`;
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
          if (existing.response) return existing.response as UnlockHoroscopeDayResponse;
          throw new HttpsError('internal', 'Stored economy request is missing response payload');
        }

        const currentBalance = asCount((balanceSnap.data() as EconomyBalanceDoc | undefined)?.balance);

        if (unlockSnap.exists) {
          logger.info("unlockHoroscopeDay already unlocked", {uid, unlockKey, dateIso, costCharged: 0});
          const stableResponse: UnlockHoroscopeDayResponse = {
            result: 'COMPLETED_SUCCESS',
            unlocked: true,
            alreadyUnlocked: true,
            balance: currentBalance,
            costCharged: 0,
          };
          const now = Timestamp.now();
          tx.set(reqRef, {
            requestId,
            type: 'HOROSCOPE_UNLOCK_DAY',
            result: stableResponse.result,
            response: stableResponse,
            responsePayload: {dateIso, sign, unlockKey},
            dateIso: todayIso,
            createdAt: now,
            updatedAt: now,
          } as EconomyRequestDoc, {merge: true});
          return stableResponse;
        }

        const isPremium = premium.isPremium;
        const ruleCost = getEconomyModuleRule('HOROSCOPE_FUTURE_DAY').moonCostPerUse ?? 1;
        const shouldCharge = !isPremium;

        if (shouldCharge && currentBalance < ruleCost) {
          throw new HttpsError('failed-precondition', 'insufficient_moons');
        }

        const usageData = (usageSnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
        const now = Timestamp.now();
        const nextBalance = shouldCharge ? currentBalance - ruleCost : currentBalance;
        const nextUsage = asCount(usageData.horoscopeFutureDayMoonUsed) + (shouldCharge ? 1 : 0);
        const costCharged = shouldCharge ? ruleCost : 0;

        tx.set(unlockRef, {
          unlockKey,
          type: 'daily',
          dateIso,
          createdAt: now,
          requestId,
          costCharged,
          premiumIncluded: isPremium,
        }, {merge: true});

        if (shouldCharge) {
          tx.set(balanceRef, {balance: nextBalance, updatedAt: now}, {merge: true});
          tx.set(usageRef, {horoscopeFutureDayMoonUsed: nextUsage, updatedAt: now}, {merge: true});
          tx.set(economyLedgerRef(uid, `${todayIso}:horoscope-future-day:${requestId}`), {
            type: 'HOROSCOPE_FUTURE_DAY_MOON_SPEND',
            amount: -ruleCost,
            requestId,
            dateIso: todayIso,
            targetDateIso: dateIso,
            unlockKey,
            module: 'HOROSCOPE_FUTURE_DAY',
            source: 'unlockHoroscopeDay',
            createdAt: now,
          } as EconomyLedgerEntryDoc, {merge: true});
        }

        logger.info("unlockHoroscopeDay charged", {uid, unlockKey, dateIso, costCharged});

        const response: UnlockHoroscopeDayResponse = {
          result: 'COMPLETED_SUCCESS',
          unlocked: true,
          alreadyUnlocked: false,
          balance: nextBalance,
          costCharged,
        };

        tx.set(reqRef, {
          requestId,
          type: 'HOROSCOPE_UNLOCK_DAY',
          result: response.result,
          response,
          responsePayload: {dateIso, sign, unlockKey, costCharged, premiumIncluded: isPremium},
          dateIso: todayIso,
          createdAt: now,
          updatedAt: now,
        } as EconomyRequestDoc, {merge: true});

        return response;
      });
    }
);
