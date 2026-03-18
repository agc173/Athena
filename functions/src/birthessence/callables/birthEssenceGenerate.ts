import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {buildRouter} from '../../llm/buildRouter';
import {ENV} from '../../config/env';
import {addLlmTokens, reserveLlmCallOrThrow, type DailyCaps} from '../../firestore/usageDaily';
import {createLlmClientFromRouter} from '../../oracle/shared/routerLlmClient';

type BirthEssenceGenerateData = {
  sunSign?: unknown;
  moonSign?: unknown;
  risingSign?: unknown;
};

const ALLOWED_SIGNS = new Set([
  'ARIES', 'TAURUS', 'GEMINI', 'CANCER', 'LEO', 'VIRGO',
  'LIBRA', 'SCORPIO', 'SAGITTARIUS', 'CAPRICORN', 'AQUARIUS', 'PISCES',
]);

function asSign(value: unknown, field: string): string {
  if (typeof value !== 'string') throw new HttpsError('invalid-argument', `${field} is required`);
  const normalized = value.trim().toUpperCase();
  if (!ALLOWED_SIGNS.has(normalized)) {
    throw new HttpsError('invalid-argument', `${field} is invalid`);
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

function parseModelOutput(raw: string): {archetype: string | null; interpretation: string} {
  const text = raw.trim();
  const archetypeMatch = text.match(/ARQUETIPO:\s*(.+)/i);
  const interpretationMatch = text.match(/INTERPRETACI[ÓO]N:\s*([\s\S]+)/i);

  const archetype = archetypeMatch?.[1]?.trim() || null;
  const interpretation = interpretationMatch?.[1]?.trim() || text;

  if (!interpretation) {
    throw new HttpsError('internal', 'Empty interpretation from model');
  }

  return {
    archetype: archetype?.slice(0, 48) || null,
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

      const {dateIso} = await reserveLlmCallOrThrow('unknown', caps());

      const router = buildRouter();
      const llm = createLlmClientFromRouter(router, 'unknown', {
        usageDailyDateIso: dateIso,
        skipUsageReservation: true,
      });

      const response = await llm.generate({
        systemPrompt: [
          'Eres BWitch, guía mística moderna.',
          'Entrega salida breve y concreta en español.',
          'Nunca menciones cálculo astral real ni coordenadas.',
          'Formato obligatorio:',
          'ARQUETIPO: <nombre corto opcional>',
          'INTERPRETACIÓN: <2-3 frases, máximo 70 palabras>',
        ].join('\n'),
        userPrompt: `Sol=${signLabel(sunSign)}, Luna=${signLabel(moonSign)}, Ascendente=${signLabel(risingSign)}.`,
        temperature: 0.5,
        maxOutputTokens: 180,
      });

      await addLlmTokens('unknown', dateIso, response.inputTokens ?? 0, response.outputTokens ?? 0);

      return parseModelOutput(response.text);
    }
);
