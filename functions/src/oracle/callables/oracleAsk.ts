import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {ENV} from '../../config/env';
import {addLlmTokens, reserveLlmCallOrThrow, type DailyCaps} from '../../firestore/usageDaily';
import {
  completeOracleEconomyRequest,
  isOracleEconomyV2Enabled,
  refundOracleEconomyRequest,
  reserveOracleEconomyAccess,
} from '../../economy';
import {dateIsoMadrid, oracleSubRef} from '../firestore/paths';
import {createLlmClientFromRouter} from '../shared/routerLlmClient';
import {generateOracleAnswer} from '../oracle/oracleService';
import {RequestType, type OracleAskData} from '../types';
import {buildEconomyPayload, stripUndefinedDeep} from './payloadBuilders';
import {normalizeMultilineInput, ORACLE_QUESTION_MAX_LENGTH} from '../../utils/inputNormalization';
import {detectSuspiciousPromptPayload} from '../oracle/promptInputRisk';
import {buildUidTag, safeErrorMessage} from '../../utils/safeLogging';


type SystemMode = 'NORMAL' | 'DEGRADED' | 'EMERGENCY';



function logControlledHttpsError(params: {
  error: HttpsError;
  requestType?: unknown;
  hasQuestion: boolean;
  hasTopic: boolean;
  hasRewardedProof: boolean;
  uid?: string;
  economyV2Enabled?: boolean;
}) {
  console.warn('BWITCH_CALLABLE_ERROR', {
    callable: 'oracleAsk',
    code: params.error.code,
    message: params.error.message,
    requestType: params.requestType ?? null,
    hasQuestion: params.hasQuestion,
    hasTopic: params.hasTopic,
    hasRewardedProof: params.hasRewardedProof,
    uidTag: buildUidTag(params.uid),
    economyV2Enabled: params.economyV2Enabled ?? null,
  });
}

function stripUndefined<T extends Record<string, any>>(obj: T): T {
  return Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== undefined)) as T;
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
  return normalizeMultilineInput(question).slice(0, ORACLE_QUESTION_MAX_LENGTH);
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
      const data = request.data as OracleAskData | undefined;
      const hasQuestion = typeof data?.question === 'string' && data.question.trim().length > 0;
      const hasTopic = data?.topic !== undefined && data.topic !== null;
      const hasRewardedProof = typeof data?.adUnlock?.rewardedProof === 'string' &&
        data.adUnlock.rewardedProof.trim().length > 0;
      const uid = request.auth?.uid;
      let economyV2Enabled: boolean | undefined;
      try {
        if (!uid) throw new HttpsError('unauthenticated', 'Authentication is required');

        if (data?.requestType !== RequestType.ORACLE_1Q) {
          throw new HttpsError('invalid-argument', 'requestType must be ORACLE_1Q');
        }
        if (typeof data?.question !== 'string') {
          throw new HttpsError('invalid-argument', 'question is required');
        }
        if (!data.requestId || typeof data.requestId !== 'string' || data.requestId.trim().length === 0) {
          throw new HttpsError('invalid-argument', 'requestId is required');
        }

        const requestId = data.requestId.trim();
        const lang = normalizeLang(data.lang);
        const question = normalizeQuestion(data.question);
        if (question.length === 0) {
          throw new HttpsError('invalid-argument', 'question is required');
        }
        const questionRisk = detectSuspiciousPromptPayload(question);
        console.info('ORACLE_ASK_INPUT_META', {
          requestId,
          uidTag: buildUidTag(uid),
          questionLength: question.length,
          questionRisk,
          lang,
          hasTopic: Boolean(data.topic),
        });
        const dateIso = dateIsoMadrid();

        economyV2Enabled = await isOracleEconomyV2Enabled();
        const [systemMode, subscriber] = await Promise.all([
          getSystemMode(),
          isSubscriber(uid),
        ]);

        if (systemMode === 'EMERGENCY' && !subscriber) {
          throw new HttpsError('resource-exhausted', 'oracleAsk temporarily unavailable');
        }

        const db = getFirestore();

        const reservation = await reserveOracleEconomyAccess({
          uid,
          requestId,
          dateIso,
          lang,
          question,
        });


        const economyDecisionSource =
        reservation.type === 'reserved' &&
        'source' in reservation ?
        reservation.source :
        undefined;
        const economyMoonCost =
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

        const {buildRouter} = await import('../../llm/buildRouter.js');
        const router = buildRouter();

        let usageDateIso: string;
        try {
          const {dateIso: reservedDateIso} = await reserveLlmCallOrThrow('oracle', llmDailyCaps());
          usageDateIso = reservedDateIso;
        } catch (error) {
          const errorMessage = safeErrorMessage(error);

          if (errorMessage.includes('DAILY_LLM_CAP_EXCEEDED')) {
            await refundOracleEconomyRequest({
              uid,
              requestId,
              dateIso,
              errorMessage: 'DAILY_LLM_CAP_EXCEEDED',
            });
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

          const responsePayload = stripUndefinedDeep({
            requestId,
            status: 'COMPLETED_SUCCESS',
            answer: generated.answer,
            systemMode,
            economy: buildEconomyPayload(
                true,
                economyDecisionSource,
                economyMoonCost
            ),
          });

          const answerRef = oracleSubRef('oracleAnswers', uid, 'items', requestId);

          await db.runTransaction(async (tx) => {
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

          await completeOracleEconomyRequest({
            uid,
            requestId,
            responsePayload,
            llmMeta,
          });

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

          return responsePayload;
        } catch (error) {
          const errorMessage = safeErrorMessage(error);

          await refundOracleEconomyRequest({
            uid,
            requestId,
            dateIso,
            errorMessage,
          });

          await updateProviderUsage({
            dateIso: usageDateIso,
            provider: router.name,
            success: false,
          });

          throw new HttpsError('internal', 'Failed to generate oracle answer');
        }
      } catch (error) {
        if (error instanceof HttpsError) {
          logControlledHttpsError({
            error,
            requestType: data?.requestType,
            hasQuestion,
            hasTopic,
            hasRewardedProof,
            uid,
            economyV2Enabled,
          });
        }
        throw error;
      }
    }
);
