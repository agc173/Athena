import {initializeApp, applicationDefault, getApps} from 'firebase-admin/app';
import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {TAROT_DECK} from '../src/oracle/tarot/deck';

const TRACK_ID = 'arcana_noctis';
const TRACKS_COLLECTION = 'tarotDeckTracks';
const REWARD_POOLS_COLLECTION = 'tarotDeckRewardPools';

function ensureAppInitialized() {
  if (getApps().length === 0) {
    initializeApp({credential: applicationDefault()});
  }
}

async function seedTarotDeckProgression() {
  ensureAppInitialized();

  const db = getFirestore();
  const now = FieldValue.serverTimestamp();
  const cardIds = TAROT_DECK.map((card) => card.id);

  if (cardIds.length !== 78) {
    throw new Error(`Expected 78 tarot card ids, got ${cardIds.length}. Aborting seed.`);
  }

  const trackRef = db.collection(TRACKS_COLLECTION).doc(TRACK_ID);
  const rewardPoolRef = db.collection(REWARD_POOLS_COLLECTION).doc(TRACK_ID);

  await db.runTransaction(async (tx) => {
    const [trackSnap, rewardPoolSnap] = await Promise.all([
      tx.get(trackRef),
      tx.get(rewardPoolRef),
    ]);

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

  console.log('✅ tarot deck progression seed applied for arcana_noctis');
  console.log(`- ${TRACKS_COLLECTION}/${TRACK_ID}`);
  console.log(`- ${REWARD_POOLS_COLLECTION}/${TRACK_ID}`);
  console.log(`- cards seeded: ${cardIds.length}`);
}

seedTarotDeckProgression().catch((error) => {
  console.error('❌ seed failed', error);
  process.exitCode = 1;
});
