import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {buildRouter} from '../../llm/buildRouter';
import {ENV} from '../../config/env';
import {addLlmTokens, reserveLlmCallOrThrow, type DailyCaps} from '../../firestore/usageDaily';
import {
  completeOracleEconomyRequest,
  isOracleEconomyV2Enabled,
  refundOracleEconomyRequest,
  reserveOracleEconomyAccess,
} from '../../economy';
import {dateIsoMadrid, oracleRef, oracleSubRef} from '../firestore/paths';
import {createLlmClientFromRouter} from '../shared/routerLlmClient';
import {generateOracleAnswer} from '../oracle/oracleService';
import {ConsumeIntent, RequestType, type OracleAskData} from '../types';

const DEFAULT_DAILY_QUOTA = {
  freeTarot1Remaining: 1,
  adUnlockRemaining: 2,
  maxRequestsRemaining: 4,
  tarot3Remaining: 1,
};

type SystemMode = 'NORMAL' | 'DEGRADED' | 'EMERGENCY';

type RequestDoc = {
  uid: string;
  requestId: string;
  requestType: RequestType.ORACLE_1Q;
  lang: string;
  topic?: unknown;
  question: string;
  dateIso: string;
  intent?: ConsumeIntent;
  status: 'PROCESSING' | 'FAILED' | 'COMPLETED_SUCCESS';
  systemMode: SystemMode;
  responsePayload?: unknown;
  llmMeta?: unknown;
  error?: unknown;
  createdAt: Timestamp;
  updatedAt: Timestamp;
};

type UserDailyDoc = {
  freeTarot1Remaining: number;
  adUnlockRemaining: number;
  maxRequestsRemaining: number;
  tarot3Remaining: number;
  createdAt?: Timestamp;
  updatedAt?: Timestamp;
};

function stripUndefined<T extends Record<string, any>>(obj: T): T {
  return Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== undefined)) as T;
}

function stripUndefinedDeep<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map(stripUndefinedDeep) as any;
  }
  if (value && typeof value === 'object') {
    const out: any = {};
    for (const [k, v] of Object.entries(value as any)) {
      if (v === undefined) continue;
      out[k] = stripUndefinedDeep(v);
    }
    return out;
  }
  return value;
}

function normalizeLang(lang?: string): string {
  if (!lang) return 'es';
  const normalized = lang.trim().toLowerCase().split('-')[0].split('_')[0];
  if ((ENV.SUPPORTED_LANGS as readonly string[]).includes(normalized)) {
    return normalized;
  }
  return 'es';
}

function normalizeQuestion(question: string): string {
  return question.trim().slice(0, 400);
}

function parseSystemMode(value: unknown): SystemMode {
  if (value === 'DEGRADED') return 'DEGRADED';
  if (value === 'EMERGENCY') return 'EMERGENCY';
  return 'NORMAL';
}

async function getSystemMode(): Promise<SystemMode> {
  const db = getFirestore();
  const statusSnap = await db.doc('oracleSystemStatus/current').get();
  return parseSystemMode(statusSnap.data()?.mode);
}

async function isSubscriber(uid: string): Promise<boolean> {
  const db = getFirestore();
  const entitlementSnap = await db.doc(`userEntitlements/${uid}`).get();
  return entitlementSnap.data()?.isSubscriber === true;
}

function llmDailyCaps(): DailyCaps {
  return {
    totalMaxCalls: ENV.DAILY_LLM_MAX_CALLS_TOTAL,
    scopeMaxCalls: {
      oracle: ENV.DAILY_LLM_MAX_CALLS_ORACLE,
      unknown: ENV.DAILY_LLM_MAX_CALLS_TOTAL,
    },
  };
}

