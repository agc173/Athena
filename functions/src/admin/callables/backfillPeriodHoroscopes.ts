import {DateTime} from 'luxon';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import type {Lang} from '../../config/env';
import type {ZodiacSign} from '../../firestore/paths';
import {ALL_ZODIAC_SIGNS, normalizeZodiacSign} from '../../firestore/zodiacSigns';

type BackfillPeriodType = 'weekly' | 'monthly';
type BackfillInput = {
  periodType: BackfillPeriodType;
  startKey: string;
  endKey: string;
  signs?: ZodiacSign[];
  langs?: Lang[];
};

const ALL_SIGNS = ALL_ZODIAC_SIGNS;

const MAX_WEEKLY_PERIODS = 8;
const MAX_MONTHLY_PERIODS = 6;

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

function parseWeekKey(weekKey: string): DateTime {
  if (!/^\d{4}-W\d{2}$/.test(weekKey)) {
    throw new HttpsError('invalid-argument', `Invalid weekKey format: ${weekKey}`);
  }
  const dt = DateTime.fromFormat(`${weekKey}-1`, 'kkkk-\'W\'WW-c', {zone: 'Europe/Madrid'});
  if (!dt.isValid) {
    throw new HttpsError('invalid-argument', `Invalid weekKey value: ${weekKey}`);
  }
  return dt.startOf('week');
}

function parseMonthKey(monthKey: string): DateTime {
  if (!/^\d{4}-\d{2}$/.test(monthKey)) {
    throw new HttpsError('invalid-argument', `Invalid monthKey format: ${monthKey}`);
  }
  const dt = DateTime.fromFormat(`${monthKey}-01`, 'yyyy-MM-dd', {zone: 'Europe/Madrid'});
  if (!dt.isValid) {
    throw new HttpsError('invalid-argument', `Invalid monthKey value: ${monthKey}`);
  }
  return dt.startOf('month');
}

function buildPeriodKeys(input: BackfillInput): string[] {
  if (input.periodType === 'weekly') {
    const start = parseWeekKey(input.startKey);
    const end = parseWeekKey(input.endKey);
    if (end < start) throw new HttpsError('invalid-argument', 'endKey must be >= startKey');

    const out: string[] = [];
    for (let cursor = start; cursor <= end; cursor = cursor.plus({weeks: 1})) {
      out.push(cursor.toFormat('kkkk-\'W\'WW'));
      if (out.length > MAX_WEEKLY_PERIODS) {
        throw new HttpsError('failed-precondition', `Too many weekly periods (max ${MAX_WEEKLY_PERIODS})`);
      }
    }
    return out;
  }

  const start = parseMonthKey(input.startKey);
  const end = parseMonthKey(input.endKey);
  if (end < start) throw new HttpsError('invalid-argument', 'endKey must be >= startKey');

  const out: string[] = [];
  for (let cursor = start; cursor <= end; cursor = cursor.plus({months: 1})) {
    out.push(cursor.toFormat('yyyy-MM'));
    if (out.length > MAX_MONTHLY_PERIODS) {
      throw new HttpsError('failed-precondition', `Too many monthly periods (max ${MAX_MONTHLY_PERIODS})`);
    }
  }
  return out;
}

function sanitizeSigns(raw: unknown): ZodiacSign[] {
  if (!Array.isArray(raw) || raw.length === 0) return ALL_SIGNS;
  const selected = raw
      .map((value) => normalizeZodiacSign(value))
      .filter((value): value is ZodiacSign => Boolean(value));
  if (!selected.length) throw new HttpsError('invalid-argument', 'No valid zodiac signs provided');
  return [...new Set(selected)];
}

function normalizeLang(raw: unknown, activeLangs: Lang[]): Lang | undefined {
  const normalized = String(raw ?? '').trim().toLowerCase();
  return activeLangs.includes(normalized as Lang) ? normalized as Lang : undefined;
}

