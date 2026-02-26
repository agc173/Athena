export type Lang = 'es' | 'en' | 'pt' | 'ru' | 'fr' | 'it' | 'de';

function req(name: string): string {
  const v = process.env[name];
  if (!v) throw new Error(`Missing env var: ${name}`);
  return v;
}

function opt(name: string, fallback: string): string {
  return process.env[name] ?? fallback;
}

export const ENV = {
  DEEPSEEK_API_KEY: process.env.DEEPSEEK_API_KEY ?? '',
  ACTIVE_LANGS: opt('ACTIVE_LANGS', 'es,en,pt').split(',').map((s) => s.trim()).filter(Boolean) as Lang[],
  SUPPORTED_LANGS: opt('SUPPORTED_LANGS', 'es,en,pt,ru,fr,it,de').split(',').map((s) => s.trim()).filter(Boolean) as Lang[],
  GENERATOR_VERSION: Number(opt('GENERATOR_VERSION', '1')),
  LLM_TIMEOUT_MS: Number(opt('LLM_TIMEOUT_MS', '25000')),
  LLM_MAX_RETRIES: Number(opt('LLM_MAX_RETRIES', '2')),
};

export function assertEnvForLLM() {
  // DeepSeek es primary, pero permitimos vacío para fallback-only en desarrollo.
  // Para forzar DeepSeek siempre, define REQUIRE_DEEPSEEK=true.
  if (process.env.REQUIRE_DEEPSEEK === 'true') req('DEEPSEEK_API_KEY');
}
