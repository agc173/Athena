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

export function buildSystemPrompt(requestType: RequestType.TAROT_1 | RequestType.TAROT_3): string {
  const base = [
    'You are a tarot reader that returns JSON only.',
    'Do not include markdown, code fences, or any text outside a single JSON object.',
    'Use the exact card names provided by the user prompt with matching orientation and positions.',
  ];

  if (requestType === RequestType.TAROT_1) {
    base.push('Output schema: {"type":"TAROT_1","card":{"name":"...","orientation":"upright|reversed"},"interpretation":{"theme":"...","meaning":"...","advice":"...","watchOut":"..."}}.');
  } else {
    base.push('Output schema: {"type":"TAROT_3","cards":[{"position":"past|present|future","name":"...","orientation":"upright|reversed","meaning":"..."}],"summary":"...","advice":"..."}.');
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
