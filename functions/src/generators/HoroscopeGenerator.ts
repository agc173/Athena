import {ENV, type Lang} from '../config/env';
import type {ZodiacSign} from '../firestore/paths';
import {createDocIfAbsent} from '../firestore/writeOnce';
import type {LLMRouter} from '../llm/LLMRouter';
import {horoscopeSystemPrompt, horoscopeUserPrompt} from '../llm/prompts/horoscopePrompt';
import {horoscopeSignDocPath} from '../firestore/paths';

export type HoroscopeDoc = {
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

function normalize(doc: unknown): Omit<HoroscopeDoc, 'createdAtEpochMillis' | 'updatedAtEpochMillis' | 'generatorVersion' | 'llmProvider'> {
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

export class HoroscopeGenerator {
  constructor(private readonly llm: LLMRouter) {}

  async generateOne(dateIso: string, sign: ZodiacSign, lang: Lang) {
    const now = Date.now();

    const res = await this.llm.generate({
      messages: [
        {role: 'system', content: horoscopeSystemPrompt(lang)},
        {role: 'user', content: horoscopeUserPrompt(dateIso, sign, lang)},
      ],
      temperature: ENV.LLM_TEMPERATURE,
      maxTokens: ENV.LLM_MAX_TOKENS,
    });

    const parsed = normalize(safeParseJson(res.text));

    const doc: HoroscopeDoc = {
      ...parsed,
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now,
      generatorVersion: ENV.GENERATOR_VERSION,
      llmProvider: res.provider,
    };

    const path = horoscopeSignDocPath(dateIso, sign);
    const result = await createDocIfAbsent(path, doc);
    return {result, path, provider: res.provider};
  }
}
