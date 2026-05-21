import {createHash} from 'node:crypto';
import {type Timestamp, type Transaction, getFirestore} from 'firebase-admin/firestore';
import {buildUidTag} from '../utils/safeLogging';

type TrackDoc = {
  enabled?: boolean;
  moonsPerUnlock?: unknown;
  rewardType?: string;
  rewardPoolId?: string;
};

type RewardPoolDoc = {
  deckId?: string;
  cardIds?: unknown;
  totalCards?: unknown;
  enabled?: boolean;
};

type ProgressDoc = {
  totalMoonSpend?: number;
  carryOverMoons?: number;
  unlocksGranted?: number;
  unlockedCardCount?: number;
  completedAt?: Timestamp;
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
    unlockedCardCount: number;
    deckTotalCards: number;
    shouldMarkCompletedAt: boolean;
    cardsToUnlock: Array<{
      cardId: string;
      deckId: string;
      rewardPoolId: string;
      trackId: string;
      grantRequestId: string;
      source: 'moon_spend_progress';
    }>;
  }>;
};

function userTarotDeckProgressRequestRef(uid: string, requestId: string) {
  return getFirestore().doc(`users/${uid}/tarotDeckProgressRequests/${requestId}`);
}

function userTarotDeckProgressRef(uid: string, trackId: string) {
  return getFirestore().doc(`users/${uid}/tarotDeckProgress/${trackId}`);
}

function unlockedCardsCollectionRef(uid: string, trackId: string) {
  return getFirestore().collection(`users/${uid}/tarotDeckProgress/${trackId}/unlockedCards`);
}

function normalizeCardIds(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.filter((cardId): cardId is string => typeof cardId === 'string' && cardId.trim().length > 0);
}

function stableHashInt(seed: string): number {
  const digest = createHash('sha256').update(seed).digest();
  return digest.readUInt32BE(0);
}

export function pickCardsDeterministically(params: {
  availableCardIds: string[];
  picks: number;
  seed: string;
}): string[] {
  if (params.picks <= 0 || params.availableCardIds.length === 0) return [];
  const ordered = [...params.availableCardIds].sort((a, b) => {
    const scoreA = stableHashInt(`${params.seed}:${a}`);
    const scoreB = stableHashInt(`${params.seed}:${b}`);
    if (scoreA !== scoreB) return scoreA - scoreB;
    return a.localeCompare(b);
  });
  return ordered.slice(0, Math.min(params.picks, ordered.length));
}


export function computeUnlockDelta(params: {carryOverMoons: number; moonCostCharged: number; moonsPerUnlock: number}): {newlyGrantedUnlocks: number; carryOverMoons: number} {
  const carryBase = Math.max(0, Math.floor(params.carryOverMoons)) + Math.max(0, Math.floor(params.moonCostCharged));
  const step = Math.floor(params.moonsPerUnlock);
  if (!Number.isFinite(step) || step < 1) {
    return {
      newlyGrantedUnlocks: 0,
      carryOverMoons: carryBase,
    };
  }
  return {
    newlyGrantedUnlocks: Math.floor(carryBase / step),
    carryOverMoons: carryBase % step,
  };
}

