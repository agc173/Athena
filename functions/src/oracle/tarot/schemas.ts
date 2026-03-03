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
  if (value.length === 0 || value.length > maxLength) {
    throw new Error(`JSON_INVALID: ${path} invalid length`);
  }
  return value;
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
    expectedDraw: Tarot1Draw | Tarot3Draw
): Tarot1Reading | Tarot3Reading {
  if (expectedDraw.type === RequestType.TAROT_1) {
    return validateTarot1Reading(value, expectedDraw);
  }
  return validateTarot3Reading(value, expectedDraw);
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

  const cards = value.cards.map((card, index) => {
    if (!isObject(card)) {
      throw new Error(`JSON_INVALID: cards[${index}] must be object`);
    }
    assertExactKeys(card, ['position', 'name', 'orientation', 'meaning'], `$.cards[${index}]`);

    const expected = expectedDraw.cards[index];
    const position = assertPosition(card.position, `$.cards[${index}].position`);
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

    return {
      position,
      name,
      orientation,
      meaning,
    };
  });

  return {
    type: 'TAROT_3',
    cards,
    summary: assertString(value.summary, '$.summary', 500),
    advice: assertString(value.advice, '$.advice', 300),
  };
}
