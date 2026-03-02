import {FieldValue, getFirestore} from 'firebase-admin/firestore';

export type LlmScope = 'horoscope' | 'tarot' | 'oracle' | 'unknown' | string;

export type DailyCaps = {
  totalMaxCalls: number; // hard fuse
  scopeMaxCalls: Record<string, number>; // per-scope hard caps
};

type UsageDoc = {
  calls?: number;
  inputTokens?: number;
  outputTokens?: number;
  updatedAtEpochMillis?: number;
};

function dateIsoMadrid(now = new Date()): string {
  // YYYY-MM-DD in Europe/Madrid without external deps
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Madrid',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now);
}

function clampNonNegativeInt(n: number): number {
  if (!Number.isFinite(n)) return 0;
  return Math.max(0, Math.floor(n));
}

function capsForScope(scope: string, caps: DailyCaps): {totalMax: number; scopeMax: number} {
  const totalMax = caps.totalMaxCalls;
  const scopeMax = caps.scopeMaxCalls[scope] ?? caps.scopeMaxCalls.unknown ?? totalMax;
  return {totalMax, scopeMax};
}

function usageRef(dateIso: string, scope: string) {
  // llmUsageDaily/{dateIso}/scopes/{scope}
  const db = getFirestore();
  return db.doc(`llmUsageDaily/${dateIso}/scopes/${scope}`);
}

export async function reserveLlmCallOrThrow(scope: LlmScope, caps: DailyCaps): Promise<{dateIso: string}> {
  const dateIso = dateIsoMadrid();
  const db = getFirestore();
  const totalRef = usageRef(dateIso, 'TOTAL');
  const scopeRef = usageRef(dateIso, scope);

  const {totalMax, scopeMax} = capsForScope(scope, caps);

  await db.runTransaction(async (tx) => {
    const [totalSnap, scopeSnap] = await Promise.all([tx.get(totalRef), tx.get(scopeRef)]);

    const total = (totalSnap.data() as UsageDoc | undefined) ?? {};
    const scoped = (scopeSnap.data() as UsageDoc | undefined) ?? {};

    const totalCalls = clampNonNegativeInt(total.calls ?? 0);
    const scopeCalls = clampNonNegativeInt(scoped.calls ?? 0);

    if (totalCalls + 1 > totalMax) {
      throw new Error(`DAILY_LLM_CAP_EXCEEDED_TOTAL dateIso=${dateIso} totalCalls=${totalCalls} max=${totalMax}`);
    }
    if (scopeCalls + 1 > scopeMax) {
      throw new Error(`DAILY_LLM_CAP_EXCEEDED_SCOPE scope=${scope} dateIso=${dateIso} scopeCalls=${scopeCalls} max=${scopeMax}`);
    }

    const patch = {
      calls: FieldValue.increment(1),
      updatedAtEpochMillis: Date.now(),
    };

    tx.set(totalRef, patch, {merge: true});
    tx.set(scopeRef, patch, {merge: true});
  });

  return {dateIso};
}

export async function addLlmTokens(scope: LlmScope, dateIso: string, inputTokens: number, outputTokens: number) {
  const db = getFirestore();
  const totalRef = usageRef(dateIso, 'TOTAL');
  const scopeRef = usageRef(dateIso, scope);

  const patch = {
    inputTokens: FieldValue.increment(clampNonNegativeInt(inputTokens)),
    outputTokens: FieldValue.increment(clampNonNegativeInt(outputTokens)),
    updatedAtEpochMillis: Date.now(),
  };

  await db.runTransaction(async (tx) => {
    tx.set(totalRef, patch, {merge: true});
    tx.set(scopeRef, patch, {merge: true});
  });
}