function sanitizeLangs(raw: unknown, activeLangs: Lang[]): Lang[] {
  if (!Array.isArray(raw) || raw.length === 0) {
    return activeLangs.filter((lang) => lang !== 'es');
  }
  const selected = raw
      .map((value) => normalizeLang(value, activeLangs))
      .filter((value): value is Lang => Boolean(value))
      .filter((lang) => lang !== 'es');
  return [...new Set(selected)];
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

      const input = (request.data ?? {}) as BackfillInput;
      if (input.periodType !== 'weekly' && input.periodType !== 'monthly') {
        throw new HttpsError('invalid-argument', 'periodType must be weekly or monthly');
      }

      const periodKeys = buildPeriodKeys(input);
      const signs = sanitizeSigns(input.signs);
      const langs = sanitizeLangs(input.langs, ENV.ACTIVE_LANGS);

      const router = buildRouter();
      const generator = new PeriodHoroscopeGenerator(router);

      const canonical = {created: 0, skipped: 0, repaired: 0, failed: 0};
      const translation = {created: 0, skipped: 0, repaired: 0, failed: 0, blockedByCanonicalFailure: 0};
      const failedCanonicalPairs = new Set<string>();

      for (const periodKey of periodKeys) {
        for (const sign of signs) {
          const pairKey = `${periodKey}|${sign}`;
          try {
            const result = await withRetry(
                () => input.periodType === 'weekly' ?
                  generator.generateWeeklyCanonical(periodKey, sign) :
                  generator.generateMonthlyCanonical(periodKey, sign),
                ENV.LLM_MAX_RETRIES
            );
            if (result.result === 'created') canonical.created++;
            else if (result.result === 'repaired') canonical.repaired++;
            else canonical.skipped++;
            logger.info('backfillPeriodHoroscopes canonical result', {
              periodType: input.periodType,
              periodKey,
              sign,
              lang: 'es',
              result: result.result,
              path: result.path,
              provider: result.provider,
            });
          } catch (error) {
            canonical.failed++;
            failedCanonicalPairs.add(pairKey);
            logger.error('backfillPeriodHoroscopes canonical failed', {
              periodType: input.periodType,
              periodKey,
              sign,
              error: String((error as Error)?.message ?? error),
            });
          }
        }
      }

      for (const periodKey of periodKeys) {
        for (const sign of signs) {
          const pairKey = `${periodKey}|${sign}`;
          for (const lang of langs) {
            if (failedCanonicalPairs.has(pairKey)) {
              translation.blockedByCanonicalFailure++;
              logger.warn('backfillPeriodHoroscopes translation blocked', {
                periodType: input.periodType,
                periodKey,
                sign,
                lang,
                reason: 'canonical_failed',
              });
              continue;
            }
            try {
              const result = await withRetry(
                  () => input.periodType === 'weekly' ?
                    generator.generateWeeklyTranslation(periodKey, sign, lang) :
                    generator.generateMonthlyTranslation(periodKey, sign, lang),
                  ENV.LLM_MAX_RETRIES
              );
              if (result.result === 'created') translation.created++;
              else if (result.result === 'repaired') translation.repaired++;
              else translation.skipped++;
              logger.info('backfillPeriodHoroscopes translation result', {
                periodType: input.periodType,
                periodKey,
                sign,
                lang,
                result: result.result,
                path: result.path,
                provider: result.provider,
              });
            } catch (error) {
              translation.failed++;
              logger.error('backfillPeriodHoroscopes translation failed', {
                periodType: input.periodType,
                periodKey,
                sign,
                lang,
                error: String((error as Error)?.message ?? error),
              });
            }
          }
        }
      }

      logger.info('backfillPeriodHoroscopes done', {
        periodType: input.periodType,
        periodKeys,
        signs,
        langs,
        canonical,
        translation,
      });

      return {
        ok: true,
        periodType: input.periodType,
        periodKeys,
        signsCount: signs.length,
        langsCount: langs.length,
        canonical,
        translation,
      };
    }
);
