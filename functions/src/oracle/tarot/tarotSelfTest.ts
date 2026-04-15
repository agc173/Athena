import {RequestType} from '../types';
import {drawTarotCards} from './draw';
import {buildSystemPrompt} from './prompts';
import {resolveTarotCardName} from './deck';
import {validateTarotReading, validateTarot3Reading, parseStrictJsonObject} from './schemas';

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

export function selfTestSystemPromptLanguageRule(): void {
  const prompt = buildSystemPrompt(RequestType.TAROT_1, 'de');
  if (!prompt.includes('Output language required: German.')) {
    throw new Error('SelfTest failed: tarot prompt language rule mismatch');
  }
}

export function selfTestMultilingualCardNames(): void {
  const samples = [
    {id: 'major-01-magician', lang: 'pt', expected: 'O Mago'},
    {id: 'major-01-magician', lang: 'ru', expected: 'Маг'},
    {id: 'minor-ace-wands', lang: 'fr', expected: 'As de Bâtons'},
    {id: 'minor-ace-wands', lang: 'it', expected: 'Asso di Bastoni'},
    {id: 'minor-ace-wands', lang: 'de', expected: 'Ass der Stäbe'},
  ] as const;

  samples.forEach((sample) => {
    const actual = resolveTarotCardName(sample.id, sample.lang);
    if (actual !== sample.expected) {
      throw new Error(`SelfTest failed: name mismatch for ${sample.lang}/${sample.id}`);
    }
  });
}

export function selfTestRejectSpanishForNonSpanishLang(): void {
  const draw = drawTarotCards({
    requestId: 'req-lang-guard-1',
    requestType: RequestType.TAROT_1,
    lang: 'fr',
  });

  if (draw.type !== RequestType.TAROT_1) {
    throw new Error('SelfTest failed: expected TAROT_1 draw');
  }

  const spanishReading = parseStrictJsonObject(JSON.stringify({
    type: 'TAROT_1',
    card: draw.card,
    interpretation: {
      theme: 'Consejo de energía',
      meaning: 'Esta carta marca tu camino y recuerda que debes avanzar.',
      advice: 'Evita decisiones impulsivas.',
      watchOut: 'Cuidado con tus dudas.',
    },
  }));

  let rejected = false;
  try {
    validateTarotReading(spanishReading, draw, 'fr');
  } catch {
    rejected = true;
  }

  if (!rejected) {
    throw new Error('SelfTest failed: Spanish content accepted for non-es language');
  }

  const mixedLabelCases = [
    {
      lang: 'ru',
      interpretation: {
        theme: 'TEMA',
        meaning: 'Это общее направление расклада.',
        advice: 'Consejo',
        watchOut: 'Resumen',
      },
    },
    {
      lang: 'fr',
      interpretation: {
        theme: 'TEMA',
        meaning: 'Lecture globale en français.',
        advice: 'Consejo',
        watchOut: 'Resumen',
      },
    },
  ] as const;

  mixedLabelCases.forEach((testCase) => {
    const mixedReading = parseStrictJsonObject(JSON.stringify({
      type: 'TAROT_1',
      card: draw.card,
      interpretation: testCase.interpretation,
    }));

    let mixedRejected = false;
    try {
      validateTarotReading(mixedReading, draw, testCase.lang);
    } catch {
      mixedRejected = true;
    }

    if (!mixedRejected) {
      throw new Error(`SelfTest failed: mixed Spanish labels accepted for lang=${testCase.lang}`);
    }
  });
}

export function main(): void {
  selfTestDeterministicDraw();
  selfTestTarot3Positions();
  selfTestRejectMismatchedName();
  selfTestSystemPromptLanguageRule();
  selfTestMultilingualCardNames();
  selfTestRejectSpanishForNonSpanishLang();
}
