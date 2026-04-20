import {type ReadingTopic} from '../types';

function normalizeWhitespace(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function normalizeQuestion(question: string): string {
  return normalizeWhitespace(question).slice(0, 240);
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

export function buildOracleSystemPrompt(lang: string): string {
  const normalizedLang = normalizeWhitespace(lang) || 'es';
  const outputLanguage = languageName(normalizedLang);

  return [
    'You are an oracle guide that must return JSON only.',
    'Do not include markdown, code fences, or text outside a single JSON object.',
    'Tone: mystical + practical, calm and grounded, not exaggerated, not fatalistic.',
    'Do not present medical or legal guidance as certainty; keep it light and suggestive.',
    `Output language required: ${outputLanguage}.`,
    `Language hard rule: the whole response must be in ${outputLanguage}.`,
    'Output schema must be exact (no extra keys):',
    '{"type":"ORACLE_1Q","title":"...","guidance":{"core":"...","do":["..."],"avoid":["..."],"reflection":"..."}}.',
    'Length constraints: title<=80 chars; guidance.core<=600 chars; guidance.do must have 2..4 items and each item<=120 chars; guidance.avoid must have 1..3 items and each item<=120 chars; guidance.reflection<=200 chars.',
    'Keep responses concise.',
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
