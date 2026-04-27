import {getFirestore} from 'firebase-admin/firestore';
import {DateTime} from 'luxon';
import {ENV, type Lang} from '../config/env';
import {
  horoscopeMonthlyLangDocPath,
  horoscopeMonthlySignDocPath,
  horoscopeWeeklyLangDocPath,
  horoscopeWeeklySignDocPath,
  type ZodiacSign,
} from '../firestore/paths';
import {createDocIfAbsent} from '../firestore/writeOnce';
import type {LLMRouter} from '../llm/LLMRouter';
import {
  monthlyCanonicalSystemPrompt,
  monthlyCanonicalUserPrompt,
  monthlyTranslationSystemPrompt,
  monthlyTranslationUserPrompt,
  weeklyCanonicalSystemPrompt,
  weeklyCanonicalUserPrompt,
  weeklyTranslationSystemPrompt,
  weeklyTranslationUserPrompt,
} from '../llm/prompts/horoscopePrompt';
import {logger} from '../utils/logger';

const WEEKLY_GENERATOR_VERSION = 1;
const MONTHLY_GENERATOR_VERSION = 1;

export type WeeklyHoroscopeDoc = {
  sign: ZodiacSign;
  weekKey: string;
  languageCode: Lang;
  title: string;
  overview: string;
  love: string;
  work: string;
  money: string;
  spiritualAdvice: string;
  keyDays: string[];
  mantra: string;
  shareText: string;
  createdAtEpochMillis: number;
  updatedAtEpochMillis: number;
  generatorVersion: number;
  llmProvider: string;
};

export type MonthlyHoroscopeDoc = {
  sign: ZodiacSign;
  monthKey: string;
  languageCode: Lang;
  title: string;
  overview: string;
  love: string;
  work: string;
  money: string;
  personalGrowth: string;
  ritualSuggestion: string;
  keyDates: string[];
  mantra: string;
  shareText: string;
  createdAtEpochMillis: number;
  updatedAtEpochMillis: number;
  generatorVersion: number;
  llmProvider: string;
};

type PreviousCompact = {
  periodKey: string;
  title: string;
  overview: string;
};

class StructureValidationError extends Error {}

function safeParseJson(text: string): unknown {
  const first = text.indexOf('{');
  const last = text.lastIndexOf('}');
  if (first === -1 || last === -1 || last <= first) {
    throw new StructureValidationError('No JSON object found');
  }
  const slice = text.slice(first, last + 1);
  return JSON.parse(slice) as unknown;
}

function asNonEmptyString(source: Record<string, unknown>, key: string) {
  const value = String(source[key] ?? '').trim();
  if (!value) throw new StructureValidationError(`Invalid: ${key} empty`);
  return value;
}

function asStringArray(source: Record<string, unknown>, key: string, min: number, max: number) {
  const raw = source[key];
  if (!Array.isArray(raw)) throw new StructureValidationError(`Invalid: ${key} must be array`);
  const values = raw.map((item) => String(item ?? '').trim()).filter(Boolean);
  if (values.length < min || values.length > max) {
    throw new StructureValidationError(`Invalid: ${key} length`);
  }
  return values;
}

function normalizeWeekly(doc: unknown): Omit<WeeklyHoroscopeDoc,
  'createdAtEpochMillis' | 'updatedAtEpochMillis' | 'generatorVersion' | 'llmProvider'> {
  const source = (doc ?? {}) as Record<string, unknown>;
  return {
    sign: asNonEmptyString(source, 'sign') as ZodiacSign,
    weekKey: asNonEmptyString(source, 'weekKey'),
    languageCode: asNonEmptyString(source, 'languageCode') as Lang,
    title: asNonEmptyString(source, 'title'),
    overview: asNonEmptyString(source, 'overview'),
    love: asNonEmptyString(source, 'love'),
    work: asNonEmptyString(source, 'work'),
    money: asNonEmptyString(source, 'money'),
    spiritualAdvice: asNonEmptyString(source, 'spiritualAdvice'),
    keyDays: asStringArray(source, 'keyDays', 2, 4),
    mantra: asNonEmptyString(source, 'mantra'),
    shareText: asNonEmptyString(source, 'shareText'),
  };
}

