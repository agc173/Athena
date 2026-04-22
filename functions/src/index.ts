import {onSchedule} from 'firebase-functions/v2/scheduler';
import {initializeApp} from 'firebase-admin/app';
import {DateTime} from 'luxon';
import {ENV, assertEnvForLLM} from './config/env';
import type {ZodiacSign} from './firestore/paths';
import {HoroscopeGenerator} from './generators/HoroscopeGenerator';
import {logger} from './utils/logger';
import {buildRouter} from './llm/buildRouter';
import {withRetry} from './utils/retry';
export {oracleGetStatus, tarotDraw, oracleAsk} from './oracle';
export {saveUserProfile} from './userprofile';
export {birthEssenceGenerate} from './birthessence';
export {getEconomyBalance, getEconomyStatus, claimDailyLogin} from './economy';

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


export const generateHoroscopesWindow = onSchedule(
    {
      schedule: 'every day 02:10',
      timeZone: 'Europe/Madrid',
      region: 'europe-west1',
      retryCount: 0,
      timeoutSeconds: 600,
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

      // ✅ Hard cap stop flag (prevents log spam + pointless retries)
      let stoppedByCap = false;
      let capError: string | undefined;

      const canonicalTasks: Array<() => Promise<void>> = [];
      const translationTasks: Array<() => Promise<void>> = [];

      for (let dayOffset = 0; dayOffset <= 13; dayOffset++) {
        const dateIso = dateIsoMadrid(dayOffset);

        // Fase A: generar canonical en español por signo.
        for (const sign of ZODIAC_SIGNS) {
          canonicalTasks.push(async () => {
          // ✅ If cap already exceeded, skip remaining tasks quietly
            if (stoppedByCap) return;

            try {
              const {result} = await withRetry(
                  () => generator.generateCanonical(dateIso, sign),
                  ENV.LLM_MAX_RETRIES
              );

              if (result === 'created') created++;
              else skipped++;
            } catch (e) {
              const msg = (e as any)?.message ?? String(e);

              // ✅ If daily cap exceeded, stop scheduling further work
              if (msg.includes('DAILY_LLM_CAP_EXCEEDED')) {
                stoppedByCap = true;
                capError = msg;
                logger.warn('generateHoroscopesWindow stopped by daily cap', {
                  phase: 'canonical',
                  dateIso,
                  sign,
                  error: msg,
                });
                return;
              }

              failed++;
              logger.error('generateHoroscopesWindow item failed', {
                phase: 'canonical',
                dateIso,
                sign,
                error: msg,
              });
            }
          });
        }

        // Fase B: generar traducciones para lang != es.
        for (const sign of ZODIAC_SIGNS) {
          for (const lang of ENV.ACTIVE_LANGS.filter((item) => item !== 'es')) {
            translationTasks.push(async () => {
              // ✅ If cap already exceeded, skip remaining tasks quietly
              if (stoppedByCap) return;

              try {
                const {result} = await withRetry(
                    () => generator.generateTranslation(dateIso, sign, lang),
                    ENV.LLM_MAX_RETRIES
                );

                if (result === 'created') created++;
                else skipped++;
              } catch (e) {
                const msg = (e as any)?.message ?? String(e);

                // ✅ If daily cap exceeded, stop scheduling further work
                if (msg.includes('DAILY_LLM_CAP_EXCEEDED')) {
                  stoppedByCap = true;
                  capError = msg;
                  logger.warn('generateHoroscopesWindow stopped by daily cap', {
                    phase: 'translation',
                    dateIso,
                    sign,
                    lang,
                    error: msg,
                  });
                  return;
                }

                failed++;
                logger.error('generateHoroscopesWindow item failed', {
                  phase: 'translation',
                  dateIso,
                  sign,
                  lang,
                  error: msg,
                });
              }
            });
          }
        }
      }

      // Ejecutamos con concurrencia limitada y por fases.
      await runWithConcurrency(canonicalTasks, ENV.LLM_CONCURRENCY);
      await runWithConcurrency(translationTasks, ENV.LLM_CONCURRENCY);

      logger.info('generateHoroscopesWindow done', {
        provider: router.name,
        activeLangs: ENV.ACTIVE_LANGS,
        created,
        skipped,
        failed,
        stoppedByCap,
        capError,
      });
    }
);
