import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {onSchedule} from 'firebase-functions/v2/scheduler';
import {dateIsoMadrid} from './firestorePaths';
import {refundBirthEssenceEconomyRequest} from './birthEssenceEconomy';
import {refundOracleEconomyRequest} from './oracleEconomy';
import {refundTarotEconomyRequest} from './tarotEconomy';
import type {EconomyRequestDoc, EconomyRequestType} from './types';

const PROCESSING_TIMEOUT_MINUTES = 15;
const MAX_REQUESTS_PER_RUN = 200;
const COVERED_TYPES = new Set<EconomyRequestType>([
  'ORACLE_1Q',
  'TAROT_1',
  'TAROT_3',
  'BIRTH_ESSENCE',
]);

type WatchdogCounters = {
  scanned: number;
  refunded: number;
  skipped: number;
  failed: number;
};

function uidFromRequestPath(path: string): string | null {
  const segments = path.split('/');
  const economyRequestsIndex = segments.indexOf('economyRequests');
  if (economyRequestsIndex < 0) return null;
  return segments[economyRequestsIndex + 1] ?? null;
}

async function refundTimedOutRequest(params: {
  uid: string;
  request: EconomyRequestDoc;
  fallbackDateIso: string;
}) {
  const errorMessage = 'ECONOMY_REQUEST_PROCESSING_TIMEOUT';
  const dateIso = params.request.dateIso ?? params.fallbackDateIso;

  switch (params.request.type) {
    case 'ORACLE_1Q':
      return refundOracleEconomyRequest({
        uid: params.uid,
        requestId: params.request.requestId,
        dateIso,
        errorMessage,
        recoveredResult: 'FAILED_TIMEOUT',
      });
    case 'TAROT_1':
    case 'TAROT_3':
      return refundTarotEconomyRequest({
        uid: params.uid,
        requestId: params.request.requestId,
        dateIso,
        errorMessage,
        recoveredResult: 'FAILED_TIMEOUT',
      });
    case 'BIRTH_ESSENCE':
      return refundBirthEssenceEconomyRequest({
        uid: params.uid,
        requestId: params.request.requestId,
        dateIso,
        errorMessage,
        recoveredResult: 'FAILED_TIMEOUT',
      });
    default:
      return;
  }
}

export async function recoverTimedOutEconomyRequests(now = new Date()): Promise<WatchdogCounters> {
  const db = getFirestore();
  const cutoff = Timestamp.fromMillis(now.getTime() - PROCESSING_TIMEOUT_MINUTES * 60 * 1000);
  const fallbackDateIso = dateIsoMadrid(now);
  const counters: WatchdogCounters = {scanned: 0, refunded: 0, skipped: 0, failed: 0};

  const snapshot = await db
      .collectionGroup('requests')
      .where('status', '==', 'PROCESSING')
      .where('updatedAt', '<=', cutoff)
      .limit(MAX_REQUESTS_PER_RUN)
      .get();

  for (const doc of snapshot.docs) {
    counters.scanned++;
    const request = doc.data() as EconomyRequestDoc;
    const uid = uidFromRequestPath(doc.ref.path);

    if (!uid || !COVERED_TYPES.has(request.type)) {
      counters.skipped++;
      continue;
    }

    try {
      const refunded = await refundTimedOutRequest({uid, request, fallbackDateIso});
      if (refunded) {
        counters.refunded++;
      } else {
        counters.skipped++;
      }
    } catch (error) {
      counters.failed++;
      console.error('ECONOMY_PROCESSING_WATCHDOG_REFUND_FAILED', {
        uidTag: uid.slice(0, 8),
        requestId: request.requestId,
        type: request.type,
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }

  console.info('ECONOMY_PROCESSING_WATCHDOG_SUMMARY', counters);
  return counters;
}

export const recoverTimedOutEconomyRequestsScheduled = onSchedule(
    {
      schedule: 'every 10 minutes',
      timeZone: 'Europe/Madrid',
      region: 'europe-west1',
      retryCount: 0,
      timeoutSeconds: 300,
    },
    async () => {
      await recoverTimedOutEconomyRequests();
    }
);
