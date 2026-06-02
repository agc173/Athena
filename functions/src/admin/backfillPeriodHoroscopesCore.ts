import {DateTime} from 'luxon';
import {HttpsError} from 'firebase-functions/v2/https';
import type {Lang} from '../config/env';
import type {ZodiacSign} from '../firestore/paths';
import {ALL_ZODIAC_SIGNS, normalizeZodiacSign} from '../firestore/zodiacSigns';
import type {LLMRouter} from '../llm/LLMRouter';

export type BackfillPeriodType = 'weekly' | 'monthly';
export type BackfillInput = {
  periodType: BackfillPeriodType;
  startKey: string;
  endKey: string;
  signs?: unknown;
  langs?: unknown;
};

type PeriodHoroscopeGeneratorCtor = new (llm: LLMRouter) => {
  generateWeeklyCanonical(periodKey: string, sign: ZodiacSign): Promise<BackfillGenerateResult>;
  generateMonthlyCanonical(periodKey: string, sign: ZodiacSign): Promise<BackfillGenerateResult>;
  generateWeeklyTranslation(periodKey: string, sign: ZodiacSign, lang: Lang): Promise<BackfillGenerateResult>;
  generateMonthlyTranslation(periodKey: string, sign: ZodiacSign, lang: Lang): Promise<BackfillGenerateResult>;
};

type BackfillGenerateResult = {
  result: string;
  path: string;
  provider: string;
};

type BackfillLogger = {
  info: (...args: unknown[]) => void;
  warn: (...args: unknown[]) => void;
  error: (...args: unknown[]) => void;
};

type BackfillDeps = {
  activeLangs: Lang[];
  maxRetries: number;
  buildRouter: () => LLMRouter;
  PeriodHoroscopeGenerator: PeriodHoroscopeGeneratorCtor;
  withRetry: <T>(fn: () => Promise<T>, maxRetries: number) => Promise<T>;
  logger: BackfillLogger;
};

const ALL_SIGNS = ALL_ZODIAC_SIGNS;

export const MAX_WEEKLY_PERIODS = 8;
export const MAX_MONTHLY_PERIODS = 6;

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

export function buildPeriodKeys(input: Pick<BackfillInput, 'periodType' | 'startKey' | 'endKey'>): string[] {
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

export function sanitizeSigns(raw: unknown): ZodiacSign[] {
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

export function sanitizeLangs(raw: unknown, activeLangs: Lang[]): Lang[] {
  if (!Array.isArray(raw) || raw.length === 0) {
    return activeLangs.filter((lang) => lang !== 'es');
  }
  const selected = raw
      .map((value) => normalizeLang(value, activeLangs))
      .filter((value): value is Lang => Boolean(value))
      .filter((lang) => lang !== 'es');
  return [...new Set(selected)];
}

function assertBackfillInput(input: BackfillInput) {
  if (input.periodType !== 'weekly' && input.periodType !== 'monthly') {
    throw new HttpsError('invalid-argument', 'periodType must be weekly or monthly');
  }
}

function countResult(bucket: {created: number; skipped: number; repaired: number; failed: number}, result: string) {
  if (result === 'created') bucket.created++;
  else if (result === 'repaired') bucket.repaired++;
  else bucket.skipped++;
}

export async function runBackfillPeriodHoroscopes(input: BackfillInput, deps: BackfillDeps) {
  assertBackfillInput(input);

  const periodKeys = buildPeriodKeys(input);
  const signs = sanitizeSigns(input.signs);
  const langs = sanitizeLangs(input.langs, deps.activeLangs);

  const router = deps.buildRouter();
  const generator = new deps.PeriodHoroscopeGenerator(router);

  const canonical = {created: 0, skipped: 0, repaired: 0, failed: 0};
  const translation = {created: 0, skipped: 0, repaired: 0, failed: 0, blockedByCanonicalFailure: 0};
  const failedCanonicalPairs = new Set<string>();

  for (const periodKey of periodKeys) {
    for (const sign of signs) {
      const pairKey = `${periodKey}|${sign}`;
      try {
        const result = await deps.withRetry(
            () => input.periodType === 'weekly' ?
              generator.generateWeeklyCanonical(periodKey, sign) :
              generator.generateMonthlyCanonical(periodKey, sign),
            deps.maxRetries
        );
        countResult(canonical, result.result);
        deps.logger.info('backfillPeriodHoroscopes canonical result', {
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
        deps.logger.error('backfillPeriodHoroscopes canonical failed', {
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
          deps.logger.warn('backfillPeriodHoroscopes translation blocked', {
            periodType: input.periodType,
            periodKey,
            sign,
            lang,
            reason: 'canonical_failed',
          });
          continue;
        }
        try {
          const result = await deps.withRetry(
              () => input.periodType === 'weekly' ?
                generator.generateWeeklyTranslation(periodKey, sign, lang) :
                generator.generateMonthlyTranslation(periodKey, sign, lang),
              deps.maxRetries
          );
          countResult(translation, result.result);
          deps.logger.info('backfillPeriodHoroscopes translation result', {
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
          deps.logger.error('backfillPeriodHoroscopes translation failed', {
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

  deps.logger.info('backfillPeriodHoroscopes done', {
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
