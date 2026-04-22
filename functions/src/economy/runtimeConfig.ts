import {getFirestore} from 'firebase-admin/firestore';

export type EconomyRuntimeConfig = {
  tarotEconomyV2Enabled: boolean;
  oracleEconomyV2Enabled: boolean;
  birthEssenceEconomyV2Enabled: boolean;
};

const DEFAULT_CONFIG: EconomyRuntimeConfig = {
  tarotEconomyV2Enabled: false,
  oracleEconomyV2Enabled: false,
  birthEssenceEconomyV2Enabled: false,
};

const CONFIG_DOC_PATH = 'config/economy/current';
const CACHE_TTL_MS = 30_000;

let cachedConfig: EconomyRuntimeConfig | null = null;
let cachedAtMs = 0;

function normalizeBoolean(value: unknown, fallback: boolean): boolean {
  if (typeof value === 'boolean') return value;
  return fallback;
}

export async function getEconomyRuntimeConfig(): Promise<EconomyRuntimeConfig> {
  const now = Date.now();
  if (cachedConfig && now - cachedAtMs <= CACHE_TTL_MS) {
    return cachedConfig;
  }

  const db = getFirestore();
  const configSnap = await db.doc(CONFIG_DOC_PATH).get();
  const raw = configSnap.data() ?? {};

  const parsed: EconomyRuntimeConfig = {
    tarotEconomyV2Enabled: normalizeBoolean(
        raw.tarotEconomyV2Enabled,
        DEFAULT_CONFIG.tarotEconomyV2Enabled
    ),
    oracleEconomyV2Enabled: normalizeBoolean(
        raw.oracleEconomyV2Enabled,
        DEFAULT_CONFIG.oracleEconomyV2Enabled
    ),
    birthEssenceEconomyV2Enabled: normalizeBoolean(
        raw.birthEssenceEconomyV2Enabled,
        DEFAULT_CONFIG.birthEssenceEconomyV2Enabled
    ),
  };

  cachedConfig = parsed;
  cachedAtMs = now;
  return parsed;
}

export async function isTarotEconomyV2Enabled(): Promise<boolean> {
  return isEconomyV2Enabled('tarot');
}

export async function isOracleEconomyV2Enabled(): Promise<boolean> {
  return isEconomyV2Enabled('oracle');
}

export async function isBirthEssenceEconomyV2Enabled(): Promise<boolean> {
  return isEconomyV2Enabled('birth-essence');
}

export async function isEconomyV2Enabled(module: 'tarot' | 'oracle' | 'birth-essence'): Promise<boolean> {
  const config = await getEconomyRuntimeConfig();

  switch (module) {
    case 'tarot':
      return config.tarotEconomyV2Enabled;
    case 'oracle':
      return config.oracleEconomyV2Enabled;
    case 'birth-essence':
      return config.birthEssenceEconomyV2Enabled;
  }
}
