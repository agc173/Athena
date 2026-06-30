import {HttpsError, onCall} from 'firebase-functions/v2/https';

type BasicNatalAuthorizeRequest = {requestId?: string; languageCode?: string};

function allowUnverifiedAppCheckInDev(): boolean {
  const raw = process.env.ALLOW_UNVERIFIED_APPCHECK_IN_DEV;
  if (raw == null || raw.trim() === '') return false;
  const normalized = raw.trim().toLowerCase();
  return normalized === '1' || normalized === 'true';
}

export const basicNatalAuthorize = onCall({
  region: 'europe-west1',
  enforceAppCheck: !allowUnverifiedAppCheckInDev(),
}, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Authentication required');

  const data = (request.data ?? {}) as BasicNatalAuthorizeRequest;
  const requestId = data.requestId?.trim();
  if (!requestId) throw new HttpsError('invalid-argument', 'requestId required');

  const [{DateTime}, {completeBasicNatalEconomyRequest, reserveBasicNatalEconomyAccess}] = await Promise.all([
    import('luxon'),
    import('../basicNatalEconomy.js'),
  ]);

  const dateIso = DateTime.now().setZone('Europe/Madrid').toISODate();
  if (!dateIso) throw new HttpsError('internal', 'Unable to resolve dateIso');

  const reservation = await (async () => {
    try {
      return await reserveBasicNatalEconomyAccess({uid, requestId, dateIso, lang: data.languageCode});
    } catch (error) {
      if (error instanceof HttpsError) {
        if (error.message === 'INSUFFICIENT_MOON_BALANCE') throw new HttpsError(error.code, 'insufficient_moons');
        if (error.message === 'BASIC_NATAL_DAILY_LIMIT_REACHED') throw new HttpsError(error.code, 'daily_limit');
      }
      throw error;
    }
  })();

  if (reservation.type === 'completed') return reservation.payload;
  if (reservation.type === 'in-progress') return {authorized: false, status: 'IN_PROGRESS'};

  const payload = {authorized: true, requestId, source: reservation.source, moonCost: reservation.moonCost};
  await completeBasicNatalEconomyRequest({uid, requestId, responsePayload: payload});
  return payload;
});
