import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {buildRouter} from '../../llm/buildRouter';
import {generateTarotReading} from '../tarot/tarotService';
import {ConsumeIntent, RequestType, type TarotDrawData} from '../types';
import {createLlmClientFromRouter} from '../shared/routerLlmClient';
import {dateIsoMadrid, oracleRef, oracleSubRef} from '../firestore/paths';

const ALLOWED_TAROT_TYPES = new Set<RequestType>([
  RequestType.TAROT_1,
  RequestType.TAROT_3,
]);

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
  requestType: RequestType.TAROT_1 | RequestType.TAROT_3;
  lang: string;
  topic?: string;
  question?: string;
  dateIso: string;
  intent?: ConsumeIntent;
  status: 'PROCESSING' | 'FAILED' | 'COMPLETED_SUCCESS';
  systemMode: SystemMode;
  readingId?: string;
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

function normalizeLang(lang?: string): string {
  if (!lang) return 'es';
  return lang.trim().toLowerCase().startsWith('en') ? 'en' : 'es';
}

function normalizeQuestion(question?: string): string | undefined {
  if (!question) return undefined;
  const trimmed = question.trim();
  if (!trimmed) return undefined;
  return trimmed.slice(0, 400);
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

export const tarotDraw = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: true,
    },
    async (request) => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError('unauthenticated', 'Authentication is required');
      }

      const data = request.data as TarotDrawData;
      if (!data?.requestType || !ALLOWED_TAROT_TYPES.has(data.requestType)) {
        throw new HttpsError(
            'invalid-argument',
            'requestType must be TAROT_1 or TAROT_3'
        );
      }

      if (!data.requestId || typeof data.requestId !== 'string' || data.requestId.trim().length === 0) {
        throw new HttpsError('invalid-argument', 'requestId is required');
      }

      const requestId = data.requestId.trim();
      const lang = normalizeLang(data.lang);
      const question = normalizeQuestion(data.question);
      const dateIso = dateIsoMadrid();

      const [systemMode, subscriber] = await Promise.all([
        getSystemMode(),
        isSubscriber(uid),
      ]);

      if (systemMode === 'EMERGENCY' && !subscriber) {
        throw new HttpsError('resource-exhausted', 'tarotDraw temporarily unavailable');
      }
      if (systemMode === 'DEGRADED' && data.requestType === RequestType.TAROT_3) {
        throw new HttpsError('resource-exhausted', 'TAROT_3 unavailable while system is DEGRADED');
      }

      const db = getFirestore();
      const requestRef = oracleRef('oracleRequests', requestId);
      const userDailyRef = oracleSubRef('oracleUserDaily', dateIso, 'users', uid);

      const reservation = await db.runTransaction(async (tx) => {
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
            throw new HttpsError('aborted', 'requestId already failed; retry with a new requestId');
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

        if (data.requestType === RequestType.TAROT_3 && nextDaily.tarot3Remaining <= 0) {
          throw new HttpsError('resource-exhausted', 'Daily TAROT_3 quota exhausted');
        }

        let intent: ConsumeIntent;

        if (subscriber) {
          intent = ConsumeIntent.SUBSCRIPTION;
        } else if (data.requestType === RequestType.TAROT_1 && nextDaily.freeTarot1Remaining > 0) {
          intent = ConsumeIntent.FREE_DAILY;
          nextDaily.freeTarot1Remaining -= 1;
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
        if (data.requestType === RequestType.TAROT_3) {
          nextDaily.tarot3Remaining -= 1;
        }

        tx.set(userDailyRef, nextDaily, {merge: true});
        tx.create(requestRef, {
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
        } satisfies RequestDoc);

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

      if (reservation.type === 'completed') {
        return reservation.payload;
      }

      if (reservation.type === 'in-progress') {
        return {requestId, status: 'IN_PROGRESS'};
      }

      const router = buildRouter();
      const llmClient = createLlmClientFromRouter(router, 'tarot');

      try {
        const generated = await generateTarotReading({
          requestId,
          requestType: data.requestType,
          lang,
          question,
          llm: llmClient,
        });

        const readingId = requestId;
        const responsePayload = {
          requestId,
          status: 'COMPLETED_SUCCESS',
          reading: generated.reading,
          draw: generated.draw,
          quotaSnapshot: reservation.quotaSnapshot,
          systemMode,
        };

        const drawForHistory = generated.draw.type === RequestType.TAROT_1 ?
          [{
            id: generated.draw.card.id,
            orientation: generated.draw.card.orientation,
            position: 'present',
          }] :
          generated.draw.cards.map((card) => ({
            id: card.id,
            orientation: card.orientation,
            position: card.position,
          }));

        const readingRef = oracleSubRef('tarotReadings', uid, 'items', readingId);
        await db.runTransaction(async (tx) => {
          tx.set(requestRef, {
            status: 'COMPLETED_SUCCESS',
            readingId,
            responsePayload,
            llmMeta: generated.llmMeta,
            updatedAt: FieldValue.serverTimestamp(),
          }, {merge: true});

          tx.set(readingRef, {
            requestId,
            requestType: data.requestType,
            lang,
            question,
            draw: drawForHistory,
            reading: generated.reading,
            createdAt: FieldValue.serverTimestamp(),
            llmMeta: generated.llmMeta,
          }, {merge: true});
        });

        await updateProviderUsage({
          dateIso,
          provider: generated.llmMeta.provider,
          success: true,
          inputTokens: generated.llmMeta.inputTokens,
          outputTokens: generated.llmMeta.outputTokens,
          costUsd: generated.llmMeta.costUsd,
          durationMs: generated.llmMeta.durationMs,
        });

        return responsePayload;
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : String(error);

        await requestRef.set({
          status: 'FAILED',
          error: {
            message: errorMessage,
          },
          updatedAt: FieldValue.serverTimestamp(),
        }, {merge: true});

        await updateProviderUsage({
          dateIso,
          provider: router.name,
          success: false,
        });

        throw new HttpsError('internal', 'Failed to generate tarot reading');
      }
    }
);
