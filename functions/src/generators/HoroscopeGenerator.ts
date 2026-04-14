import {ENV, type Lang} from '../config/env';
import type {ZodiacSign} from '../firestore/paths';
import {createDocIfAbsent} from '../firestore/writeOnce';
import type {LLMRouter} from '../llm/LLMRouter';
import {
  horoscopeCanonicalSystemPrompt,
  horoscopeCanonicalUserPrompt,
  horoscopeSystemPrompt,
  horoscopeTranslationSystemPrompt,
  horoscopeTranslationUserPrompt,
  horoscopeUserPrompt,
} from '../llm/prompts/horoscopePrompt';
import {horoscopeLangDocPath, horoscopeSignDocPath} from '../firestore/paths';
import {getFirestore} from 'firebase-admin/firestore';

export type HoroscopeDoc = {
  languageCode: Lang;
  text: string;
  mood: string;
  luckyNumber: number;
  luckyColor: string;
  shareText: string;
  createdAtEpochMillis: number;
  updatedAtEpochMillis: number;
  generatorVersion: number;
  llmProvider: string;
};

function safeParseJson(text: string): unknown {
  const first = text.indexOf('{');
  const last = text.lastIndexOf('}');
  if (first === -1 || last === -1 || last <= first) throw new Error('No JSON object found');
  const slice = text.slice(first, last + 1);
  return JSON.parse(slice) as unknown;
}

function normalize(
    doc: unknown,
): Omit<HoroscopeDoc, 'languageCode' | 'createdAtEpochMillis' | 'updatedAtEpochMillis' | 'generatorVersion' | 'llmProvider'> {
  const source = (doc ?? {}) as Record<string, unknown>;
  const out = {
    text: String(source.text ?? '').trim(),
    mood: String(source.mood ?? '').trim(),
    luckyNumber: Number(source.luckyNumber),
    luckyColor: String(source.luckyColor ?? '').trim(),
    shareText: String(source.shareText ?? '').trim(),
  };

  if (!out.text) throw new Error('Invalid: text empty');
  if (!out.mood) throw new Error('Invalid: mood empty');
  if (!Number.isInteger(out.luckyNumber) || out.luckyNumber < 1 || out.luckyNumber > 99) {
    throw new Error('Invalid: luckyNumber');
  }
  if (!out.luckyColor) throw new Error('Invalid: luckyColor empty');
  if (!out.shareText) throw new Error('Invalid: shareText empty');

  return out;
}

function normalizeTranslation(
    doc: unknown,
): Pick<HoroscopeDoc, 'text' | 'shareText'> {
  const source = (doc ?? {}) as Record<string, unknown>;
  const out = {
    text: String(source.text ?? '').trim(),
    shareText: String(source.shareText ?? '').trim(),
  };

  if (!out.text) throw new Error('Invalid: translation text empty');
  if (!out.shareText) throw new Error('Invalid: translation shareText empty');

  return out;
}

export class HoroscopeGenerator {
  private readonly db = getFirestore();

  constructor(private readonly llm: LLMRouter) {}

