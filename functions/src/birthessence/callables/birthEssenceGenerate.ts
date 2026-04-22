import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {buildRouter} from '../../llm/buildRouter';
import {ENV} from '../../config/env';
import {addLlmTokens, reserveLlmCallOrThrow, type DailyCaps} from '../../firestore/usageDaily';
import {createLlmClientFromRouter} from '../../oracle/shared/routerLlmClient';
import {
  completeBirthEssenceEconomyRequest,
  dateIsoMadrid,
  isBirthEssenceEconomyV2Enabled,
  refundBirthEssenceEconomyRequest,
  reserveBirthEssenceEconomyAccess,
} from '../../economy';
import type {Lang} from '../../config/env';

type BirthEssenceGenerateData = {
  sunSign?: unknown;
  moonSign?: unknown;
  risingSign?: unknown;
  languageCode?: unknown;
  lang?: unknown;
  archetype?: unknown;
  requestId?: unknown;
};

const SUPPORTED_OUTPUT_LANGS: readonly Lang[] = ['es', 'en', 'pt', 'ru', 'fr', 'it', 'de'];

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
  if (
    SUPPORTED_OUTPUT_LANGS.includes(normalized as Lang) &&
    ENV.SUPPORTED_LANGS.includes(normalized as Lang)
  ) {
    return normalized as Lang;
  }
  return 'es';
}

function languageMeta(lang: Lang): {name: string; toneHint: string; secondPerson: string} {
  switch (lang) {
    case 'en':
      return {
        name: 'English',
        toneHint: 'Use natural contemporary English, never Spanish.',
        secondPerson: 'you',
      };
    case 'pt':
      return {
        name: 'Portuguese',
        toneHint: 'Use natural Brazilian/neutral Portuguese, never Spanish.',
        secondPerson: 'você',
      };
    case 'ru':
      return {
        name: 'Russian',
        toneHint: 'Пиши естественным русским языком, без испанских фраз.',
        secondPerson: 'ты',
      };
    case 'fr':
      return {
        name: 'French',
        toneHint: 'Utilise un français naturel, sans espagnol.',
        secondPerson: 'tu',
      };
    case 'it':
      return {
        name: 'Italian',
        toneHint: 'Usa italiano naturale, senza frasi in spagnolo.',
        secondPerson: 'tu',
      };
    case 'de':
      return {
        name: 'German',
        toneHint: 'Nutze natürliches Deutsch, ohne Spanisch.',
        secondPerson: 'du',
      };
    case 'es':
    default:
      return {
        name: 'Spanish',
        toneHint: 'Usa español natural y cercano.',
        secondPerson: 'tú',
      };
  }
}

