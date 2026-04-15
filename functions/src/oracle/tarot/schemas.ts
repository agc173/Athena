import {RequestType} from '../types';
import {type CardOrientation, type Tarot1Draw, type Tarot3Draw, type TarotPosition} from './draw';

export interface Tarot1Reading {
  type: 'TAROT_1';
  card: {
    name: string;
    orientation: CardOrientation;
  };
  interpretation: {
    theme: string;
    meaning: string;
    advice: string;
    watchOut: string;
  };
}

export interface Tarot3Reading {
  type: 'TAROT_3';
  cards: Array<{
    position: TarotPosition;
    name: string;
    orientation: CardOrientation;
    meaning: string;
  }>;
  summary: string;
  advice: string;
}

const NON_SPANISH_ALLOWED_LANGS = new Set(['pt', 'ru', 'fr', 'it', 'de']);

function normalizeLang(lang: string): string {
  return lang.trim().toLowerCase().split('-')[0].split('_')[0];
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function assertExactKeys(obj: Record<string, unknown>, allowed: string[], path: string): void {
  const actual = Object.keys(obj).sort().join(',');
  const expected = [...allowed].sort().join(',');
  if (actual !== expected) {
    throw new Error(`JSON_INVALID: invalid keys at ${path}`);
  }
}

function assertString(value: unknown, path: string, maxLength: number): string {
  if (typeof value !== 'string') {
    throw new Error(`JSON_INVALID: ${path} must be a string`);
  }
  const trimmed = value.trim();
  if (trimmed.length === 0 || trimmed.length > maxLength) {
    throw new Error(`JSON_INVALID: ${path} invalid length`);
  }
  return trimmed;
}

function assertOrientation(value: unknown, path: string): CardOrientation {
  if (value !== 'upright' && value !== 'reversed') {
    throw new Error(`JSON_INVALID: ${path} invalid orientation`);
  }
  return value;
}

function assertPosition(value: unknown, path: string): TarotPosition {
  if (value !== 'past' && value !== 'present' && value !== 'future') {
    throw new Error(`JSON_INVALID: ${path} invalid position`);
  }
  return value;
}

export function parseStrictJsonObject(text: string): Record<string, unknown> {
  const trimmed = text.trim();
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) {
    throw new Error('JSON_INVALID: response is not a JSON object');
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(trimmed);
  } catch {
    throw new Error('JSON_INVALID: malformed JSON');
  }

  if (!isObject(parsed)) {
    throw new Error('JSON_INVALID: response root must be object');
  }

  return parsed;
}

export function validateTarotReading(
    value: Record<string, unknown>,
    expectedDraw: Tarot1Draw | Tarot3Draw,
    lang?: string
): Tarot1Reading | Tarot3Reading {
  const requestedLang = normalizeLang(lang ?? 'es');

  if (expectedDraw.type === RequestType.TAROT_1) {
    const reading = validateTarot1Reading(value, expectedDraw);
    assertLanguageGuard(reading, requestedLang);
    return reading;
  }
  const reading = validateTarot3Reading(value, expectedDraw);
  assertLanguageGuard(reading, requestedLang);
  return reading;
}

function assertLanguageGuard(reading: Tarot1Reading | Tarot3Reading, lang: string): void {
  if (!NON_SPANISH_ALLOWED_LANGS.has(lang)) {
    return;
  }

  const textBlob = reading.type === 'TAROT_1' ?
    [reading.interpretation.theme, reading.interpretation.meaning, reading.interpretation.advice, reading.interpretation.watchOut].join(' ') :
    [...reading.cards.map((card) => card.meaning), reading.summary, reading.advice].join(' ');

  if (looksClearlySpanish(textBlob)) {
    throw new Error(`LANG_INVALID: response appears to be Spanish for lang=${lang}`);
  }
}

function looksClearlySpanish(text: string): boolean {
  const normalized = ` ${text.toLowerCase().replace(/\s+/g, ' ').trim()} `;
  if (!normalized.trim()) {
    return false;
  }

  const markers = [
    ' consejo ',
    ' cuidado ',
    ' carta ',
    ' cartas ',
    ' pasado ',
    ' presente ',
    ' futuro ',
    ' energía ',
    ' deberías ',
    ' evita ',
    ' recuerda ',
    ' tu camino ',
    ' tus ',
    ' para que ',
  ];

  let score = 0;
  markers.forEach((marker) => {
    if (normalized.includes(marker)) {
      score += 1;
    }
  });

  const hasSpanishPunctuationOrChar = /[¿¡ñáéíóú]/.test(normalized);
  return score >= 2 || (score >= 1 && hasSpanishPunctuationOrChar);
}