function normalizeMonthly(doc: unknown): Omit<MonthlyHoroscopeDoc,
  'createdAtEpochMillis' | 'updatedAtEpochMillis' | 'generatorVersion' | 'llmProvider'> {
  const source = (doc ?? {}) as Record<string, unknown>;
  return {
    sign: asNonEmptyString(source, 'sign') as ZodiacSign,
    monthKey: asNonEmptyString(source, 'monthKey'),
    languageCode: asNonEmptyString(source, 'languageCode') as Lang,
    title: asNonEmptyString(source, 'title'),
    overview: asNonEmptyString(source, 'overview'),
    love: asNonEmptyString(source, 'love'),
    work: asNonEmptyString(source, 'work'),
    money: asNonEmptyString(source, 'money'),
    personalGrowth: asNonEmptyString(source, 'personalGrowth'),
    ritualSuggestion: asNonEmptyString(source, 'ritualSuggestion'),
    keyDates: asStringArray(source, 'keyDates', 3, 5),
    mantra: asNonEmptyString(source, 'mantra'),
    shareText: asNonEmptyString(source, 'shareText'),
  };
}

function seasonContextForPeriod(periodDate: DateTime) {
  const month = periodDate.month;
  if (month >= 3 && month <= 5) return 'spring-renewal';
  if (month >= 6 && month <= 8) return 'summer-expansion';
  if (month >= 9 && month <= 11) return 'autumn-harvest';
  return 'winter-reflection';
}

function collectiveEnergyForWeek(weekKey: string) {
  const weekNumber = Number(weekKey.slice(-2));
  const slot = Number.isFinite(weekNumber) ? weekNumber % 4 : 0;
  if (slot === 0) return 'focus-and-closure';
  if (slot === 1) return 'initiation-and-momentum';
  if (slot === 2) return 'dialogue-and-adjustments';
  return 'grounding-and-priorities';
}

function collectiveEnergyForMonth(monthKey: string) {
  const month = Number(monthKey.slice(5, 7));
  const slot = Number.isFinite(month) ? month % 4 : 0;
  if (slot === 0) return 'integration-and-balance';
  if (slot === 1) return 'new-cycles-and-vision';
  if (slot === 2) return 'systems-and-consistency';
  return 'relationships-and-alignment';
}

async function runWithStructureRetry<T>(
    fn: () => Promise<T>,
    maxStructureRetries = 1
): Promise<T> {
  let attempt = 0;
  for (;;) {
    try {
      return await fn();
    } catch (error) {
      const isStructureError =
        error instanceof StructureValidationError ||
        error instanceof SyntaxError;
      if (!isStructureError || attempt >= maxStructureRetries) {
        throw error;
      }
      attempt++;
      logger.warn('period horoscope structure validation failed; retrying once', {
        attempt,
        error: String(error),
      });
    }
  }
}

export class PeriodHoroscopeGenerator {
  private readonly db = getFirestore();

  constructor(private readonly llm: LLMRouter) {}

  private async loadPreviousWeeklyCompact(weekKey: string, sign: ZodiacSign): Promise<PreviousCompact[]> {
    const periodDate = DateTime.fromFormat(`${weekKey}-1`, 'kkkk-\'W\'WW-c', {zone: 'Europe/Madrid'}).minus({weeks: 1});
    const previousKey = periodDate.toFormat('kkkk-\'W\'WW');
    const snap = await this.db.doc(horoscopeWeeklySignDocPath(previousKey, sign)).get();
    if (!snap.exists) return [];
    const data = (snap.data() ?? {}) as Record<string, unknown>;
    const title = String(data.title ?? '').trim();
    const overview = String(data.overview ?? '').trim();
    if (!title || !overview) return [];
    return [{periodKey: previousKey, title, overview}];
  }

