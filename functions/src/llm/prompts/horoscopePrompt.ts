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
