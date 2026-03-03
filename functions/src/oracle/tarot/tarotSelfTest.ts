import {RequestType} from '../types';
import {drawTarotCards} from './draw';
import {validateTarot3Reading, parseStrictJsonObject} from './schemas';

export function selfTestDeterministicDraw(): void {
  const first = drawTarotCards({
    requestId: 'req-stable-1',
    requestType: RequestType.TAROT_3,
    lang: 'es',
  });
  const second = drawTarotCards({
    requestId: 'req-stable-1',
    requestType: RequestType.TAROT_3,
    lang: 'es',
  });

  if (JSON.stringify(first) !== JSON.stringify(second)) {
    throw new Error('SelfTest failed: deterministic draw mismatch');
  }
}

export function selfTestTarot3Positions(): void {
  const draw = drawTarotCards({
    requestId: 'req-positions-1',
    requestType: RequestType.TAROT_3,
    lang: 'en',
  });

  if (draw.type !== RequestType.TAROT_3) {
    throw new Error('SelfTest failed: expected TAROT_3 draw');
  }

  const positions = draw.cards.map((card) => card.position).join(',');
  if (positions !== 'past,present,future') {
    throw new Error('SelfTest failed: invalid TAROT_3 positions');
  }
}

export function selfTestRejectMismatchedName(): void {
  const draw = drawTarotCards({
    requestId: 'req-schema-1',
    requestType: RequestType.TAROT_3,
    lang: 'en',
  });

  if (draw.type !== RequestType.TAROT_3) {
    throw new Error('SelfTest failed: expected TAROT_3 draw');
  }

  const invalidJson = JSON.stringify({
    type: 'TAROT_3',
    cards: draw.cards.map((card, index) => ({
      ...card,
      name: index === 0 ? `${card.name} X` : card.name,
      meaning: 'Meaning',
    })),
    summary: 'Summary',
    advice: 'Advice',
  });

  let rejected = false;
  try {
    validateTarot3Reading(parseStrictJsonObject(invalidJson), draw);
  } catch {
    rejected = true;
  }

  if (!rejected) {
    throw new Error('SelfTest failed: mismatched name was accepted');
  }
}

export function main(): void {
  selfTestDeterministicDraw();
  selfTestTarot3Positions();
  selfTestRejectMismatchedName();
}