export function shouldMarkCompletedAt(params: {
  alreadyCompleted: boolean;
  unlockedCardCount: number;
  totalCards: number;
}): boolean {
  if (params.alreadyCompleted) return false;
  return params.totalCards > 0 && params.unlockedCardCount >= params.totalCards;
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
    return {shouldApply: false, alreadyApplied: false, requestRef, requestId: params.requestId, moonCostCharged: params.moonCostCharged, source: params.source, tracksEvaluated: 0, updates: []};
  }

  const requestSnap = await params.tx.get(requestRef);
  if (requestSnap.exists) {
    return {shouldApply: false, alreadyApplied: true, requestRef, requestId: params.requestId, moonCostCharged: params.moonCostCharged, source: params.source, tracksEvaluated: 0, updates: []};
  }

  const tracksQuery = await params.tx.get(getFirestore().collection('tarotDeckTracks').where('enabled', '==', true));
  const updates: DeckProgressPlan['updates'] = [];

  for (const trackSnap of tracksQuery.docs) {
    const trackId = trackSnap.id;
    const track = (trackSnap.data() as TrackDoc | undefined) ?? {};
    const rawMoonsPerUnlock = track.moonsPerUnlock;
    const moonsPerUnlock = typeof rawMoonsPerUnlock === 'number' ? Math.floor(rawMoonsPerUnlock) : NaN;
    if (!Number.isFinite(moonsPerUnlock) || moonsPerUnlock < 1) {
      console.warn('DECK_TRACK_CONFIG_INVALID', {trackId, uidTag: buildUidTag(params.uid), requestId: params.requestId});
      continue;
    }

    const progressRef = userTarotDeckProgressRef(params.uid, trackId);
    const progressSnap = await params.tx.get(progressRef);
    const progress = (progressSnap.data() as ProgressDoc | undefined) ?? {};
    const currentTotal = Math.max(0, Math.floor(progress.totalMoonSpend ?? 0));
    const currentCarry = Math.max(0, Math.floor(progress.carryOverMoons ?? 0));
    const currentUnlocks = Math.max(0, Math.floor(progress.unlocksGranted ?? 0));
    const currentUnlockedCardCount = Math.max(0, Math.floor(progress.unlockedCardCount ?? 0));

    const unlockDelta = computeUnlockDelta({carryOverMoons: currentCarry, moonCostCharged: params.moonCostCharged, moonsPerUnlock});
    const newlyGrantedUnlocks = unlockDelta.newlyGrantedUnlocks;
    const nextCarry = unlockDelta.carryOverMoons;

    let cardsToUnlock: DeckProgressPlan['updates'][number]['cardsToUnlock'] = [];
    let unlockedCardCount = currentUnlockedCardCount;
    let deckTotalCards = currentUnlockedCardCount;
    let shouldSetCompletedAt = false;
    const alreadyCompleted = !!progress.completedAt;

    if (track.rewardType === 'TAROT_CARD') {
      if (!track.rewardPoolId) {
        console.warn('DECK_REWARD_POOL_INVALID', {trackId, reason: 'missing_rewardPoolId', uidTag: buildUidTag(params.uid), requestId: params.requestId});
      } else {
        const rewardPoolSnap = await params.tx.get(getFirestore().doc(`tarotDeckRewardPools/${track.rewardPoolId}`));
        const rewardPool = rewardPoolSnap.data() as RewardPoolDoc | undefined;
        const rewardPoolEnabled = rewardPool?.enabled === true;
        const cardIds = normalizeCardIds(rewardPool?.cardIds);
        const totalCardsRaw = typeof rewardPool?.totalCards === 'number' ? Math.floor(rewardPool.totalCards) : NaN;
        const totalCards = Number.isFinite(totalCardsRaw) && totalCardsRaw > 0 ? totalCardsRaw : cardIds.length;

        if (!rewardPoolSnap.exists || !rewardPoolEnabled || cardIds.length === 0 || !rewardPool?.deckId) {
          console.warn('DECK_REWARD_POOL_INVALID', {
            trackId,
            rewardPoolId: track.rewardPoolId,
            reason: !rewardPoolSnap.exists ? 'missing_pool' : !rewardPoolEnabled ? 'pool_disabled' : cardIds.length === 0 ? 'empty_card_ids' : 'missing_deck_id',
            uidTag: buildUidTag(params.uid),
            requestId: params.requestId,
          });
        } else {
          if (totalCards !== cardIds.length) {
            console.warn('DECK_REWARD_POOL_INVALID', {
              trackId,
              rewardPoolId: track.rewardPoolId,
              reason: 'totalCards_mismatch_uses_cardIds_length',
              configuredTotalCards: totalCardsRaw,
              sourceOfTruthTotalCards: cardIds.length,
            });
          }
          deckTotalCards = cardIds.length;
          const unlockedCardsSnap = await params.tx.get(unlockedCardsCollectionRef(params.uid, trackId));
          const unlockedSet = new Set(unlockedCardsSnap.docs.map((doc) => doc.id));
          const available = cardIds.filter((cardId) => !unlockedSet.has(cardId));
          const requestedUnlocks = Math.max(0, newlyGrantedUnlocks);
          const selectedCardIds = pickCardsDeterministically({availableCardIds: available, picks: requestedUnlocks, seed: `${params.uid}:${params.requestId}:${trackId}:${requestedUnlocks}`});

          cardsToUnlock = selectedCardIds.map((cardId) => ({
            cardId,
            deckId: rewardPool.deckId as string,
            rewardPoolId: track.rewardPoolId as string,
            trackId,
            grantRequestId: params.requestId,
            source: 'moon_spend_progress',
          }));

          unlockedCardCount = Math.min(cardIds.length, unlockedSet.size + cardsToUnlock.length);
          shouldSetCompletedAt = shouldMarkCompletedAt({alreadyCompleted, unlockedCardCount, totalCards: cardIds.length});
        }
      }
    }

    updates.push({
      trackId,
      progressRef,
      totalMoonSpend: currentTotal + params.moonCostCharged,
      carryOverMoons: nextCarry,
      unlocksGranted: currentUnlocks + newlyGrantedUnlocks,
      newlyGrantedUnlocks,
      rewardType: track.rewardType ?? null,
      rewardPoolId: track.rewardPoolId ?? null,
      unlockedCardCount,
      deckTotalCards,
      shouldMarkCompletedAt: shouldSetCompletedAt,
      cardsToUnlock,
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
      unlockedCardCount: update.unlockedCardCount,
      ...(update.shouldMarkCompletedAt ? {completedAt: now} : {}),
      updatedAt: now,
    }, {merge: true});

    const unlockedCardsRef = unlockedCardsCollectionRef(params.uid, update.trackId);
    for (const card of update.cardsToUnlock) {
      tx.create(unlockedCardsRef.doc(card.cardId), {
        cardId: card.cardId,
        deckId: card.deckId,
        rewardPoolId: card.rewardPoolId,
        trackId: card.trackId,
        grantRequestId: card.grantRequestId,
        unlockedAt: now,
        source: card.source,
      });
      console.info('DECK_CARD_UNLOCKED', {uidTag: buildUidTag(params.uid), requestId: plan.requestId, trackId: update.trackId, rewardPoolId: card.rewardPoolId, cardId: card.cardId});
    }

    totalUnlocksGranted += update.newlyGrantedUnlocks;
    if (update.newlyGrantedUnlocks > 0) {
      console.info('DECK_UNLOCK_GRANTED', {uidTag: buildUidTag(params.uid), requestId: plan.requestId, trackId: update.trackId, unlocksGranted: update.newlyGrantedUnlocks, rewardType: update.rewardType, rewardPoolId: update.rewardPoolId});
    }

    if (update.shouldMarkCompletedAt) {
      console.info('DECK_COMPLETED', {
        uidTag: buildUidTag(params.uid),
        requestId: plan.requestId,
        trackId: update.trackId,
        rewardPoolId: update.rewardPoolId,
        unlockedCardCount: update.unlockedCardCount,
        totalCards: update.deckTotalCards,
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
