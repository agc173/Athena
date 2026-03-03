import {type ReadingTopic} from '../types';

function normalizeWhitespace(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function normalizeQuestion(question: string): string {
  return normalizeWhitespace(question).slice(0, 240);
}

export function buildOracleSystemPrompt(lang: string): string {
  const normalizedLang = normalizeWhitespace(lang) || 'es';

  return [
    'You are an oracle guide that must return JSON only.',
    'Do not include markdown, code fences, or text outside a single JSON object.',
    'Tone: mystical + practical, calm and grounded, not exaggerated, not fatalistic.',
    'Do not present medical or legal guidance as certainty; keep it light and suggestive.',
    'Use language:',
    normalizedLang + '.',
    'Output schema must be exact (no extra keys):',
    '{"type":"ORACLE_1Q","title":"...","guidance":{"core":"...","do":["..."],"avoid":["..."],"reflection":"..."}}.',
  ].join(' ');
}

export function buildOracleUserPrompt(params: {
  lang: string;
  question: string;
  topic?: ReadingTopic;
}): string {
  const payload = {
    lang: normalizeWhitespace(params.lang) || 'es',
    question: normalizeQuestion(params.question),
    topic: params.topic,
  };

  return JSON.stringify(payload);
}
