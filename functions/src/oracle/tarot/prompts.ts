import {RequestType} from '../types';
import {type TarotDrawResult} from './draw';

function normalizeWhitespace(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function normalizeQuestion(question?: string): string | undefined {
  if (!question) {
    return undefined;
  }
  const normalized = normalizeWhitespace(question);
  if (!normalized) {
    return undefined;
  }
  return normalized.slice(0, 240);
}

function languageName(lang: string): string {
  switch (lang) {
    case 'en':
      return 'English';
    case 'pt':
      return 'Portuguese';
    case 'ru':
      return 'Russian';
    case 'fr':
      return 'French';
    case 'it':
      return 'Italian';
    case 'de':
      return 'German';
    case 'es':
    default:
      return 'Spanish';
  }
}

export function buildSystemPrompt(
    requestType: RequestType.TAROT_1 | RequestType.TAROT_3,
    lang: string
): string {
  const outputLanguage = languageName(normalizeWhitespace(lang) || 'es');
  const base = [
    'You are a tarot reader that returns JSON only.',
    'Return ONLY a single JSON object. No markdown. No extra keys.',
    'Use the exact card names provided by the user prompt with matching orientation and positions.',
    `Output language required: ${outputLanguage}.`,
    `Language hard rule: the whole response must be in ${outputLanguage}.`,
  ];

  if (requestType === RequestType.TAROT_1) {
    base.push('Required keys only: type, card, interpretation. Output schema: {"type":"TAROT_1","card":{"name":"...","orientation":"upright|reversed"},"interpretation":{"theme":"...","meaning":"...","advice":"...","watchOut":"..."}}.');
  } else {
    base.push('Required keys only: type, cards, summary, advice. Output schema: {"type":"TAROT_3","cards":[{"position":"past|present|future","name":"...","orientation":"upright|reversed","meaning":"..."}],"summary":"...","advice":"..."}.');
  }

  return base.join(' ');
}

export function buildUserPrompt(params: {
  requestType: RequestType.TAROT_1 | RequestType.TAROT_3;
  lang: string;
  draw: TarotDrawResult;
  question?: string;
}): string {
  const payload = {
    requestType: params.requestType,
    lang: params.lang,
    question: normalizeQuestion(params.question),
    draw: params.draw,
  };

  return JSON.stringify(payload);
}
