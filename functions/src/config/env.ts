export type Lang = 'es' | 'en' | 'pt' | 'ru' | 'fr' | 'it' | 'de';

const ALL_LANGS: Lang[] = ['es', 'en', 'pt', 'ru', 'fr', 'it', 'de'];


function opt(name: string, fallback: string): string {
  return process.env[name] ?? fallback;
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
  USE_MOCK_LLM:
    (process.env.USE_MOCK_LLM ?? '').toLowerCase() === '1' ||
    (process.env.USE_MOCK_LLM ?? '').toLowerCase() === 'true',

  // Parse + validate (no casts)
  SUPPORTED_LANGS: toLangList(
    parseCsv('SUPPORTED_LANGS', 'es,en,pt,ru,fr,it,de'),
    ALL_LANGS
  ),

  // ACTIVE_LANGS se rellena abajo para poder filtrar contra SUPPORTED_LANGS
  ACTIVE_LANGS: [] as Lang[],

  GENERATOR_VERSION: Number(opt('GENERATOR_VERSION', '1')),
  LLM_TIMEOUT_MS: Number(opt('LLM_TIMEOUT_MS', '25000')),
  LLM_MAX_RETRIES: Number(opt('LLM_MAX_RETRIES', '2')),
};

// ACTIVE_LANGS: parse + validate + filtrar contra SUPPORTED_LANGS
ENV.ACTIVE_LANGS = toLangList(
  parseCsv('ACTIVE_LANGS', 'es,en,pt'),
  ['es', 'en', 'pt']
).filter((l) => ENV.SUPPORTED_LANGS.includes(l));

export function assertEnvForLLM() {
  // Si forzamos mock, no exigimos API key.
  if (ENV.USE_MOCK_LLM) return;

  // Si quieres forzar DeepSeek en producción:
  // if (process.env.REQUIRE_DEEPSEEK === 'true') req('DEEPSEEK_API_KEY');

  // En dev dejamos que el router elija mock automáticamente si falta key.
}