export function validateTarot1Reading(value: Record<string, unknown>, expectedDraw: Tarot1Draw): Tarot1Reading {
  assertExactKeys(value, ['type', 'card', 'interpretation'], '$');
  if (value.type !== 'TAROT_1') {
    throw new Error('JSON_INVALID: type must be TAROT_1');
  }

  if (!isObject(value.card)) {
    throw new Error('JSON_INVALID: card must be object');
  }
  assertExactKeys(value.card, ['name', 'orientation'], '$.card');
  const cardName = assertString(value.card.name, '$.card.name', 120);
  const cardOrientation = assertOrientation(value.card.orientation, '$.card.orientation');

  if (cardName !== expectedDraw.card.name) {
    throw new Error('JSON_INVALID: card.name mismatch');
  }
  if (cardOrientation !== expectedDraw.card.orientation) {
    throw new Error('JSON_INVALID: card.orientation mismatch');
  }

  if (!isObject(value.interpretation)) {
    throw new Error('JSON_INVALID: interpretation must be object');
  }
  assertExactKeys(value.interpretation, ['theme', 'meaning', 'advice', 'watchOut'], '$.interpretation');

  return {
    type: 'TAROT_1',
    card: {
      name: cardName,
      orientation: cardOrientation,
    },
    interpretation: {
      theme: assertString(value.interpretation.theme, '$.interpretation.theme', 120),
      meaning: assertString(value.interpretation.meaning, '$.interpretation.meaning', 500),
      advice: assertString(value.interpretation.advice, '$.interpretation.advice', 300),
      watchOut: assertString(value.interpretation.watchOut, '$.interpretation.watchOut', 200),
    },
  };
}

export function validateTarot3Reading(value: Record<string, unknown>, expectedDraw: Tarot3Draw): Tarot3Reading {
  assertExactKeys(value, ['type', 'cards', 'summary', 'advice'], '$');
  if (value.type !== 'TAROT_3') {
    throw new Error('JSON_INVALID: type must be TAROT_3');
  }
  if (!Array.isArray(value.cards) || value.cards.length !== 3) {
    throw new Error('JSON_INVALID: cards must have length 3');
  }

  const expectedByPosition = new Map<TarotPosition, Tarot3Draw['cards'][number]>(
      expectedDraw.cards.map((card) => [card.position, card])
  );
  const cardsByPosition = new Map<TarotPosition, Tarot3Reading['cards'][number]>();

  value.cards.forEach((card, index) => {
    if (!isObject(card)) {
      throw new Error(`JSON_INVALID: cards[${index}] must be object`);
    }
    assertExactKeys(card, ['position', 'name', 'orientation', 'meaning'], `$.cards[${index}]`);

    const position = assertPosition(card.position, `$.cards[${index}].position`);
    if (cardsByPosition.has(position)) {
      throw new Error(`JSON_INVALID: cards[${index}].position duplicated`);
    }

    const expected = expectedByPosition.get(position);
    if (!expected) {
      throw new Error(`JSON_INVALID: cards[${index}].position mismatch`);
    }

    const name = assertString(card.name, `$.cards[${index}].name`, 120);
    const orientation = assertOrientation(card.orientation, `$.cards[${index}].orientation`);
    const meaning = assertString(card.meaning, `$.cards[${index}].meaning`, 350);

    if (position !== expected.position) {
      throw new Error(`JSON_INVALID: cards[${index}].position mismatch`);
    }
    if (name !== expected.name) {
      throw new Error(`JSON_INVALID: cards[${index}].name mismatch`);
    }
    if (orientation !== expected.orientation) {
      throw new Error(`JSON_INVALID: cards[${index}].orientation mismatch`);
    }

    cardsByPosition.set(position, {
      position,
      name,
      orientation,
      meaning,
    });
  });

  const orderedPositions: TarotPosition[] = ['past', 'present', 'future'];
  const cards = orderedPositions.map((position) => {
    const card = cardsByPosition.get(position);
    if (!card) {
      throw new Error('JSON_INVALID: cards must contain past, present and future');
    }
    return card;
  });

  return {
    type: 'TAROT_3',
    cards,
    summary: assertString(value.summary, '$.summary', 500),
    advice: assertString(value.advice, '$.advice', 300),
  };
}
