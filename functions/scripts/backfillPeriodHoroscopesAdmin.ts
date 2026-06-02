import {applicationDefault, getApps, initializeApp} from 'firebase-admin/app';
import {ENV, type Lang} from '../src/config/env';
import {PeriodHoroscopeGenerator} from '../src/generators/PeriodHoroscopeGenerator';
import {buildRouter} from '../src/llm/buildRouter';
import {
  type BackfillInput,
  type BackfillPeriodType,
  MAX_MONTHLY_PERIODS,
  MAX_WEEKLY_PERIODS,
  runBackfillPeriodHoroscopes,
} from '../src/admin/backfillPeriodHoroscopesCore';
import {logger} from '../src/utils/logger';
import {withRetry} from '../src/utils/retry';

type CliArgs = Record<string, string[]>;

function parseCliArgs(argv: string[]): CliArgs {
  const args: CliArgs = {};
  for (let i = 0; i < argv.length; i++) {
    const token = argv[i];
    if (!token.startsWith('--')) {
      throw new Error(`Unexpected argument: ${token}`);
    }

    const eq = token.indexOf('=');
    const key = eq >= 0 ? token.slice(2, eq) : token.slice(2);
    const value = eq >= 0 ? token.slice(eq + 1) : argv[++i];
    if (!key || value == null || value.startsWith('--')) {
      throw new Error(`Missing value for --${key}`);
    }
    args[key] = [...(args[key] ?? []), value];
  }
  return args;
}

function requiredArg(args: CliArgs, key: string): string {
  const value = args[key]?.at(-1)?.trim();
  if (!value) throw new Error(`Missing required argument --${key}`);
  return value;
}

function optionalListArg(args: CliArgs, key: string): string[] | undefined {
  const values = args[key]
      ?.flatMap((value) => value.split(','))
      .map((value) => value.trim())
      .filter(Boolean);
  return values?.length ? values : undefined;
}

function parsePeriodType(value: string): BackfillPeriodType {
  if (value === 'weekly' || value === 'monthly') return value;
  throw new Error('Invalid --periodType. Expected weekly or monthly');
}

function parseInput(argv: string[]): BackfillInput {
  const args = parseCliArgs(argv);
  const input: BackfillInput = {
    periodType: parsePeriodType(requiredArg(args, 'periodType')),
    startKey: requiredArg(args, 'startKey'),
    endKey: requiredArg(args, 'endKey'),
    signs: optionalListArg(args, 'signs'),
    langs: optionalListArg(args, 'langs') as Lang[] | undefined,
  };
  return input;
}

function initializeAdminApp() {
  if (getApps().length) return;

  const projectId = process.env.FIREBASE_PROJECT_ID ?? process.env.GCLOUD_PROJECT;
  initializeApp({
    credential: applicationDefault(),
    ...(projectId ? {projectId} : {}),
  });
}

async function main() {
  const input = parseInput(process.argv.slice(2));
  initializeAdminApp();

  logger.info('backfillPeriodHoroscopes admin script starting', {
    input,
    activeLangs: ENV.ACTIVE_LANGS,
    maxRetries: ENV.LLM_MAX_RETRIES,
    limits: {
      weekly: MAX_WEEKLY_PERIODS,
      monthly: MAX_MONTHLY_PERIODS,
    },
  });

  const result = await runBackfillPeriodHoroscopes(input, {
    activeLangs: ENV.ACTIVE_LANGS,
    maxRetries: ENV.LLM_MAX_RETRIES,
    buildRouter,
    PeriodHoroscopeGenerator,
    withRetry,
    logger,
  });

  logger.info('backfillPeriodHoroscopes admin script finished', result);
}

main().catch((error: unknown) => {
  console.error('backfillPeriodHoroscopes admin script failed', error);
  process.exit(1);
});
