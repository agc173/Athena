import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {logger} from 'firebase-functions';
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
  UnlockHoroscopeMonthlyData,
  UnlockHoroscopeMonthlyResponse,
} from '../types';
import {assertAllowedMonthKey, normalizeMonthKey} from './horoscopePeriodKeys';

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

export const unlockHoroscopeMonthly = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<UnlockHoroscopeMonthlyResponse> => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as UnlockHoroscopeMonthlyData;
      const requestId = normalizeRequestId(data.requestId);
      const monthKey = normalizeMonthKey(data.monthKey);
      const sign = normalizeSign(data.sign);

      assertAllowedMonthKey(monthKey);

      const premium = await getPremiumStatus(uid);
      const db = getFirestore();
      return db.runTransaction(async (tx) => {
        const todayIso = dateIsoMadrid();
        const reqRef = economyRequestRef(uid, requestId);
        const unlockKey = `monthly:${monthKey}`;
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
          if (existing.response) return existing.response as UnlockHoroscopeMonthlyResponse;
          throw new HttpsError('internal', 'Stored economy request is missing response payload');
        }

        const currentBalance = asCount((balanceSnap.data() as EconomyBalanceDoc | undefined)?.balance);

        if (unlockSnap.exists) {
          const stableResponse: UnlockHoroscopeMonthlyResponse = {
            result: 'COMPLETED_SUCCESS',
            unlocked: true,
            alreadyUnlocked: true,
            balance: currentBalance,
            costCharged: 0,
          };
          const now = Timestamp.now();
          tx.set(reqRef, {
            requestId,
            type: 'HOROSCOPE_UNLOCK_MONTHLY',
            result: stableResponse.result,
            response: stableResponse,
            responsePayload: {monthKey, sign, unlockKey},
            dateIso: todayIso,
            monthKey,
            createdAt: now,
            updatedAt: now,
          } as EconomyRequestDoc, {merge: true});
          return stableResponse;
        }

        const isPremium = premium.isPremium;
        const ruleCost = getEconomyModuleRule('HOROSCOPE_MONTHLY').moonCostPerUse ?? 1;
        const shouldCharge = !isPremium;

        if (shouldCharge && currentBalance < ruleCost) {
          throw new HttpsError('failed-precondition', 'insufficient_moons');
        }

        const usageData = (usageSnap.data() as EconomyDailyUsageDoc | undefined) ?? {};
        const now = Timestamp.now();
        const nextBalance = shouldCharge ? currentBalance - ruleCost : currentBalance;
        const costCharged = shouldCharge ? ruleCost : 0;
        const nextUsage = asCount(usageData.horoscopeMonthlyMoonUsed) + (shouldCharge ? 1 : 0);

        tx.set(unlockRef, {
          unlockKey,
          type: 'monthly',
          monthKey,
          createdAt: now,
          requestId,
          costCharged,
          premiumIncluded: isPremium,
          contextSign: sign,
        }, {merge: true});

        if (shouldCharge) {
          tx.set(balanceRef, {balance: nextBalance, updatedAt: now}, {merge: true});
          tx.set(usageRef, {horoscopeMonthlyMoonUsed: nextUsage, updatedAt: now}, {merge: true});
          tx.set(economyLedgerRef(uid, `${todayIso}:horoscope-monthly:${requestId}`), {
            type: 'HOROSCOPE_MONTHLY_MOON_SPEND',
            amount: -ruleCost,
            requestId,
            dateIso: todayIso,
            monthKey,
            unlockKey,
            module: 'HOROSCOPE_MONTHLY',
            source: 'unlockHoroscopeMonthly',
            createdAt: now,
          } as EconomyLedgerEntryDoc, {merge: true});
        }

        logger.info('unlockHoroscopeMonthly charged', {uid, unlockKey, monthKey, costCharged});

        const response: UnlockHoroscopeMonthlyResponse = {
          result: 'COMPLETED_SUCCESS',
          unlocked: true,
          alreadyUnlocked: false,
          balance: nextBalance,
          costCharged,
        };

        tx.set(reqRef, {
          requestId,
          type: 'HOROSCOPE_UNLOCK_MONTHLY',
          result: response.result,
          response,
          responsePayload: {monthKey, sign, unlockKey, costCharged, premiumIncluded: isPremium},
          dateIso: todayIso,
          monthKey,
          createdAt: now,
          updatedAt: now,
        } as EconomyRequestDoc, {merge: true});

        return response;
      });
    }
);
