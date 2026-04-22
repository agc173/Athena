import {Timestamp} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {economyBalanceRef, economyUsageDailyRef, dateIsoMadrid} from '../firestorePaths';
import {REWARDED_AD_DAILY_MAX} from '../rulesCatalog';
import type {EconomyBalanceDoc, EconomyDailyUsageDoc, GetEconomyBalanceResponse} from '../types';

export const getEconomyBalance = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
    },
    async (request): Promise<GetEconomyBalanceResponse> => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const dateIso = dateIsoMadrid();
      const [balanceSnap, dailyUsageSnap] = await Promise.all([
        economyBalanceRef(uid).get(),
        economyUsageDailyRef(dateIso, uid).get(),
      ]);

      const balanceData = balanceSnap.data() as EconomyBalanceDoc | undefined;
      const dailyData = dailyUsageSnap.data() as EconomyDailyUsageDoc | undefined;

      const rewardedAdsClaimed = Number(dailyData?.rewardedAdsClaimed ?? 0);
      const safeClaimed = Number.isFinite(rewardedAdsClaimed) ? Math.max(0, Math.floor(rewardedAdsClaimed)) : 0;

      return {
        balance: Number(balanceData?.balance ?? 0),
        updatedAt: balanceData?.updatedAt instanceof Timestamp ? balanceData.updatedAt : undefined,
        dailyLoginClaimed: dailyData?.dailyLoginClaimed === true,
        rewardedAdsClaimed: safeClaimed,
        rewardedAdsRemaining: Math.max(0, REWARDED_AD_DAILY_MAX - safeClaimed),
      };
    }
);
