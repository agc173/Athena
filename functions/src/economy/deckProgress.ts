import {type Timestamp, type Transaction, getFirestore} from 'firebase-admin/firestore';
import {buildUidTag} from '../utils/safeLogging';

type TrackDoc = {
  enabled?: boolean;
  moonsPerUnlock?: unknown;
  rewardType?: string;
  rewardPoolId?: string;
};

type ProgressDoc = {
  totalMoonSpend?: number;
  carryOverMoons?: number;
  unlocksGranted?: number;
};

type Source = 'MOON' | 'FREE' | 'PREMIUM_INCLUDED' | 'REJECT';

export type DeckProgressPlan = {
  shouldApply: boolean;
  alreadyApplied: boolean;
  requestRef: FirebaseFirestore.DocumentReference;
  requestId: string;
  moonCostCharged: number;
  source: Source;
  tracksEvaluated: number;
  updates: Array<{
    trackId: string;
    progressRef: FirebaseFirestore.DocumentReference;
    totalMoonSpend: number;
    carryOverMoons: number;
    unlocksGranted: number;
    newlyGrantedUnlocks: number;
    rewardType: string | null;
    rewardPoolId: string | null;
  }>;
};

function userTarotDeckProgressRequestRef(uid: string, requestId: string) {
  return getFirestore().doc(`users/${uid}/tarotDeckProgressRequests/${requestId}`);
}

function userTarotDeckProgressRef(uid: string, trackId: string) {
  return getFirestore().doc(`users/${uid}/tarotDeckProgress/${trackId}`);
}

export async function planDeckProgressFromMoonSpend(params: {
  tx: Transaction;
  uid: string;
  requestId: string;
  moonCostCharged: number;
  source: Source;
}): Promise<DeckProgressPlan> {
  const requestRef = userTarotDeckProgressRequestRef(params.uid, params.requestId);
  if (params.source !== 'MOON' || params.moonCostCharged <= 0) {
    return {
      shouldApply: false,
      alreadyApplied: false,
      requestRef,
      requestId: params.requestId,
      moonCostCharged: params.moonCostCharged,
      source: params.source,
      tracksEvaluated: 0,
      updates: [],
    };
  }

  const requestSnap = await params.tx.get(requestRef);
  if (requestSnap.exists) {
    return {
      shouldApply: false,
      alreadyApplied: true,
      requestRef,
      requestId: params.requestId,
      moonCostCharged: params.moonCostCharged,
      source: params.source,
      tracksEvaluated: 0,
      updates: [],
    };
  }

  const tracksQuery = await params.tx.get(
      getFirestore().collection('tarotDeckTracks').where('enabled', '==', true),
  );
  const updates: DeckProgressPlan['updates'] = [];
  for (const trackSnap of tracksQuery.docs) {
    const trackId = trackSnap.id;
    const track = (trackSnap.data() as TrackDoc | undefined) ?? {};
    const rawMoonsPerUnlock = track.moonsPerUnlock;
    const moonsPerUnlock = typeof rawMoonsPerUnlock === 'number' ? Math.floor(rawMoonsPerUnlock) : NaN;
    if (!Number.isFinite(moonsPerUnlock) || moonsPerUnlock < 1) {
      console.warn('DECK_TRACK_CONFIG_INVALID', {
        trackId,
        uidTag: buildUidTag(params.uid),
        requestId: params.requestId,
      });
      continue;
    }

    const progressRef = userTarotDeckProgressRef(params.uid, trackId);
    const progressSnap = await params.tx.get(progressRef);
    const progress = (progressSnap.data() as ProgressDoc | undefined) ?? {};

    const currentTotal = Math.max(0, Math.floor(progress.totalMoonSpend ?? 0));
    const currentCarry = Math.max(0, Math.floor(progress.carryOverMoons ?? 0));
    const currentUnlocks = Math.max(0, Math.floor(progress.unlocksGranted ?? 0));

    const carryBase = currentCarry + params.moonCostCharged;
    const newlyGrantedUnlocks = Math.floor(carryBase / moonsPerUnlock);
    const nextCarry = carryBase % moonsPerUnlock;

    updates.push({
      trackId,
      progressRef,
      totalMoonSpend: currentTotal + params.moonCostCharged,
      carryOverMoons: nextCarry,
      unlocksGranted: currentUnlocks + newlyGrantedUnlocks,
      newlyGrantedUnlocks,
      rewardType: track.rewardType ?? null,
      rewardPoolId: track.rewardPoolId ?? null,
    });
  }

  return {
    shouldApply: true,
    alreadyApplied: false,
    requestRef,
    requestId: params.requestId,
    moonCostCharged: params.moonCostCharged,
    source: params.source,
    tracksEvaluated: tracksQuery.size,
    updates,
  };
}

export function applyDeckProgressPlan(params: {
  tx: Transaction;
  uid: string;
  now: Timestamp;
  plan: DeckProgressPlan;
}): void {
  const {tx, now, plan} = params;
  if (!plan.shouldApply || plan.alreadyApplied) return;

  let totalUnlocksGranted = 0;
  for (const update of plan.updates) {
    tx.set(update.progressRef, {
      totalMoonSpend: update.totalMoonSpend,
      carryOverMoons: update.carryOverMoons,
      unlocksGranted: update.unlocksGranted,
      updatedAt: now,
    }, {merge: true});

    totalUnlocksGranted += update.newlyGrantedUnlocks;
    if (update.newlyGrantedUnlocks > 0) {
      console.info('DECK_UNLOCK_GRANTED', {
        uidTag: buildUidTag(params.uid),
        requestId: plan.requestId,
        trackId: update.trackId,
        unlocksGranted: update.newlyGrantedUnlocks,
        rewardType: update.rewardType,
        rewardPoolId: update.rewardPoolId,
      });
    }
  }

  tx.create(plan.requestRef, {
    applied: true,
    moonCostCharged: plan.moonCostCharged,
    source: plan.source,
    createdAt: now,
  });

  console.info('DECK_PROGRESS_APPLIED', {
    uidTag: buildUidTag(params.uid),
    requestId: plan.requestId,
    moonCostCharged: plan.moonCostCharged,
    unlocksGranted: totalUnlocksGranted,
    tracksEvaluated: plan.tracksEvaluated,
  });
}