  private async loadPreviousMonthlyCompact(monthKey: string, sign: ZodiacSign): Promise<PreviousCompact[]> {
    const periodDate = DateTime.fromFormat(`${monthKey}-01`, 'yyyy-MM-dd', {zone: 'Europe/Madrid'}).minus({months: 1});
    const previousKey = periodDate.toFormat('yyyy-MM');
    const snap = await this.db.doc(horoscopeMonthlySignDocPath(previousKey, sign)).get();
    if (!snap.exists) return [];
    const data = (snap.data() ?? {}) as Record<string, unknown>;
    const title = String(data.title ?? '').trim();
    const overview = String(data.overview ?? '').trim();
    if (!title || !overview) return [];
    return [{periodKey: previousKey, title, overview}];
  }

  async generateWeeklyCanonical(weekKey: string, sign: ZodiacSign) {
    const path = horoscopeWeeklySignDocPath(weekKey, sign);
    const snap = await this.db.doc(path).get();
    if (snap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const periodDate = DateTime.fromFormat(`${weekKey}-1`, 'kkkk-\'W\'WW-c', {zone: 'Europe/Madrid'});
    const seed = `${sign}|${weekKey}|weekly-v${WEEKLY_GENERATOR_VERSION}`;
    const seasonContext = seasonContextForPeriod(periodDate);
    const collectiveEnergy = collectiveEnergyForWeek(weekKey);
    const previous = await this.loadPreviousWeeklyCompact(weekKey, sign);

    const now = Date.now();
    const parsed = await runWithStructureRetry(async () => {
      const res = await this.llm.generate({
        scope: 'horoscope',
        messages: [
          {role: 'system', content: weeklyCanonicalSystemPrompt()},
          {
            role: 'user',
            content: weeklyCanonicalUserPrompt(
                weekKey,
                sign,
                seed,
                seasonContext,
                collectiveEnergy,
                previous
            ),
          },
        ],
        temperature: ENV.LLM_TEMPERATURE,
        maxTokens: Math.max(ENV.LLM_MAX_TOKENS, 900),
      });

      const normalized = normalizeWeekly(safeParseJson(res.text));
      if (normalized.sign !== sign) throw new StructureValidationError('Invalid: sign mismatch');
      if (normalized.weekKey !== weekKey) throw new StructureValidationError('Invalid: weekKey mismatch');
      normalized.languageCode = 'es';

      return {normalized, provider: res.provider};
    });

    const doc: WeeklyHoroscopeDoc = {
      ...parsed.normalized,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      generatorVersion: WEEKLY_GENERATOR_VERSION,
      llmProvider: parsed.provider,
    };

    const result = await createDocIfAbsent(path, doc);
    return {result, path, provider: parsed.provider};
  }

  async generateWeeklyTranslation(weekKey: string, sign: ZodiacSign, lang: Lang) {
    if (lang === 'es') {
      return {result: 'skipped', path: horoscopeWeeklyLangDocPath(weekKey, sign, lang), provider: 'none'};
    }

    const path = horoscopeWeeklyLangDocPath(weekKey, sign, lang);
    const translationSnap = await this.db.doc(path).get();
    if (translationSnap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const canonicalPath = horoscopeWeeklySignDocPath(weekKey, sign);
    const canonicalSnap = await this.db.doc(canonicalPath).get();
    if (!canonicalSnap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const canonical = normalizeWeekly(canonicalSnap.data());
    const canonicalPayloadJson = JSON.stringify(canonical);

    const now = Date.now();
    const parsed = await runWithStructureRetry(async () => {
      const res = await this.llm.generate({
        scope: 'horoscope',
        messages: [
          {role: 'system', content: weeklyTranslationSystemPrompt(lang)},
          {role: 'user', content: weeklyTranslationUserPrompt(lang, canonicalPayloadJson)},
        ],
        temperature: ENV.LLM_TEMPERATURE,
        maxTokens: Math.max(ENV.LLM_MAX_TOKENS, 900),
      });

      const normalized = normalizeWeekly(safeParseJson(res.text));
      if (normalized.sign !== sign) throw new StructureValidationError('Invalid: sign mismatch');
      if (normalized.weekKey !== weekKey) throw new StructureValidationError('Invalid: weekKey mismatch');
      normalized.languageCode = lang;

      return {normalized, provider: res.provider};
    });

    const doc: WeeklyHoroscopeDoc = {
      ...parsed.normalized,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      generatorVersion: WEEKLY_GENERATOR_VERSION,
      llmProvider: parsed.provider,
    };

    const result = await createDocIfAbsent(path, doc);
    return {result, path, provider: parsed.provider};
  }

  async generateMonthlyCanonical(monthKey: string, sign: ZodiacSign) {
    const path = horoscopeMonthlySignDocPath(monthKey, sign);
    const snap = await this.db.doc(path).get();
    if (snap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const periodDate = DateTime.fromFormat(`${monthKey}-01`, 'yyyy-MM-dd', {zone: 'Europe/Madrid'});
    const seed = `${sign}|${monthKey}|monthly-v${MONTHLY_GENERATOR_VERSION}`;
    const seasonContext = seasonContextForPeriod(periodDate);
    const collectiveEnergy = collectiveEnergyForMonth(monthKey);
    const previous = await this.loadPreviousMonthlyCompact(monthKey, sign);

    const now = Date.now();
    const parsed = await runWithStructureRetry(async () => {
      const res = await this.llm.generate({
        scope: 'horoscope',
        messages: [
          {role: 'system', content: monthlyCanonicalSystemPrompt()},
          {
            role: 'user',
            content: monthlyCanonicalUserPrompt(
                monthKey,
                sign,
                seed,
                seasonContext,
                collectiveEnergy,
                previous
            ),
          },
        ],
        temperature: ENV.LLM_TEMPERATURE,
        maxTokens: Math.max(ENV.LLM_MAX_TOKENS, 1100),
      });

      const normalized = normalizeMonthly(safeParseJson(res.text));
      if (normalized.sign !== sign) throw new StructureValidationError('Invalid: sign mismatch');
      if (normalized.monthKey !== monthKey) throw new StructureValidationError('Invalid: monthKey mismatch');
      normalized.languageCode = 'es';

      return {normalized, provider: res.provider};
    });

    const doc: MonthlyHoroscopeDoc = {
      ...parsed.normalized,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      generatorVersion: MONTHLY_GENERATOR_VERSION,
      llmProvider: parsed.provider,
    };

    const result = await createDocIfAbsent(path, doc);
    return {result, path, provider: parsed.provider};
  }

  async generateMonthlyTranslation(monthKey: string, sign: ZodiacSign, lang: Lang) {
    if (lang === 'es') {
      return {result: 'skipped', path: horoscopeMonthlyLangDocPath(monthKey, sign, lang), provider: 'none'};
    }

    const path = horoscopeMonthlyLangDocPath(monthKey, sign, lang);
    const translationSnap = await this.db.doc(path).get();
    if (translationSnap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const canonicalPath = horoscopeMonthlySignDocPath(monthKey, sign);
    const canonicalSnap = await this.db.doc(canonicalPath).get();
    if (!canonicalSnap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const canonical = normalizeMonthly(canonicalSnap.data());
    const canonicalPayloadJson = JSON.stringify(canonical);

    const now = Date.now();
    const parsed = await runWithStructureRetry(async () => {
      const res = await this.llm.generate({
        scope: 'horoscope',
        messages: [
          {role: 'system', content: monthlyTranslationSystemPrompt(lang)},
          {role: 'user', content: monthlyTranslationUserPrompt(lang, canonicalPayloadJson)},
        ],
        temperature: ENV.LLM_TEMPERATURE,
        maxTokens: Math.max(ENV.LLM_MAX_TOKENS, 1100),
      });

      const normalized = normalizeMonthly(safeParseJson(res.text));
      if (normalized.sign !== sign) throw new StructureValidationError('Invalid: sign mismatch');
      if (normalized.monthKey !== monthKey) throw new StructureValidationError('Invalid: monthKey mismatch');
      normalized.languageCode = lang;

      return {normalized, provider: res.provider};
    });

    const doc: MonthlyHoroscopeDoc = {
      ...parsed.normalized,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      generatorVersion: MONTHLY_GENERATOR_VERSION,
      llmProvider: parsed.provider,
    };

    const result = await createDocIfAbsent(path, doc);
    return {result, path, provider: parsed.provider};
  }
}
