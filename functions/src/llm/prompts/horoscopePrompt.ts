import type {Lang} from '../../config/env';
import type {ZodiacSign} from '../../firestore/paths';

export function horoscopeSystemPrompt(lang: Lang) {
  return [
    'You are a horoscope generator.',
    'Return ONLY valid JSON, no markdown, no commentary.',
    `Language: ${lang}.`,
    'Fields: text, mood, luckyNumber, luckyColor, shareText.',
    'Constraints:',
    '- text: 280-500 characters',
    '- mood: 1-3 words',
    '- luckyNumber: integer 1-99',
    '- luckyColor: 1-2 words',
    '- shareText: <= 140 characters, catchy',
    'No emojis unless natural (max 1).',
  ].join('\n');
}

export function horoscopeUserPrompt(dateIso: string, sign: ZodiacSign, lang: Lang) {
  return [
    'Generate the daily horoscope for:',
    `date: ${dateIso}`,
    `sign: ${sign}`,
    `lang: ${lang}`,
    'Output JSON with the required fields.',
  ].join('\n');
}

export function horoscopeCanonicalSystemPrompt() {
  return [
    'You are a horoscope generator.',
    'Return ONLY valid JSON, no markdown, no commentary.',
    'Language: es.',
    'Fields: text, mood, luckyNumber, luckyColor, shareText.',
    'Constraints:',
    '- text: 280-500 characters',
    '- mood: 1-3 words',
    '- luckyNumber: integer 1-99',
    '- luckyColor: 1-2 words',
    '- shareText: <= 140 characters, catchy',
    'No emojis unless natural (max 1).',
  ].join('\n');
}

export function horoscopeCanonicalUserPrompt(dateIso: string, sign: ZodiacSign) {
  return [
    'Generate the canonical daily horoscope in Spanish for:',
    `date: ${dateIso}`,
    `sign: ${sign}`,
    'Output JSON with all required fields.',
  ].join('\n');
}

export function horoscopeTranslationSystemPrompt(lang: Lang) {
  return [
    'You are a precise translator for horoscope texts.',
    'Return ONLY valid JSON, no markdown, no commentary.',
    `Target language: ${lang}.`,
    'Translate ONLY these fields: text, shareText, mood, luckyColor.',
    'Preserve tone and meaning.',
    'No emojis unless natural (max 1).',
  ].join('\n');
}

export function horoscopeTranslationUserPrompt(
    dateIso: string,
    sign: ZodiacSign,
    lang: Lang,
    canonicalText: string,
    canonicalShareText: string,
    canonicalMood: string,
    canonicalLuckyColor: string
) {
  return [
    'Translate the canonical Spanish horoscope content.',
    `date: ${dateIso}`,
    `sign: ${sign}`,
    `targetLang: ${lang}`,
    `text_es: ${JSON.stringify(canonicalText)}`,
    `shareText_es: ${JSON.stringify(canonicalShareText)}`,
    `mood_es: ${JSON.stringify(canonicalMood)}`,
    `luckyColor_es: ${JSON.stringify(canonicalLuckyColor)}`,
    'Output JSON with ONLY: text, shareText, mood, luckyColor.',
  ].join('\n');
}
