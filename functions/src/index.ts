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
import {MockLLMProvider} from './llm/providers/MockLLMProvider';

initializeApp();
assertEnvForLLM();

const ZODIAC_SIGNS: ZodiacSign[] = [
  'aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo',
  'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces',
];

function dateIsoMadrid(dayOffset: number): string {
  return DateTime.now()
      .setZone('Europe/Madrid')
      .plus({days: dayOffset + ENV.DATE_OFFSET_DAYS})
      .toISODate()!;
}

async function runWithConcurrency<T>(
    tasks: Array<() => Promise<T>>,
    concurrency: number
): Promise<T[]> {
  const results: T[] = [];
  let i = 0;

  async function worker() {
    for (;;) {
      const currentIndex = i++;
      if (currentIndex >= tasks.length) break;

      results[currentIndex] = await tasks[currentIndex]();
    }
  }

  const workers = Array.from(
      {length: Math.max(1, concurrency)},
      () => worker()
  );

  await Promise.all(workers);
  return results;
}

/**
 * Build a router that only enables fallback if GeminiVertexProvider is actually implemented.
 * This avoids the current situation where the fallback always throws "not implemented yet"
 * and turns recoverable DeepSeek failures into guaranteed failures.
 */
function buildRouter(): LLMRouter {
  const gemini = new GeminiVertexProvider();

  const geminiImplemented =
    typeof (gemini as any).isImplemented === 'function' ?
      Boolean((gemini as any).isImplemented()) :
      false;

  const useMock = ENV.USE_MOCK_LLM;

  const primaryProvider = useMock ?
    new MockLLMProvider() :
    (ENV.DEEPSEEK_API_KEY ?
        new DeepSeekProvider() :
        new MockLLMProvider());


  return geminiImplemented ?
    LLMRouter.withFallback(primaryProvider, gemini) :
    new LLMRouter(primaryProvider);
}

export const generateHoroscopesWindow = onSchedule(
    {
      schedule: 'every day 02:10',
      timeZone: 'Europe/Madrid',
      region: 'europe-west1',
      retryCount: 3,
      secrets: ['DEEPSEEK_API_KEY'],
    },
    async () => {
      const router = buildRouter();

      logger.info('LLM configuration', {
        useMock: ENV.USE_MOCK_LLM,
        deepSeekKeyPresent: Boolean(ENV.DEEPSEEK_API_KEY),
        activeLangs: ENV.ACTIVE_LANGS,
        router: router.name,
      });

      if (!ENV.USE_MOCK_LLM && ENV.REQUIRE_DEEPSEEK && !ENV.DEEPSEEK_API_KEY) {
        throw new Error('REQUIRE_DEEPSEEK=true but DEEPSEEK_API_KEY is missing');
      }

      if (ENV.USE_MOCK_LLM) {
        logger.warn('LLM is running in MOCK mode', {
          activeLangs: ENV.ACTIVE_LANGS,
          dateOffsetDays: ENV.DATE_OFFSET_DAYS,
        });
      }

      const generator = new HoroscopeGenerator(router);

      let created = 0;
      let skipped = 0;
      let failed = 0;

      const tasks: Array<() => Promise<void>> = [];

      for (let dayOffset = 0; dayOffset <= 7; dayOffset++) {
        const dateIso = dateIsoMadrid(dayOffset);

        for (const sign of ZODIAC_SIGNS) {
          for (const lang of ENV.ACTIVE_LANGS) {
            tasks.push(async () => {
              try {
                const {result} = await withRetry(
                    () => generator.generateOne(dateIso, sign, lang),
                    ENV.LLM_MAX_RETRIES
                );

                if (result === 'created') created++;
                else skipped++;
              } catch (e) {
                failed++;
                logger.error('generateHoroscopesWindow item failed', {
                  dateIso,
                  sign,
                  lang,
                  error: String(e),
                });
              }
            });
          }
        }
      }

      // Ejecutamos con concurrencia limitada
      await runWithConcurrency(tasks, ENV.LLM_CONCURRENCY);

      logger.info('generateHoroscopesWindow done', {
        provider: router.name,
        activeLangs: ENV.ACTIVE_LANGS,
        created,
        skipped,
        failed,
      });
    }
);
