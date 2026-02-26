import {onSchedule} from 'firebase-functions/v2/scheduler';
import {initializeApp} from 'firebase-admin/app';
import {DateTime} from 'luxon';
import {ENV, assertEnvForLLM} from './config/env';
import type {ZodiacSign} from './firestore/paths';
import {HoroscopeGenerator} from './generators/HoroscopeGenerator';
import {LLMRouter} from './llm/LLMRouter';
import {DeepSeekProvider} from './llm/providers/DeepSeekProvider';
import {GeminiVertexProvider} from './llm/providers/GeminiVertexProvider';
import {logger} from './utils/logger';
import {withRetry} from './utils/retry';

initializeApp();
assertEnvForLLM();

const ZODIAC_SIGNS: ZodiacSign[] = [
  'aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo',
  'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces',
];

function dateIsoMadrid(dayOffset: number): string {
  return DateTime.now().setZone('Europe/Madrid').plus({days: dayOffset}).toISODate()!;
}

export const generateHoroscopesWindow = onSchedule(
    {
      schedule: 'every day 02:10',
      timeZone: 'Europe/Madrid',
      region: 'europe-west1',
      retryCount: 3,
    },
    async () => {
      const router = LLMRouter.withFallback(
          new DeepSeekProvider(),
          new GeminiVertexProvider()
      );

      const generator = new HoroscopeGenerator(router);

      let created = 0;
      let skipped = 0;

      for (let dayOffset = 0; dayOffset <= 7; dayOffset++) {
        const dateIso = dateIsoMadrid(dayOffset);

        for (const sign of ZODIAC_SIGNS) {
          for (const lang of ENV.ACTIVE_LANGS) {
            const {result, path, provider} = await withRetry(
                () => generator.generateOne(dateIso, sign, lang),
                ENV.LLM_MAX_RETRIES
            );

            if (result === 'created') created++;
            else skipped++;

            logger.info('generateHoroscopesWindow item', {
              dateIso,
              sign,
              lang,
              path,
              result,
              provider,
            });
          }
        }
      }

      logger.info('generateHoroscopesWindow done', {
        provider: router.name,
        activeLangs: ENV.ACTIVE_LANGS,
        created,
        skipped,
      });
    }
);
