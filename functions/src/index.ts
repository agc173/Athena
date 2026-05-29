import {onSchedule} from 'firebase-functions/v2/scheduler';
import {initializeApp} from 'firebase-admin/app';
import {DateTime} from 'luxon';
import type {ZodiacSign} from './firestore/paths';
import {logger} from './utils/logger';
import {withRetry} from './utils/retry';
export {oracleGetStatus, tarotDraw, oracleAsk} from './oracle';
export {saveUserProfile} from './userprofile';
export {birthEssenceGenerate} from './birthessence';
export {validateGooglePlayPurchase, restoreGooglePlayPurchases, refreshEntitlement} from './premium';
export {requestAccountDeletion, restoreAccount} from './account';
export {backfillPeriodHoroscopes} from './admin/callables/backfillPeriodHoroscopes';
export {
  registerPushToken,
  unregisterPushToken,
  updateNotificationPreferences,
  sendTestNotification,
  sendDailyHoroscopeNotifications,
} from './notifications';
export {
  getEconomyBalance,
  getEconomyStatus,
  getEconomyModulePreviews,
  claimDailyLogin,
  claimRewardedAd,
  unlockHoroscopeDay,
  getHoroscopeDailyUnlocks,
  unlockHoroscopeWeekly,
  unlockHoroscopeMonthly,
  getHoroscopeWeeklyUnlocks,
  getHoroscopeMonthlyUnlocks,
  synastryAuthorize,
  pendulumAuthorize,
  claimMoonPackPurchase,
} from './economy';
export {claimDailyConstellationProgress} from './economy/callables/claimDailyConstellationProgress';
export {getConstellationProgress} from './economy/callables/getConstellationProgress';

initializeApp();

const ZODIAC_SIGNS: ZodiacSign[] = [
  'aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo',
  'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces',
];

type SchedulerEnv = {
  DATE_OFFSET_DAYS: number;
  ACTIVE_LANGS: string[];
  USE_MOCK_LLM: boolean;
  DEEPSEEK_API_KEY: string;
  REQUIRE_DEEPSEEK: boolean;
  LLM_CONCURRENCY: number;
  LLM_MAX_RETRIES: number;
  WEEKLY_WINDOW_SIZE: number;
  MONTHLY_WINDOW_SIZE: number;
};

type SchedulerGenerateResult = {result: string; path: string; provider: string};

function dateIsoMadrid(dayOffset: number, env: SchedulerEnv): string {
  return DateTime.now()
      .setZone('Europe/Madrid')
      .plus({days: dayOffset + env.DATE_OFFSET_DAYS})
      .toISODate()!;
}

function weekKeyMadrid(weekOffset: number, env: SchedulerEnv): string {
  const base = DateTime.now()
      .setZone('Europe/Madrid')
      .plus({days: env.DATE_OFFSET_DAYS})
      .startOf('week')
      .plus({weeks: weekOffset});
  return base.toFormat('kkkk-\'W\'WW');
}

function monthKeyMadrid(monthOffset: number, env: SchedulerEnv): string {
  return DateTime.now()
      .setZone('Europe/Madrid')
      .plus({days: env.DATE_OFFSET_DAYS})
      .startOf('month')
      .plus({months: monthOffset})
      .toFormat('yyyy-MM');
}

