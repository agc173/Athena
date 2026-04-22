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
import {REWARDED_AD_DAILY_MAX, REWARDED_AD_REWARD} from '../rulesCatalog';
import type {
  ClaimRewardedAdData,
  ClaimRewardedAdResponse,
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

function normalizeAdProof(value: unknown): string {
  if (typeof value !== 'string') {
    throw new HttpsError('invalid-argument', 'adProof is required');
  }

  const trimmed = value.trim();
  if (!trimmed) {
    throw new HttpsError('invalid-argument', 'adProof is required');
  }

  if (trimmed.length < 8) {
    throw new HttpsError('invalid-argument', 'adProof is too short');
  }

  if (trimmed.length > 2000) {
    throw new HttpsError('invalid-argument', 'adProof is too long');
  }

  return trimmed;
}

function normalizePlacement(value: unknown): string | undefined {
  if (value == null) return undefined;
  if (typeof value !== 'string') {
    throw new HttpsError('invalid-argument', 'placement must be a string');
  }

  const trimmed = value.trim();
  if (!trimmed) return undefined;

  if (trimmed.length > 100) {
    throw new HttpsError('invalid-argument', 'placement is too long');
  }

  return trimmed;
}

function asCount(value: unknown): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export const claimRewardedAd = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<ClaimRewardedAdResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const data = (request.data ?? {}) as ClaimRewardedAdData;
      const requestId = normalizeRequestId(data.requestId);
      const adProof = normalizeAdProof(data.adProof);
      const placement = normalizePlacement(data.placement);
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
          return existing.response as ClaimRewardedAdResponse;
        }

        const balanceData = (balanceSnap.data() as EconomyBalanceDoc | undefined) ?? {};
        const usageData = (usageSnap.data() as EconomyDailyUsageDoc | undefined) ?? {};

        const currentBalance = asCount(balanceData.balance);
        const rewardedAdsClaimed = asCount(usageData.rewardedAdsClaimed);
        const dailyLoginClaimed = usageData.dailyLoginClaimed === true;

        const now = Timestamp.now();

        if (rewardedAdsClaimed >= REWARDED_AD_DAILY_MAX) {
          const stableResponse: ClaimRewardedAdResponse = {
            result: 'DAILY_LIMIT_REACHED',
            balance: currentBalance,
            dailyLoginClaimed,
            rewardedAdsClaimed,
            rewardedAdsRemaining: 0,
          };

          tx.set(reqRef, {
            requestId,
            type: 'CLAIM_REWARDED_AD',
            result: stableResponse.result,
            response: stableResponse,
            responsePayload: {
              adProof,
              placement,
            },
            dateIso,
            createdAt: now,
            updatedAt: now,
          } as EconomyRequestDoc, {merge: true});

          return stableResponse;
        }

        const nextRewardedAdsClaimed = rewardedAdsClaimed + 1;
        const nextBalance = currentBalance + REWARDED_AD_REWARD;

        const successResponse: ClaimRewardedAdResponse = {
          result: 'CLAIMED',
          balance: nextBalance,
          dailyLoginClaimed,
          rewardedAdsClaimed: nextRewardedAdsClaimed,
          rewardedAdsRemaining: Math.max(0, REWARDED_AD_DAILY_MAX - nextRewardedAdsClaimed),
        };

        tx.set(balanceRef, {
          balance: nextBalance,
          updatedAt: now,
        }, {merge: true});

        tx.set(usageRef, {
          rewardedAdsClaimed: nextRewardedAdsClaimed,
          updatedAt: now,
        }, {merge: true});

        const ledgerEntryId = `${dateIso}:rewarded-ad:${requestId}`;
        tx.set(economyLedgerRef(uid, ledgerEntryId), {
          type: 'REWARDED_AD_CLAIM',
          amount: REWARDED_AD_REWARD,
          requestId,
          dateIso,
          placement,
          createdAt: now,
        } as EconomyLedgerEntryDoc, {merge: true});

        tx.set(reqRef, {
          requestId,
          type: 'CLAIM_REWARDED_AD',
          result: successResponse.result,
          response: successResponse,
          responsePayload: {
            adProof,
            placement,
          },
          dateIso,
          createdAt: now,
          updatedAt: now,
        } as EconomyRequestDoc, {merge: true});

        return successResponse;
      });

      return response;
    }
);
