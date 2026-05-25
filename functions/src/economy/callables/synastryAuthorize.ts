import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {DateTime} from 'luxon';
import {completeSynastryEconomyRequest, reserveSynastryEconomyAccess} from '../synastryEconomy';
import {isSynastryEconomyV2Enabled} from '../runtimeConfig';
import {ENV} from '../../config/env';

type SynastryAuthorizeRequest = {
  requestId?: string;
  languageCode?: string;
};

export const synastryAuthorize = onCall({
  region: 'europe-west1',
  enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
}, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Authentication required');

  const economyEnabled = await isSynastryEconomyV2Enabled();
  if (!economyEnabled) {
    return {authorized: true, economyDisabled: true};
  }

  const data = (request.data ?? {}) as SynastryAuthorizeRequest;
  const requestId = data.requestId?.trim();
  if (!requestId) throw new HttpsError('invalid-argument', 'requestId required');

  const dateIso = DateTime.now().setZone('Europe/Madrid').toISODate();
  if (!dateIso) throw new HttpsError('internal', 'Unable to resolve dateIso');

  const reservation = await (async () => {
    try {
      return await reserveSynastryEconomyAccess({
        uid,
        requestId,
        dateIso,
        lang: data.languageCode,
      });
    } catch (error) {
      if (error instanceof HttpsError) {
        if (error.message === 'INSUFFICIENT_MOON_BALANCE') {
          throw new HttpsError(error.code, 'insufficient_moons');
        }
        if (error.message === 'SYNASTRY_DAILY_LIMIT_REACHED' || error.message === 'SYNASTRY_PREMIUM_DAILY_LIMIT_REACHED') {
          throw new HttpsError(error.code, 'daily_limit');
        }
      }
      throw error;
    }
  })();

  if (reservation.type === 'completed') return reservation.payload;
  if (reservation.type === 'in-progress') return {authorized: false, status: 'IN_PROGRESS'};

  const payload = {
    authorized: true,
    requestId,
    source: reservation.source,
    moonCost: reservation.moonCost,
    deckCardUnlockRewards: reservation.deckCardUnlockRewards,
  };

  await completeSynastryEconomyRequest({uid, requestId, responsePayload: payload});
  return payload;
});