function looksSpanish(text: string): boolean {
  const normalized = ` ${text.toLowerCase()} `;
  const markers = [' tú ', ' tu energía ', ' tu intuición ', ' luna ', ' ascendente ', ' eres '];
  return markers.filter((marker) => normalized.includes(marker)).length >= 2;
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
      const requestLanguageCode = typeof data.languageCode === 'string' ? data.languageCode : null;
      const requestLang = typeof data.lang === 'string' ? data.lang : null;
      const sunSign = asSign(data.sunSign, 'sunSign');
      const moonSign = asSign(data.moonSign, 'moonSign');
      const risingSign = asSign(data.risingSign, 'risingSign');
      const languageCode = parseLanguageCode(data);
      const language = languageMeta(languageCode);
      const archetypeHint = asArchetypeHint(data.archetype);
      const economyV2Enabled = await isBirthEssenceEconomyV2Enabled();
      const dateIso = dateIsoMadrid();

      const requestId = economyV2Enabled ?
        (typeof data.requestId === 'string' ? data.requestId.trim() : '') :
        '';

      if (economyV2Enabled && !requestId) {
        throw new HttpsError('invalid-argument', 'requestId is required');
      }

      console.info('BIRTH_ESSENCE_LANGUAGE_REQUEST', {
        uid,
        requestedLanguageCode: requestLanguageCode,
        requestedLang: requestLang,
        normalizedLanguageCode: languageCode,
      });

      const reservation = economyV2Enabled ?
        await reserveBirthEssenceEconomyAccess({
          uid,
          requestId,
          dateIso,
          lang: languageCode,
        }) :
        {type: 'reserved' as const};

      const economyDecisionSource = economyV2Enabled &&
        reservation.type === 'reserved' &&
        'source' in reservation ?
        reservation.source :
        undefined;
      const economyMoonCost = economyV2Enabled &&
        reservation.type === 'reserved' &&
        'moonCost' in reservation ?
        reservation.moonCost :
        undefined;

      if (reservation.type === 'completed') {
        return reservation.payload;
      }

      if (reservation.type === 'in-progress') {
        return {requestId, status: 'IN_PROGRESS'};
      }

      const router = buildRouter();

      let usageDateIso: string;
      try {
        const {dateIso: reservedDateIso} = await reserveLlmCallOrThrow('unknown', caps());
        usageDateIso = reservedDateIso;
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : String(error);

        if (economyV2Enabled && requestId && errorMessage.includes('DAILY_LLM_CAP_EXCEEDED')) {
          await refundBirthEssenceEconomyRequest({
            uid,
            requestId,
            dateIso,
            errorMessage: 'DAILY_LLM_CAP_EXCEEDED',
          });
        }

        throw error;
      }

      const llm = createLlmClientFromRouter(router, 'unknown', {
        usageDailyDateIso: usageDateIso,
        skipUsageReservation: true,
      });

      try {
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
            `Return ONLY the final interpretation in ${language.name}.`,
            `Language hard rule: the whole output must be in ${language.name}.`,
            language.toneHint,
          ].join('\n'),
          userPrompt: [
            `Output language required: ${language.name}.`,
            `Return ONLY the final interpretation in ${language.name}.`,
            `Write in second person (${language.secondPerson}) and keep BWitch mystical empowering tone.`,
            `Sol=${signLabel(sunSign)}, Luna=${signLabel(moonSign)}, Ascendente=${signLabel(risingSign)}.`,
            archetypeHint ? `Energía base: ${archetypeHint}.` : null,
          ].filter(Boolean).join('\n'),
          temperature: 0.5,
          maxOutputTokens: 180,
        });

        await addLlmTokens('unknown', usageDateIso, response.inputTokens ?? 0, response.outputTokens ?? 0);

        const parsed = parseModelOutput(response.text);
        const possibleSpanishMismatch = languageCode !== 'es' && looksSpanish(parsed.interpretation);

        console.info('BIRTH_ESSENCE_LANGUAGE_RESULT', {
          uid,
          languageCode,
          possibleSpanishMismatch,
        });

        // Manual validation checklist:
        // 1) languageCode=en with ARIES/CANCER/LEO should return fully English interpretation.
        // 2) languageCode=pt-BR should normalize to pt and return Portuguese interpretation.
        // 3) languageCode=fr with archetype hint should keep French output and BWitch tone.
        // 4) languageCode=xx should fallback to es.

        const responsePayload = {
          ...parsed,
          languageCode,
          requestId: economyV2Enabled ? requestId : undefined,
          economy: economyV2Enabled ? {
            source: economyDecisionSource,
            moonCost: economyMoonCost,
          } : undefined,
        };

        if (economyV2Enabled && requestId) {
          await completeBirthEssenceEconomyRequest({
            uid,
            requestId,
            responsePayload,
            llmMeta: {
              provider: router.name,
              inputTokens: response.inputTokens,
              outputTokens: response.outputTokens,
            },
          });
        }

        return responsePayload;
      } catch (error) {
        if (economyV2Enabled && requestId) {
          await refundBirthEssenceEconomyRequest({
            uid,
            requestId,
            dateIso,
            errorMessage: error instanceof Error ? error.message : String(error),
          });
        }
        throw error;
      }
    }
);
