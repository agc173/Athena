import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {runBackfillPeriodHoroscopes} from '../backfillPeriodHoroscopesCore';

function allowUnverifiedAppCheckInDev(): boolean {
  const raw = process.env.ALLOW_UNVERIFIED_APPCHECK_IN_DEV;
  if (raw == null || raw.trim() === '') return false;
  const normalized = raw.trim().toLowerCase();
  return normalized === '1' || normalized === 'true';
}

function assertAdminUid(uid: string | undefined, backfillAdminUids: string[]) {
  if (!uid) {
    throw new HttpsError('unauthenticated', 'Authentication is required');
  }
  if (!backfillAdminUids.includes(uid)) {
    throw new HttpsError('permission-denied', 'Admin privileges required');
  }
}

export const backfillPeriodHoroscopes = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !allowUnverifiedAppCheckInDev(),
      timeoutSeconds: 600,
      secrets: ['DEEPSEEK_API_KEY'],
    },
    async (request) => {
      const [
        {ENV},
        {buildRouter},
        {PeriodHoroscopeGenerator},
        {withRetry},
        {logger},
      ] = await Promise.all([
        import('../../config/env.js'),
        import('../../llm/buildRouter.js'),
        import('../../generators/PeriodHoroscopeGenerator.js'),
        import('../../utils/retry.js'),
        import('../../utils/logger.js'),
      ]);

      assertAdminUid(request.auth?.uid, ENV.BACKFILL_ADMIN_UIDS);

      return runBackfillPeriodHoroscopes(request.data ?? {}, {
        activeLangs: ENV.ACTIVE_LANGS,
        maxRetries: ENV.LLM_MAX_RETRIES,
        buildRouter,
        PeriodHoroscopeGenerator,
        withRetry,
        logger,
      });
    }
);
