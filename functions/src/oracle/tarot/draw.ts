import {RequestType} from '../types';
import {TAROT_DECK, normalizeTarotLang, resolveTarotCardName, type TarotLang} from './deck';

export type CardOrientation = 'upright' | 'reversed';
export type TarotPosition = 'past' | 'present' | 'future';

export interface DrawnCard {
  id: string;
  name: string;
  orientation: CardOrientation;
}

export interface Tarot1Draw {
  type: RequestType.TAROT_1;
  card: DrawnCard;
}

export interface Tarot3Draw {
  type: RequestType.TAROT_3;
  cards: Array<DrawnCard & {position: TarotPosition}>;
}

export type TarotDrawResult = Tarot1Draw | Tarot3Draw;

function fnv1a32(input: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < input.length; i++) {
    hash ^= input.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return hash >>> 0;
}

function mulberry32(seed: number): () => number {
  let t = seed;
  return () => {
    t += 0x6D2B79F5;
    let x = Math.imul(t ^ (t >>> 15), 1 | t);
    x ^= x + Math.imul(x ^ (x >>> 7), 61 | x);
    return ((x ^ (x >>> 14)) >>> 0) / 4294967296;
  };
}

function normalizeLang(lang: string): TarotLang {
  return normalizeTarotLang(lang);
}

function shuffledIndexes(rng: () => number, size: number): number[] {
  const indexes = Array.from({length: size}, (_, i) => i);
  for (let i = indexes.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [indexes[i], indexes[j]] = [indexes[j], indexes[i]];
  }
  return indexes;
}

export function drawTarotCards(params: {
  requestId: string;
  requestType: RequestType.TAROT_1 | RequestType.TAROT_3;
  lang: string;
}): TarotDrawResult {
  const seed = fnv1a32(params.requestId);
  const rng = mulberry32(seed);
  const lang = normalizeLang(params.lang);
  const indexes = shuffledIndexes(rng, TAROT_DECK.length);

  const makeCard = (index: number): DrawnCard => {
    const card = TAROT_DECK[index];
    return {
      id: card.id,
      name: resolveTarotCardName(card.id, lang),
      orientation: rng() < 0.5 ? 'upright' : 'reversed',
    };
  };

  if (params.requestType === RequestType.TAROT_1) {
    return {
      type: RequestType.TAROT_1,
      card: makeCard(indexes[0]),
    };
  }

  return {
    type: RequestType.TAROT_3,
    cards: [
      {position: 'past', ...makeCard(indexes[0])},
      {position: 'present', ...makeCard(indexes[1])},
      {position: 'future', ...makeCard(indexes[2])},
    ],
  };
}
