import {getFirestore} from 'firebase-admin/firestore';

export type EconomyRuntimeConfig = {
  tarotEconomyV2Enabled: boolean;
};

const DEFAULT_CONFIG: EconomyRuntimeConfig = {
  tarotEconomyV2Enabled: false,
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
  };

  cachedConfig = parsed;
  cachedAtMs = now;
  return parsed;
}

export async function isTarotEconomyV2Enabled(): Promise<boolean> {
  const config = await getEconomyRuntimeConfig();
  return config.tarotEconomyV2Enabled;
}
