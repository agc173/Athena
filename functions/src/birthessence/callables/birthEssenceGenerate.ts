import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {buildRouter} from '../../llm/buildRouter';
import {ENV} from '../../config/env';
import {addLlmTokens, reserveLlmCallOrThrow, type DailyCaps} from '../../firestore/usageDaily';
import {createLlmClientFromRouter} from '../../oracle/shared/routerLlmClient';
import type {Lang} from '../../config/env';

type BirthEssenceGenerateData = {
  sunSign?: unknown;
  moonSign?: unknown;
  risingSign?: unknown;
  languageCode?: unknown;
  lang?: unknown;
  archetype?: unknown;
};

const ALLOWED_SIGNS = new Set([
  'ARIES', 'TAURUS', 'GEMINI', 'CANCER', 'LEO', 'VIRGO',
  'LIBRA', 'SCORPIO', 'SAGITTARIUS', 'CAPRICORN', 'AQUARIUS', 'PISCES',
]);

const ALLOWED_ARCHETYPES = new Set([
  'MISTICA', 'GUERRERA', 'SANADORA', 'VIDENTE', 'ALQUIMISTA', 'GUARDIANA',
]);

function asSign(value: unknown, field: string): string {
  if (typeof value !== 'string') throw new HttpsError('invalid-argument', `${field} is required`);
  const normalized = value.trim().toUpperCase();
  if (!ALLOWED_SIGNS.has(normalized)) {
    throw new HttpsError('invalid-argument', `${field} is invalid`);
  }
  return normalized;
}

function asArchetypeHint(value: unknown): string | null {
  if (value == null) return null;
  if (typeof value !== 'string') throw new HttpsError('invalid-argument', 'archetype is invalid');
  const normalized = value.trim().toUpperCase();
  if (!ALLOWED_ARCHETYPES.has(normalized)) {
    throw new HttpsError('invalid-argument', 'archetype is invalid');
  }
  return normalized;
}

function caps(): DailyCaps {
  return {
    totalMaxCalls: ENV.DAILY_LLM_MAX_CALLS_TOTAL,
    scopeMaxCalls: {
      unknown: ENV.DAILY_LLM_MAX_CALLS_UNKNOWN,
    },
  };
}

function signLabel(sign: string): string {
  return sign.toLowerCase();
}

function parseLanguageCode(data: BirthEssenceGenerateData): Lang {
  const raw = typeof data.languageCode === 'string' ? data.languageCode : data.lang;
  if (typeof raw !== 'string') return 'es';
  const normalized = raw.trim().toLowerCase().split('-')[0].split('_')[0];
  if (ENV.SUPPORTED_LANGS.includes(normalized as Lang)) {
    return normalized as Lang;
  }
  return 'es';
}

function languageSystemPrompt(lang: Lang): string {
  switch (lang) {
    case 'en':
      return 'Write the final interpretation in English.';
    case 'pt':
      return 'Escreve a interpretação final em português.';
    case 'ru':
      return 'Напиши итоговую интерпретацию на русском языке.';
    case 'fr':
      return 'Écris l’interprétation finale en français.';
    case 'it':
      return 'Scrivi l’interpretazione finale in italiano.';
    case 'de':
      return 'Schreibe die finale Interpretation auf Deutsch.';
    case 'es':
    default:
      return 'Escribe la interpretación final en español.';
  }
}

function cleanInterpretation(raw: string): string {
  let text = raw.trim();

  if (text.startsWith('{') && text.endsWith('}')) {
    const parsed = runCatchingJsonParse(text);
    if (typeof parsed?.interpretation === 'string') {
      text = parsed.interpretation.trim();
    }
  }

  text = text
      .replace(/^\{+|\}+$/g, '')
      .replace(/^\s*ARQUETIPO\s*:\s*.*$/gim, '')
      .replace(/^\s*INTERPRETACI[ÓO]N\s*:\s*/i, '')
      .trim();

  return text;
}

function runCatchingJsonParse(value: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(value);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) return null;
    return parsed as Record<string, unknown>;
  } catch {
    return null;
  }
}

function parseModelOutput(raw: string): {archetype: null; interpretation: string} {
  const interpretation = cleanInterpretation(raw);

  if (!interpretation) {
    throw new HttpsError('internal', 'Empty interpretation from model');
  }

  return {
    archetype: null,
    interpretation: interpretation.slice(0, 420),
  };
}

export const birthEssenceGenerate = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
      secrets: ['DEEPSEEK_API_KEY'],
    },
    async (request) => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = (request.data ?? {}) as BirthEssenceGenerateData;
      const sunSign = asSign(data.sunSign, 'sunSign');
      const moonSign = asSign(data.moonSign, 'moonSign');
      const risingSign = asSign(data.risingSign, 'risingSign');
      const languageCode = parseLanguageCode(data);
      const archetypeHint = asArchetypeHint(data.archetype);

      const {dateIso} = await reserveLlmCallOrThrow('unknown', caps());

      const router = buildRouter();
      const llm = createLlmClientFromRouter(router, 'unknown', {
        usageDailyDateIso: dateIso,
        skipUsageReservation: true,
      });

      const response = await llm.generate({
        systemPrompt: [
          'Eres BWitch, una guía mística moderna.',
          'Escribe una interpretación breve y poderosa basada en Sol, Luna y Ascendente.',
          'Reglas:',
          '- 2 o 3 frases máximo',
          '- tono íntimo, místico y empoderador',
          '- habla en segunda persona (tú)',
          '- no expliques astrología',
          '- no uses etiquetas, encabezados ni formato',
          '- no menciones arquetipos',
          languageSystemPrompt(languageCode),
        ].join('\n'),
        userPrompt: [
          `Sol=${signLabel(sunSign)}, Luna=${signLabel(moonSign)}, Ascendente=${signLabel(risingSign)}.`,
          archetypeHint ? `Energía base: ${archetypeHint}.` : null,
        ].filter(Boolean).join('\n'),
        temperature: 0.5,
        maxOutputTokens: 180,
      });

      await addLlmTokens('unknown', dateIso, response.inputTokens ?? 0, response.outputTokens ?? 0);

      return {
        ...parseModelOutput(response.text),
        languageCode,
      };
    }
);