async function failRequestAndCompensateQuota(params: {
  requestRef: FirebaseFirestore.DocumentReference;
  userDailyRef: FirebaseFirestore.DocumentReference;
  intent: ConsumeIntent;
  errorMessage: string;
}) {
  const db = getFirestore();

  await db.runTransaction(async (tx) => {
    const requestSnap = await tx.get(params.requestRef);
    const requestData = requestSnap.data() as RequestDoc | undefined;

    if (requestData?.status !== 'PROCESSING') {
      return;
    }

    const compensationPatch: Record<string, FieldValue> = {
      maxRequestsRemaining: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    };

    if (params.intent === ConsumeIntent.AD_UNLOCK) {
      compensationPatch.adUnlockRemaining = FieldValue.increment(1);
    }

    tx.set(params.userDailyRef, compensationPatch, {merge: true});
    tx.set(params.requestRef, {
      status: 'FAILED',
      error: {message: params.errorMessage},
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  });
}

async function updateProviderUsage(params: {
  dateIso: string;
  provider: string;
  success: boolean;
  inputTokens?: number;
  outputTokens?: number;
  costUsd?: number;
  durationMs?: number;
}) {
  const db = getFirestore();
  const providerRef = db.doc(`llmUsageDaily/${params.dateIso}/providers/${params.provider}`);

  await providerRef.set({
    calls: FieldValue.increment(1),
    success: FieldValue.increment(params.success ? 1 : 0),
    failed: FieldValue.increment(params.success ? 0 : 1),
    inputTokens: FieldValue.increment(params.inputTokens ?? 0),
    outputTokens: FieldValue.increment(params.outputTokens ?? 0),
    costUsd: FieldValue.increment(params.costUsd ?? 0),
    latencyMsTotal: FieldValue.increment(params.durationMs ?? 0),
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});
}

export const oracleAsk = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !ENV.ALLOW_UNVERIFIED_APPCHECK_IN_DEV,
      secrets: ['DEEPSEEK_API_KEY'],
    },
    async (request) => {
      const uid = request.auth?.uid;
      if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

      const data = request.data as OracleAskData;

      if (data?.requestType !== RequestType.ORACLE_1Q) {
        throw new HttpsError('invalid-argument', 'requestType must be ORACLE_1Q');
      }
      if (!data.question || data.question.trim().length === 0) {
        throw new HttpsError('invalid-argument', 'question is required');
      }
      if (!data.requestId || typeof data.requestId !== 'string' || data.requestId.trim().length === 0) {
        throw new HttpsError('invalid-argument', 'requestId is required');
      }

      const requestId = data.requestId.trim();
      const lang = normalizeLang(data.lang);
      const question = normalizeQuestion(data.question);
      const dateIso = dateIsoMadrid();

      const economyV2Enabled = await isOracleEconomyV2Enabled();
      const [systemMode, subscriber] = await Promise.all([
        getSystemMode(),
        isSubscriber(uid),
      ]);

      if (systemMode === 'EMERGENCY' && !subscriber) {
        throw new HttpsError('resource-exhausted', 'oracleAsk temporarily unavailable');
      }

      const db = getFirestore();
      const requestRef = oracleRef('oracleRequests', requestId);
      const userDailyRef = oracleSubRef('oracleUserDaily', dateIso, 'users', uid);

      const reservation = economyV2Enabled ?
        await reserveOracleEconomyAccess({
          uid,
          requestId,
          dateIso,
          lang,
          question,
        }) :
        await db.runTransaction(async (tx) => {
          const [requestSnap, userDailySnap] = await Promise.all([
            tx.get(requestRef),
            tx.get(userDailyRef),
          ]);

          if (requestSnap.exists) {
            const existing = requestSnap.data() as RequestDoc;

            if (existing.uid !== uid) {
              throw new HttpsError('permission-denied', 'requestId already exists for a different user');
            }
            if (existing.status === 'COMPLETED_SUCCESS' && existing.responsePayload) {
              return {type: 'completed' as const, payload: existing.responsePayload};
            }
            if (existing.status === 'FAILED') {
              throw new HttpsError('aborted', 'requestId already failed; retry with new requestId');
            }
            return {type: 'in-progress' as const};
          }

          const now = Timestamp.now();
          const currentDaily = (userDailySnap.data() as UserDailyDoc | undefined) ?? {
            ...DEFAULT_DAILY_QUOTA,
            createdAt: now,
          };

          const nextDaily: UserDailyDoc = {
            freeTarot1Remaining: currentDaily.freeTarot1Remaining,
            adUnlockRemaining: currentDaily.adUnlockRemaining,
            maxRequestsRemaining: currentDaily.maxRequestsRemaining,
            tarot3Remaining: currentDaily.tarot3Remaining,
            createdAt: currentDaily.createdAt ?? now,
            updatedAt: now,
          };

          if (nextDaily.maxRequestsRemaining <= 0) {
            throw new HttpsError('resource-exhausted', 'Daily max requests exhausted');
          }

          let intent: ConsumeIntent;
          if (subscriber) {
            intent = ConsumeIntent.SUBSCRIPTION;
          } else {
            if (!data.adUnlock?.rewardedProof || data.adUnlock.rewardedProof.trim().length === 0) {
              throw new HttpsError('failed-precondition', 'rewardedProof is required for AD_UNLOCK');
            }

            const effectiveAdUnlockRemaining = systemMode === 'DEGRADED' ?
              Math.min(nextDaily.adUnlockRemaining, 1) :
              nextDaily.adUnlockRemaining;

            if (effectiveAdUnlockRemaining <= 0) {
              throw new HttpsError('resource-exhausted', 'Daily ad unlock quota exhausted');
            }

            intent = ConsumeIntent.AD_UNLOCK;
            nextDaily.adUnlockRemaining -= 1;
          }

          nextDaily.maxRequestsRemaining -= 1;

          tx.set(userDailyRef, nextDaily, {merge: true});

          const requestDoc: RequestDoc = {
            uid,
            requestId,
            requestType: data.requestType,
            lang,
            topic: data.topic,
            question,
            dateIso,
            intent,
            status: 'PROCESSING',
            systemMode,
            createdAt: now,
            updatedAt: now,
          };

          tx.create(requestRef, stripUndefined(requestDoc));

          return {
            type: 'reserved' as const,
            intent,
            quotaSnapshot: {
              freeTarot1Remaining: nextDaily.freeTarot1Remaining,
              adUnlockRemaining: nextDaily.adUnlockRemaining,
              maxRequestsRemaining: nextDaily.maxRequestsRemaining,
              tarot3Remaining: nextDaily.tarot3Remaining,
            },
          };
        });

      const legacyIntent = !economyV2Enabled &&
        reservation.type === 'reserved' &&
        'intent' in reservation ?
        reservation.intent :
        undefined;
      const legacyQuotaSnapshot = !economyV2Enabled &&
        reservation.type === 'reserved' &&
        'quotaSnapshot' in reservation ?
        reservation.quotaSnapshot :
        undefined;
      const economyDecisionSource = economyV2Enabled &&
        reservation.type === 'reserved' &&
        'source' in reservation ?
        reservation.source :
        undefined;
      const economyMoonCost = economyV2Enabled &&
        reservation.type === 'reserved' &&
        'moonCost' in reservation ?
        reservation.moonCost :
        undefined;

      if (reservation.type === 'completed') {
        return reservation.payload;
      }

      if (reservation.type === 'in-progress') {
        return {requestId, status: 'IN_PROGRESS'};
      }

      const router = buildRouter();

      let usageDateIso: string;
      try {
        const {dateIso: reservedDateIso} = await reserveLlmCallOrThrow('oracle', llmDailyCaps());
        usageDateIso = reservedDateIso;
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : String(error);

        if (errorMessage.includes('DAILY_LLM_CAP_EXCEEDED')) {
          if (economyV2Enabled) {
            await refundOracleEconomyRequest({
              uid,
              requestId,
              dateIso,
              errorMessage: 'DAILY_LLM_CAP_EXCEEDED',
            });
          } else {
            await failRequestAndCompensateQuota({
              requestRef,
              userDailyRef,
              intent: legacyIntent ?? ConsumeIntent.AD_UNLOCK,
              errorMessage: 'DAILY_LLM_CAP_EXCEEDED',
            });
          }
          throw new HttpsError('resource-exhausted', 'DAILY_LLM_CAP_EXCEEDED');
        }

        throw error;
      }

      const llmClient = createLlmClientFromRouter(router, 'oracle', {
        usageDailyDateIso: usageDateIso,
        skipUsageReservation: true,
        skipUsageTokenTracking: true,
      });

      try {
        const generated = await generateOracleAnswer({
          requestId,
          lang,
          question,
          topic: data.topic,
          llm: llmClient,
        });

        const llmMeta = stripUndefinedDeep(generated.llmMeta);

        const responsePayload = {
          requestId,
          status: 'COMPLETED_SUCCESS',
          answer: generated.answer,
          quotaSnapshot: economyV2Enabled ? undefined : legacyQuotaSnapshot,
          systemMode,
          economy: economyV2Enabled ? {
            source: economyDecisionSource,
            moonCost: economyMoonCost,
          } : undefined,
        };

        const answerRef = oracleSubRef('oracleAnswers', uid, 'items', requestId);

        await db.runTransaction(async (tx) => {
          if (!economyV2Enabled) {
            tx.set(requestRef, {
              status: 'COMPLETED_SUCCESS',
              responsePayload,
              llmMeta,
              updatedAt: FieldValue.serverTimestamp(),
            }, {merge: true});
          }

          const answerDoc = stripUndefined({
            requestId,
            lang,
            topic: data.topic,
            question,
            answer: generated.answer,
            createdAt: FieldValue.serverTimestamp(),
            llmMeta,
          });

          tx.set(answerRef, answerDoc, {merge: true});
        });

        if (economyV2Enabled) {
          await completeOracleEconomyRequest({
            uid,
            requestId,
            responsePayload,
            llmMeta,
          });
        }

        await addLlmTokens(
            'oracle',
            usageDateIso,
            generated.llmMeta.inputTokens,
            generated.llmMeta.outputTokens
        );

        await updateProviderUsage({
          dateIso: usageDateIso,
          provider: generated.llmMeta.provider,
          success: true,
          inputTokens: generated.llmMeta.inputTokens,
          outputTokens: generated.llmMeta.outputTokens,
          costUsd: generated.llmMeta.costUsd,
          durationMs: generated.llmMeta.durationMs,
        });

        return stripUndefinedDeep(responsePayload);
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : String(error);

        if (economyV2Enabled) {
          await refundOracleEconomyRequest({
            uid,
            requestId,
            dateIso,
            errorMessage,
          });
        } else {
          await requestRef.set({
            status: 'FAILED',
            error: {
              message: errorMessage,
            },
            updatedAt: FieldValue.serverTimestamp(),
          }, {merge: true});
        }

        await updateProviderUsage({
          dateIso: usageDateIso,
          provider: router.name,
          success: false,
        });

        throw new HttpsError('internal', 'Failed to generate oracle answer');
      }
    }
);