  async generateOne(dateIso: string, sign: ZodiacSign, lang: Lang) {
    // ✅ 0) Resolve final doc path FIRST (so we can check existence before any LLM call)
    const path = ENV.HOROSCOPE_USE_LANGS ?
      horoscopeLangDocPath(dateIso, sign, lang) :
      horoscopeSignDocPath(dateIso, sign);

    // ✅ 1) COST GUARD: if doc exists -> skip without LLM
    const snap = await this.db.doc(path).get();
    if (snap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    // ✅ 2) Only now call the LLM
    const now = Date.now();

    const res = await this.llm.generate({
      scope: 'horoscope',
      messages: [
        {role: 'system', content: horoscopeSystemPrompt(lang)},
        {role: 'user', content: horoscopeUserPrompt(dateIso, sign, lang)},
      ],
      temperature: ENV.LLM_TEMPERATURE,
      maxTokens: ENV.LLM_MAX_TOKENS,
    });

    const parsed = normalize(safeParseJson(res.text));

    const doc: HoroscopeDoc = {
      languageCode: lang,
      ...parsed,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      generatorVersion: ENV.GENERATOR_VERSION,
      llmProvider: res.provider,
    };

    // ✅ 3) Write-once create (still safe if a race happens)
    const result = await createDocIfAbsent(path, doc);

    // If a race happened, createDocIfAbsent may report skipped; that's fine.
    return {result, path, provider: res.provider};
  }

  async generateCanonical(dateIso: string, sign: ZodiacSign) {
    const path = horoscopeSignDocPath(dateIso, sign);
    const snap = await this.db.doc(path).get();
    if (snap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const now = Date.now();
    const res = await this.llm.generate({
      scope: 'horoscope',
      messages: [
        {role: 'system', content: horoscopeCanonicalSystemPrompt()},
        {role: 'user', content: horoscopeCanonicalUserPrompt(dateIso, sign)},
      ],
      temperature: ENV.LLM_TEMPERATURE,
      maxTokens: ENV.LLM_MAX_TOKENS,
    });

    const parsed = normalize(safeParseJson(res.text));
    const doc: HoroscopeDoc = {
      languageCode: 'es',
      ...parsed,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      generatorVersion: ENV.GENERATOR_VERSION,
      llmProvider: res.provider,
    };

    const result = await createDocIfAbsent(path, doc);
    return {result, path, provider: res.provider};
  }

  async generateTranslation(dateIso: string, sign: ZodiacSign, lang: Lang) {
    if (lang === 'es') {
      return {result: 'skipped', path: horoscopeLangDocPath(dateIso, sign, lang), provider: 'none'};
    }

    const path = horoscopeLangDocPath(dateIso, sign, lang);
    const translationSnap = await this.db.doc(path).get();
    if (translationSnap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const canonicalPath = horoscopeSignDocPath(dateIso, sign);
    const canonicalSnap = await this.db.doc(canonicalPath).get();
    if (!canonicalSnap.exists) {
      return {result: 'skipped', path, provider: 'none'};
    }

    const canonical = canonicalSnap.data() as Partial<HoroscopeDoc> | undefined;
    const canonicalText = String(canonical?.text ?? '').trim();
    const canonicalShareText = String(canonical?.shareText ?? '').trim();
    const canonicalMood = String(canonical?.mood ?? '').trim();
    const canonicalLuckyNumber = Number(canonical?.luckyNumber);
    const canonicalLuckyColor = String(canonical?.luckyColor ?? '').trim();

    if (!canonicalText || !canonicalShareText || !canonicalMood || !canonicalLuckyColor ||
      !Number.isInteger(canonicalLuckyNumber)) {
      throw new Error(`Invalid canonical horoscope payload at ${canonicalPath}`);
    }

    const now = Date.now();
    const res = await this.llm.generate({
      scope: 'horoscope',
      messages: [
        {role: 'system', content: horoscopeTranslationSystemPrompt(lang)},
        {
          role: 'user',
          content: horoscopeTranslationUserPrompt(
              dateIso,
              sign,
              lang,
              canonicalText,
              canonicalShareText
          ),
        },
      ],
      temperature: ENV.LLM_TEMPERATURE,
      maxTokens: ENV.LLM_MAX_TOKENS,
    });

    const parsed = normalizeTranslation(safeParseJson(res.text));
    const doc: HoroscopeDoc = {
      languageCode: lang,
      text: parsed.text,
      shareText: parsed.shareText,
      mood: canonicalMood,
      luckyNumber: canonicalLuckyNumber,
      luckyColor: canonicalLuckyColor,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      generatorVersion: ENV.GENERATOR_VERSION,
      llmProvider: res.provider,
    };

    const result = await createDocIfAbsent(path, doc);
    return {result, path, provider: res.provider};
  }
}
