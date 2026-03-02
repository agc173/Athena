export type Lang = 'es' | 'en' | 'pt' | 'ru' | 'fr' | 'it' | 'de';

const ALL_LANGS: Lang[] = ['es', 'en', 'pt', 'ru', 'fr', 'it', 'de'];

function opt(name: string, fallback: string): string {
  return process.env[name] ?? fallback;
}

function optNum(name: string, fallback: number): number {
  const raw = process.env[name];
  if (raw == null || raw.trim() === '') return fallback;
  const n = Number(raw);
  return Number.isFinite(n) ? n : fallback;
}

function parseCsv(name: string, fallback: string): string[] {
  return opt(name, fallback)
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
}

function toLangList(values: string[], fallback: Lang[]): Lang[] {
  const out = values.filter((v): v is Lang => (ALL_LANGS as string[]).includes(v));
  return out.length ? out : fallback;
}

export const ENV = {
  DEEPSEEK_API_KEY: process.env.DEEPSEEK_API_KEY ?? '',
  REQUIRE_DEEPSEEK: (process.env.REQUIRE_DEEPSEEK ?? '').toLowerCase() === 'true',
  USE_MOCK_LLM:
    (process.env.USE_MOCK_LLM ?? '').toLowerCase() === '1' ||
    (process.env.USE_MOCK_LLM ?? '').toLowerCase() === 'true',

  // Se mantienen todos para futuro multi-idioma, pero NO lo activamos aÃºn.
  SUPPORTED_LANGS: toLangList(parseCsv('SUPPORTED_LANGS', 'es,en,pt,ru,fr,it,de'), ALL_LANGS),

  HOROSCOPE_USE_LANGS: (process.env.HOROSCOPE_USE_LANGS ?? '').toLowerCase() === 'true',

  // ACTIVE_LANGS se rellena abajo para poder filtrar contra SUPPORTED_LANGS
  ACTIVE_LANGS: [] as Lang[],

  GENERATOR_VERSION: optNum('GENERATOR_VERSION', 1),

  // LLM tuning (control de coste/calidad)
  LLM_TEMPERATURE: optNum('LLM_TEMPERATURE', 0.4),
  LLM_MAX_TOKENS: optNum('LLM_MAX_TOKENS', 350),

  LLM_TIMEOUT_MS: optNum('LLM_TIMEOUT_MS', 25000),
  LLM_MAX_RETRIES: optNum('LLM_MAX_RETRIES', 2),
  DATE_OFFSET_DAYS: optNum('DATE_OFFSET_DAYS', 0),
  LLM_CONCURRENCY: optNum('LLM_CONCURRENCY', 4),
  DAILY_LLM_MAX_CALLS_TOTAL: optNum('DAILY_LLM_MAX_CALLS_TOTAL', 500),
  DAILY_LLM_MAX_CALLS_HOROSCOPE: optNum('DAILY_LLM_MAX_CALLS_HOROSCOPE', 120),
  DAILY_LLM_MAX_CALLS_TAROT: optNum('DAILY_LLM_MAX_CALLS_TAROT', 250),
  DAILY_LLM_MAX_CALLS_ORACLE: optNum('DAILY_LLM_MAX_CALLS_ORACLE', 250),
  DAILY_LLM_MAX_CALLS_UNKNOWN: optNum('DAILY_LLM_MAX_CALLS_UNKNOWN', 500),
  // Cost metrics (USD per 1M tokens) + FX
  DEEPSEEK_PRICE_INPUT_PER_MILLION_USD: optNum('DEEPSEEK_PRICE_INPUT_PER_MILLION_USD', 0.27),
  DEEPSEEK_PRICE_OUTPUT_PER_MILLION_USD: optNum('DEEPSEEK_PRICE_OUTPUT_PER_MILLION_USD', 1.10),

};

// ACTIVE_LANGS: parse + validate + filtrar contra SUPPORTED_LANGS
// âœ… Paso 3: por defecto SOLO 'es' (single-language) hasta estabilizar DeepSeek.
ENV.ACTIVE_LANGS = toLangList(parseCsv('ACTIVE_LANGS', 'es'), ['es']).filter((l) =>
  ENV.SUPPORTED_LANGS.includes(l)
);

// Evita mutaciones accidentales en runtime
Object.freeze(ENV);

export function assertEnvForLLM() {
  if (ENV.USE_MOCK_LLM) return;

  if (ENV.REQUIRE_DEEPSEEK && !ENV.DEEPSEEK_API_KEY) {
    throw new Error('Missing env var: DEEPSEEK_API_KEY');
  }
}
