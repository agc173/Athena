import {applicationDefault, getApps, initializeApp} from 'firebase-admin/app';
import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import {TAROT_DECK_CARD_IDS} from '../src/economy/tarotDeckCardIds';

const TRACK_ID = 'arcana_noctis';
const DECK_ID = 'arcana_noctis';
const REWARD_POOL_ID = 'arcana_noctis';
const EXPECTED_CARD_COUNT = 78;
const UNLOCK_SOURCE = 'moon_spend_progress';

type CliArgs = Record<string, string[]>;

function parseCliArgs(argv: string[]): CliArgs {
  const args: CliArgs = {};
  for (let i = 0; i < argv.length; i++) {
    const token = argv[i];
    if (!token.startsWith('--')) {
      throw new Error(`Unexpected argument: ${token}`);
    }

    const key = token.slice(2);
    if (key === 'commit') {
      args[key] = ['true'];
      continue;
    }

    const value = argv[++i];
    if (!key || value == null || value.startsWith('--')) {
      throw new Error(`Missing value for --${key}`);
    }
    args[key] = [...(args[key] ?? []), value];
  }
  return args;
}

function requiredArg(args: CliArgs, key: string): string {
  const value = args[key]?.at(-1)?.trim();
  if (!value) throw new Error(`Missing required argument --${key}`);
  return value;
}

function initializeAdminApp() {
  if (getApps().length) return;

  const projectId = process.env.FIREBASE_PROJECT_ID ?? process.env.GCLOUD_PROJECT;
  initializeApp({
    credential: applicationDefault(),
    ...(projectId ? {projectId} : {}),
  });
}

function arcanaNoctisCardIds(): string[] {
  const cardIds = [...TAROT_DECK_CARD_IDS].sort();
  const uniqueCardIds = new Set(cardIds);
  if (cardIds.length !== EXPECTED_CARD_COUNT || uniqueCardIds.size !== EXPECTED_CARD_COUNT) {
    throw new Error(`Expected ${EXPECTED_CARD_COUNT} unique Arcana Noctis cards, got ${cardIds.length} entries and ${uniqueCardIds.size} unique ids.`);
  }
  return cardIds;
}

async function unlockArcanaNoctisForUid(uid: string, commit: boolean) {
  initializeAdminApp();

  const db = getFirestore();
  const now = Timestamp.now();
  const cardIds = arcanaNoctisCardIds();
  const progressRef = db.doc(`users/${uid}/tarotDeckProgress/${TRACK_ID}`);
  const unlockedCardsRef = progressRef.collection('unlockedCards');
  const grantRequestId = `admin_unlock_arcana_noctis_${now.toMillis()}`;

  const summary = await db.runTransaction(async (tx) => {
    const progressSnap = await tx.get(progressRef);
    const unlockedSnap = await tx.get(unlockedCardsRef);
    const existingCardIds = new Set(unlockedSnap.docs.map((doc) => doc.id));
    const missingCardIds = cardIds.filter((cardId) => !existingCardIds.has(cardId));

    const existingProgress = progressSnap.data() ?? {};
    const progressUpdate: Record<string, unknown> = {
      totalMoonSpend: existingProgress.totalMoonSpend ?? 0,
      carryOverMoons: existingProgress.carryOverMoons ?? 0,
      unlocksGranted: existingProgress.unlocksGranted ?? existingCardIds.size + missingCardIds.length,
      unlockedCardCount: cardIds.length,
      updatedAt: now,
    };
    if (!existingProgress.completedAt) {
      progressUpdate.completedAt = now;
    }

    if (commit) {
      tx.set(progressRef, progressUpdate, {merge: true});
      for (const cardId of missingCardIds) {
        tx.create(unlockedCardsRef.doc(cardId), {
          cardId,
          deckId: DECK_ID,
          rewardPoolId: REWARD_POOL_ID,
          trackId: TRACK_ID,
          grantRequestId,
          unlockedAt: now,
          source: UNLOCK_SOURCE,
        });
      }
    }

    return {
      uid,
      progressPath: progressRef.path,
      unlockedCardsPath: unlockedCardsRef.path,
      trackId: TRACK_ID,
      deckId: DECK_ID,
      rewardPoolId: REWARD_POOL_ID,
      expectedCardCount: cardIds.length,
      existingUnlockedCardDocs: existingCardIds.size,
      missingCardDocsToCreate: missingCardIds.length,
      grantRequestId,
      committed: commit,
      progressUpdate,
      cardIds,
    };
  });

  console.log(JSON.stringify(summary, null, 2));
  if (!commit) {
    console.log('Dry run only. Re-run with --commit to write Firestore.');
  }
}

async function main() {
  const args = parseCliArgs(process.argv.slice(2));
  const uid = requiredArg(args, 'uid');
  const commit = args.commit?.at(-1) === 'true';
  await unlockArcanaNoctisForUid(uid, commit);
}

main().catch((error: unknown) => {
  console.error('unlockArcanaNoctis admin script failed', error);
  process.exit(1);
});