function rangeOffsets(windowSize: number): number[] {
  return Array.from({length: Math.max(2, windowSize)}, (_, i) => i);
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
      const [
        {ENV, assertEnvForLLM},
        {buildRouter},
        {HoroscopeGenerator},
      ] = await Promise.all([
        import('./config/env.js'),
        import('./llm/buildRouter.js'),
        import('./generators/HoroscopeGenerator.js'),
      ]);

      assertEnvForLLM();

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
      let repaired = 0;
      let failed = 0;

      // ✅ Hard cap stop flag (prevents log spam + pointless retries)
      let stoppedByCap = false;
      let capError: string | undefined;

      const canonicalTasks: Array<() => Promise<void>> = [];
      const translationTasks: Array<() => Promise<void>> = [];

      for (let dayOffset = 0; dayOffset <= 13; dayOffset++) {
        const dateIso = dateIsoMadrid(dayOffset, ENV);

        // Fase A: generar canonical en español por signo.
        for (const sign of ZODIAC_SIGNS) {
          canonicalTasks.push(async () => {
          // ✅ If cap already exceeded, skip remaining tasks quietly
            if (stoppedByCap) return;

            try {
              const {result} = await withRetry<SchedulerGenerateResult>(
                  () => generator.generateCanonical(dateIso, sign),
                  ENV.LLM_MAX_RETRIES
              );

              if (result === 'created') created++;
              else if (result === 'repaired') repaired++;
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
                const {result} = await withRetry<SchedulerGenerateResult>(
                    () => generator.generateTranslation(dateIso, sign, lang),
                    ENV.LLM_MAX_RETRIES
                );

                if (result === 'created') created++;
                else if (result === 'repaired') repaired++;
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
        repaired,
        failed,
        stoppedByCap,
        capError,
      });
    }
);

export const generateWeeklyHoroscopesWindow = onSchedule(
    {
      schedule: 'every day 02:20',
      timeZone: 'Europe/Madrid',
      region: 'europe-west1',
      retryCount: 0,
      timeoutSeconds: 600,
      secrets: ['DEEPSEEK_API_KEY'],
    },
    async () => {
      const [
        {ENV, assertEnvForLLM},
        {buildRouter},
        {PeriodHoroscopeGenerator},
      ] = await Promise.all([
        import('./config/env.js'),
        import('./llm/buildRouter.js'),
        import('./generators/PeriodHoroscopeGenerator.js'),
      ]);

      assertEnvForLLM();
      const router = buildRouter();
      const generator = new PeriodHoroscopeGenerator(router);

      let created = 0;
      let skipped = 0;
      let repaired = 0;
      let failed = 0;
      let stoppedByCap = false;
      let capError: string | undefined;
      let blockedByCanonicalFailure = 0;

      const canonicalTasks: Array<() => Promise<void>> = [];
      const translationTasks: Array<() => Promise<void>> = [];
      const failedCanonicalPairs = new Set<string>();

      for (const weekOffset of rangeOffsets(ENV.WEEKLY_WINDOW_SIZE)) {
        const weekKey = weekKeyMadrid(weekOffset, ENV);

        for (const sign of ZODIAC_SIGNS) {
          canonicalTasks.push(async () => {
            if (stoppedByCap) return;

            try {
              const {result} = await withRetry<SchedulerGenerateResult>(
                  () => generator.generateWeeklyCanonical(weekKey, sign),
                  ENV.LLM_MAX_RETRIES
              );

              if (result === 'created') created++;
              else if (result === 'repaired') repaired++;
              else skipped++;
            } catch (e) {
              const msg = (e as Error)?.message ?? String(e);
              if (msg.includes('DAILY_LLM_CAP_EXCEEDED')) {
                stoppedByCap = true;
                capError = msg;
                logger.warn('generateWeeklyHoroscopesWindow stopped by daily cap', {
                  phase: 'canonical',
                  weekKey,
                  sign,
                  error: msg,
                });
                return;
              }
              failed++;
              failedCanonicalPairs.add(`${weekKey}|${sign}`);
              logger.error('generateWeeklyHoroscopesWindow item failed', {
                phase: 'canonical',
                weekKey,
                sign,
                error: msg,
              });
            }
          });
        }

        for (const sign of ZODIAC_SIGNS) {
          for (const lang of ENV.ACTIVE_LANGS.filter((item) => item !== 'es')) {
            translationTasks.push(async () => {
              if (stoppedByCap) return;
              if (failedCanonicalPairs.has(`${weekKey}|${sign}`)) {
                blockedByCanonicalFailure++;
                logger.warn('generateWeeklyHoroscopesWindow translation blocked', {
                  phase: 'translation',
                  reason: 'canonical_failed',
                  weekKey,
                  sign,
                  lang,
                });
                return;
              }

              try {
                const {result} = await withRetry<SchedulerGenerateResult>(
                    () => generator.generateWeeklyTranslation(weekKey, sign, lang),
                    ENV.LLM_MAX_RETRIES
                );

                if (result === 'created') created++;
                else if (result === 'repaired') repaired++;
                else skipped++;
              } catch (e) {
                const msg = (e as Error)?.message ?? String(e);
                if (msg.includes('DAILY_LLM_CAP_EXCEEDED')) {
                  stoppedByCap = true;
                  capError = msg;
                  logger.warn('generateWeeklyHoroscopesWindow stopped by daily cap', {
                    phase: 'translation',
                    weekKey,
                    sign,
                    lang,
                    error: msg,
                  });
                  return;
                }
                failed++;
                logger.error('generateWeeklyHoroscopesWindow item failed', {
                  phase: 'translation',
                  weekKey,
                  sign,
                  lang,
                  error: msg,
                });
              }
            });
          }
        }
      }

      await runWithConcurrency(canonicalTasks, ENV.LLM_CONCURRENCY);
      await runWithConcurrency(translationTasks, ENV.LLM_CONCURRENCY);

      logger.info('generateWeeklyHoroscopesWindow done', {
        provider: router.name,
        activeLangs: ENV.ACTIVE_LANGS,
        created,
        skipped,
        repaired,
        failed,
        blockedByCanonicalFailure,
        stoppedByCap,
        capError,
      });
    }
);

export const generateMonthlyHoroscopesWindow = onSchedule(
    {
      schedule: 'every day 03:05',
      timeZone: 'Europe/Madrid',
      region: 'europe-west1',
      retryCount: 0,
      timeoutSeconds: 600,
      secrets: ['DEEPSEEK_API_KEY'],
    },
    async () => {
      const [
        {ENV, assertEnvForLLM},
        {buildRouter},
        {PeriodHoroscopeGenerator},
      ] = await Promise.all([
        import('./config/env.js'),
        import('./llm/buildRouter.js'),
        import('./generators/PeriodHoroscopeGenerator.js'),
      ]);

      assertEnvForLLM();
      const router = buildRouter();
      const generator = new PeriodHoroscopeGenerator(router);

      let created = 0;
      let skipped = 0;
      let repaired = 0;
      let failed = 0;
      let stoppedByCap = false;
      let capError: string | undefined;
      let blockedByCanonicalFailure = 0;

      const canonicalTasks: Array<() => Promise<void>> = [];
      const translationTasks: Array<() => Promise<void>> = [];
      const failedCanonicalPairs = new Set<string>();

      for (const monthOffset of rangeOffsets(ENV.MONTHLY_WINDOW_SIZE)) {
        const monthKey = monthKeyMadrid(monthOffset, ENV);

        for (const sign of ZODIAC_SIGNS) {
          canonicalTasks.push(async () => {
            if (stoppedByCap) return;

            try {
              const {result} = await withRetry<SchedulerGenerateResult>(
                  () => generator.generateMonthlyCanonical(monthKey, sign),
                  ENV.LLM_MAX_RETRIES
              );

              if (result === 'created') created++;
              else if (result === 'repaired') repaired++;
              else skipped++;
            } catch (e) {
              const msg = (e as Error)?.message ?? String(e);
              if (msg.includes('DAILY_LLM_CAP_EXCEEDED')) {
                stoppedByCap = true;
                capError = msg;
                logger.warn('generateMonthlyHoroscopesWindow stopped by daily cap', {
                  phase: 'canonical',
                  monthKey,
                  sign,
                  error: msg,
                });
                return;
              }
              failed++;
              failedCanonicalPairs.add(`${monthKey}|${sign}`);
              logger.error('generateMonthlyHoroscopesWindow item failed', {
                phase: 'canonical',
                monthKey,
                sign,
                error: msg,
              });
            }
          });
        }

        for (const sign of ZODIAC_SIGNS) {
          for (const lang of ENV.ACTIVE_LANGS.filter((item) => item !== 'es')) {
            translationTasks.push(async () => {
              if (stoppedByCap) return;
              if (failedCanonicalPairs.has(`${monthKey}|${sign}`)) {
                blockedByCanonicalFailure++;
                logger.warn('generateMonthlyHoroscopesWindow translation blocked', {
                  phase: 'translation',
                  reason: 'canonical_failed',
                  monthKey,
                  sign,
                  lang,
                });
                return;
              }

              try {
                const {result} = await withRetry<SchedulerGenerateResult>(
                    () => generator.generateMonthlyTranslation(monthKey, sign, lang),
                    ENV.LLM_MAX_RETRIES
                );

                if (result === 'created') created++;
                else if (result === 'repaired') repaired++;
                else skipped++;
              } catch (e) {
                const msg = (e as Error)?.message ?? String(e);
                if (msg.includes('DAILY_LLM_CAP_EXCEEDED')) {
                  stoppedByCap = true;
                  capError = msg;
                  logger.warn('generateMonthlyHoroscopesWindow stopped by daily cap', {
                    phase: 'translation',
                    monthKey,
                    sign,
                    lang,
                    error: msg,
                  });
                  return;
                }
                failed++;
                logger.error('generateMonthlyHoroscopesWindow item failed', {
                  phase: 'translation',
                  monthKey,
                  sign,
                  lang,
                  error: msg,
                });
              }
            });
          }
        }
      }

      await runWithConcurrency(canonicalTasks, ENV.LLM_CONCURRENCY);
      await runWithConcurrency(translationTasks, ENV.LLM_CONCURRENCY);

      logger.info('generateMonthlyHoroscopesWindow done', {
        provider: router.name,
        activeLangs: ENV.ACTIVE_LANGS,
        created,
        skipped,
        repaired,
        failed,
        blockedByCanonicalFailure,
        stoppedByCap,
        capError,
      });
    }
);
