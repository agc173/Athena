import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {HttpsError, onCall} from 'firebase-functions/v2/https';
import {TAROT_DECK} from '../../oracle/tarot/deck';

type SeedTarotDeckProgressionInput = {
  dryRun?: boolean;
};

const TRACK_ID = 'arcana_noctis';
const TRACKS_COLLECTION = 'tarotDeckTracks';
const REWARD_POOLS_COLLECTION = 'tarotDeckRewardPools';

function allowUnverifiedAppCheckInDev(): boolean {
  const raw = process.env.ALLOW_UNVERIFIED_APPCHECK_IN_DEV;
  if (raw == null || raw.trim() === '') return false;
  const normalized = raw.trim().toLowerCase();
  return normalized === '1' || normalized === 'true';
}

function assertAdminSeedAccess(params: {uid?: string; email?: string | null; allowedUids: string[]; allowedEmails: string[]; enabled: boolean}) {
  if (!params.uid) {
    throw new HttpsError('unauthenticated', 'Authentication is required');
  }
  if (!params.enabled) {
    throw new HttpsError('permission-denied', 'Seed callable disabled');
  }

  const email = (params.email ?? '').trim().toLowerCase();
  const uidAllowed = params.allowedUids.includes(params.uid);
  const emailAllowed = email.length > 0 && params.allowedEmails.includes(email);

  if (!uidAllowed && !emailAllowed) {
    throw new HttpsError('permission-denied', 'Admin privileges required');
  }
}

export const seedTarotDeckProgressionConfig = onCall(
    {
      region: 'europe-west1',
      enforceAppCheck: !allowUnverifiedAppCheckInDev(),
    },
    async (request) => {
      const {ENV} = await import('../../config/env.js');
      assertAdminSeedAccess({
        uid: request.auth?.uid,
        email: request.auth?.token?.email,
        allowedUids: ENV.SEED_ADMIN_UIDS,
        allowedEmails: ENV.SEED_ADMIN_EMAILS,
        enabled: ENV.ENABLE_TAROT_DECK_SEED_CALLABLE,
      });

      const input = (request.data ?? {}) as SeedTarotDeckProgressionInput;
      const dryRun = input?.dryRun === true;
      const cardIds = TAROT_DECK.map((card) => card.id);

      if (cardIds.length !== 78) {
        throw new HttpsError('failed-precondition', `Expected 78 tarot card ids, got ${cardIds.length}`);
      }

      const db = getFirestore();
      const trackRef = db.collection(TRACKS_COLLECTION).doc(TRACK_ID);
      const rewardPoolRef = db.collection(REWARD_POOLS_COLLECTION).doc(TRACK_ID);

      if (dryRun) {
        return {
          ok: true,
          dryRun: true,
          trackPath: `${TRACKS_COLLECTION}/${TRACK_ID}`,
          rewardPoolPath: `${REWARD_POOLS_COLLECTION}/${TRACK_ID}`,
          cardsCount: cardIds.length,
          enabled: ENV.ENABLE_TAROT_DECK_SEED_CALLABLE,
        };
      }

      await db.runTransaction(async (tx) => {
        const [trackSnap, rewardPoolSnap] = await Promise.all([
          tx.get(trackRef),
          tx.get(rewardPoolRef),
        ]);

        const now = FieldValue.serverTimestamp();

        tx.set(trackRef, {
          enabled: true,
          moonsPerUnlock: 5,
          rewardType: 'TAROT_CARD',
          rewardPoolId: TRACK_ID,
          createdAt: trackSnap.exists ? trackSnap.get('createdAt') ?? now : now,
          updatedAt: now,
        }, {merge: true});

        tx.set(rewardPoolRef, {
          deckId: TRACK_ID,
          enabled: true,
          totalCards: 78,
          cardIds,
          createdAt: rewardPoolSnap.exists ? rewardPoolSnap.get('createdAt') ?? now : now,
          updatedAt: now,
        }, {merge: true});
      });

      return {
        ok: true,
        dryRun: false,
        trackPath: `${TRACKS_COLLECTION}/${TRACK_ID}`,
        rewardPoolPath: `${REWARD_POOLS_COLLECTION}/${TRACK_ID}`,
        cardsCount: cardIds.length,
      };
    }
);
