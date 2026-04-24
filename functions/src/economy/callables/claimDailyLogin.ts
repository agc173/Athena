import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {
  dateIsoMadrid,
  economyBalanceRef,
  economyLedgerRef,
  economyRequestRef,
  economyUsageDailyRef,
} from '../firestorePaths';
import {DAILY_LOGIN_REWARD, REWARDED_AD_DAILY_MAX} from '../rulesCatalog';
import type {
  ClaimDailyLoginData,
  ClaimDailyLoginResponse,
  EconomyBalanceDoc,
  EconomyDailyUsageDoc,
  EconomyLedgerEntryDoc,
  EconomyRequestDoc,
} from '../types';

function normalizeRequestId(value: unknown): string {
  if (typeof value !== 'string') {
    throw new HttpsError('invalid-argument', 'requestId is required');
  }

  const trimmed = value.trim();
  if (!trimmed) {
    throw new HttpsError('invalid-argument', 'requestId is required');
  }

  if (trimmed.length > 100) {
    throw new HttpsError('invalid-argument', 'requestId is too long');
  }

  return trimmed;
}

function sanitizeClaimed(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export const claimDailyLogin = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<ClaimDailyLoginResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const data = (request.data ?? {}) as ClaimDailyLoginData;
      const requestId = normalizeRequestId(data.requestId);
      const dateIso = dateIsoMadrid();

      const db = getFirestore();
      const response = await db.runTransaction(async (tx) => {
        const balanceRef = economyBalanceRef(uid);
        const usageRef = economyUsageDailyRef(dateIso, uid);
        const reqRef = economyRequestRef(uid, requestId);

        const [requestSnap, balanceSnap, usageSnap] = await Promise.all([
          tx.get(reqRef),
          tx.get(balanceRef),
          tx.get(usageRef),
        ]);

        if (requestSnap.exists) {
          const existing = requestSnap.data() as EconomyRequestDoc;
          if (!existing.response) {
            throw new HttpsError('internal', 'Stored economy request is missing response payload');
          }
          return existing.response as ClaimDailyLoginResponse;
        }

        const balanceData = (balanceSnap.data() as EconomyBalanceDoc | undefined) ?? {};
        const usageData = (usageSnap.data() as EconomyDailyUsageDoc | undefined) ?? {};

        const rewardedAdsClaimed = sanitizeClaimed(usageData.rewardedAdsClaimed);

        if (usageData.dailyLoginClaimed === true) {
          const stableResponse: ClaimDailyLoginResponse = {
            result: 'ALREADY_CLAIMED',
            balance: Number(balanceData.balance ?? 0),
            dailyLoginClaimed: true,
            rewardedAdsClaimed,
            rewardedAdsRemaining: Math.max(0, REWARDED_AD_DAILY_MAX - rewardedAdsClaimed),
          };

          const now = Timestamp.now();
          tx.set(reqRef, {
            requestId,
            type: 'CLAIM_DAILY_LOGIN',
            result: stableResponse.result,
            response: stableResponse,
            createdAt: now,
            updatedAt: now,
          } as EconomyRequestDoc, {merge: true});

          return stableResponse;
        }

        const currentBalance = Number(balanceData.balance ?? 0);
        const nextBalance = currentBalance + DAILY_LOGIN_REWARD;
        const now = Timestamp.now();

        const successResponse: ClaimDailyLoginResponse = {
          result: 'CLAIMED',
          balance: nextBalance,
          dailyLoginClaimed: true,
          rewardedAdsClaimed,
          rewardedAdsRemaining: Math.max(0, REWARDED_AD_DAILY_MAX - rewardedAdsClaimed),
        };

        tx.set(balanceRef, {
          balance: nextBalance,
          updatedAt: now,
        }, {merge: true});

        tx.set(usageRef, {
          dailyLoginClaimed: true,
          dailyLoginClaimedAt: now,
          updatedAt: now,
        }, {merge: true});

        const ledgerEntryId = `${dateIso}:daily-login:${requestId}`;
        tx.set(economyLedgerRef(uid, ledgerEntryId), {
          type: 'DAILY_LOGIN_CLAIM',
          amount: DAILY_LOGIN_REWARD,
          requestId,
          dateIso,
          createdAt: now,
        } as EconomyLedgerEntryDoc, {merge: true});

        tx.set(reqRef, {
          requestId,
          type: 'CLAIM_DAILY_LOGIN',
          result: successResponse.result,
          response: successResponse,
          createdAt: now,
          updatedAt: now,
        } as EconomyRequestDoc, {merge: true});
        return successResponse;
      });

      return response;
    }
);
