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
    'Adapt the narrative form according to toneStyle.',
    'Do not change the JSON contract.',
    'Do not add sections, headings, or extra fields.',
    'Keep text strictly between 280 and 500 characters.',
    'toneStyle behavior:',
    '- soft-reflective: gentle and introspective tone, calm ending.',
    '- direct-action: direct voice, action verbs, clear closing.',
    '- observational: describe a concrete situation before advice.',
    '- practical-grounded: practical grounded guidance, less mystical framing.',
    '- introspective: focus on inner emotion and self-observation.',
    '- poetic-contained: elegant symbolic image, no excessive poetic language.',
    '- concise-warning: kind warning about a pattern to avoid.',
    'Avoid cliches and generic filler phrases.',
    'Do NOT use these Spanish phrases: "confía en tu intuición", "se abren nuevas puertas", "sal de tu zona de confort", "todo fluirá", "energías del universo", "el destino te sonríe", "escucha a tu corazón", "vienen cambios importantes".',
    'No emojis unless natural (max 1).',
  ].join('\n');
}

type PreviousDailyCompact = {
  dateIso: string;
  mood: string;
  luckyColor: string;
  shareText: string;
};

function compactPreviousDaily(previous: PreviousDailyCompact[]) {
  if (!previous.length) return '[]';
  return JSON.stringify(previous.slice(0, 1));
}

export function horoscopeCanonicalUserPrompt(
    dateIso: string,
    sign: ZodiacSign,
    seed: string,
    dailyAngle: string,
    toneStyle: string,
    previousCompact: PreviousDailyCompact[]
) {
  return [
    'Generate the canonical daily horoscope in Spanish for:',
    `date: ${dateIso}`,
    `sign: ${sign}`,
    `seed: ${seed}`,
    `dailyAngle: ${dailyAngle}`,
    `toneStyle: ${toneStyle}`,
    `previousHoroscopesCompact: ${compactPreviousDaily(previousCompact)}`,
    'Keep novelty against previousHoroscopesCompact while preserving quality and coherence.',
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

type PreviousHoroscopeItem = {
  periodKey: string;
  title: string;
  overview: string;
};

function compactPrevious(previous: PreviousHoroscopeItem[]) {
  if (!previous.length) return '[]';
  return JSON.stringify(previous.slice(0, 3));
}

export function weeklyCanonicalSystemPrompt() {
  return [
    'You are an expert horoscope writer for WEEKLY forecasts.',
    'Return ONLY strict valid JSON. No markdown. No extra keys.',
    'Language: es.',
    'Forbidden fields/topics: mood, luckyNumber, luckyColor.',
    'Output keys exactly:',
    'sign, weekKey, languageCode, title, overview, loveAndRelationships, workAndMoney, spiritualEnergy, weeklyAdvice, mantra, shareText.',
    'Constraints:',
    '- sign lowercase zodiac slug',
    '- weekKey format YYYY-Www',
    '- languageCode must be "es"',
    '- title 25-70 chars and non-generic',
    '- overview/loveAndRelationships/workAndMoney/spiritualEnergy/weeklyAdvice: 220-420 chars each',
    '- mantra: 8-24 words',
    '- shareText: <= 170 chars',
    '- high variability and concrete imagery, avoid repeated cliches.',
  ].join('\n');
}

export function weeklyCanonicalUserPrompt(
    weekKey: string,
    sign: ZodiacSign,
    seed: string,
    seasonContext: string,
    collectiveEnergy: string,
    previous: PreviousHoroscopeItem[]
) {
  return [
    'Generate canonical weekly horoscope in Spanish.',
    `weekKey: ${weekKey}`,
    `sign: ${sign}`,
    `seed: ${seed}`,
    `seasonContext: ${seasonContext}`,
    `collectiveEnergy: ${collectiveEnergy}`,
    `previousHoroscopesCompact: ${compactPrevious(previous)}`,
    'Output strict JSON with the required keys only.',
  ].join('\n');
}

export function weeklyTranslationSystemPrompt(lang: Lang) {
  return [
    'You are a precise translator for WEEKLY horoscope payloads.',
    'Return ONLY strict valid JSON. No markdown.',
    `Target language: ${lang}.`,
    'Keep keys unchanged and preserve structure.',
    'Do not invent or remove any fields.',
    'Translate text fields naturally, preserving tone and meaning.',
  ].join('\n');
}

export function weeklyTranslationUserPrompt(
    lang: Lang,
    canonicalPayloadJson: string
) {
  return [
    'Translate this canonical weekly horoscope JSON from Spanish.',
    `targetLang: ${lang}`,
    `canonicalJson: ${canonicalPayloadJson}`,
    `Set languageCode to "${lang}" and keep all other non-text identity fields unchanged.`,
    'Return strict JSON only.',
  ].join('\n');
}

export function monthlyCanonicalSystemPrompt() {
  return [
    'You are an expert horoscope writer for MONTHLY forecasts.',
    'Return ONLY strict valid JSON. No markdown. No extra keys.',
    'Language: es.',
    'Forbidden fields/topics: mood, luckyNumber, luckyColor.',
    'Output keys exactly:',
    'sign, monthKey, languageCode, title, monthTheme, loveAndRelationships, workAndMoney, personalGrowth, ritualSuggestion, mantra, shareText.',
    'Constraints:',
    '- sign lowercase zodiac slug',
    '- monthKey format YYYY-MM',
    '- languageCode must be "es"',
    '- title 25-70 chars and non-generic',
    '- monthTheme/loveAndRelationships/workAndMoney/personalGrowth/ritualSuggestion: 240-460 chars each',
    '- mantra: 8-24 words',
    '- shareText: <= 170 chars',
    '- high variability and concrete imagery, avoid repeated cliches.',
  ].join('\n');
}

export function monthlyCanonicalUserPrompt(
    monthKey: string,
    sign: ZodiacSign,
    seed: string,
    seasonContext: string,
    collectiveEnergy: string,
    previous: PreviousHoroscopeItem[]
) {
  return [
    'Generate canonical monthly horoscope in Spanish.',
    `monthKey: ${monthKey}`,
    `sign: ${sign}`,
    `seed: ${seed}`,
    `seasonContext: ${seasonContext}`,
    `collectiveEnergy: ${collectiveEnergy}`,
    `previousHoroscopesCompact: ${compactPrevious(previous)}`,
    'Output strict JSON with the required keys only.',
  ].join('\n');
}

export function monthlyTranslationSystemPrompt(lang: Lang) {
  return [
    'You are a precise translator for MONTHLY horoscope payloads.',
    'Return ONLY strict valid JSON. No markdown.',
    `Target language: ${lang}.`,
    'Keep keys unchanged and preserve structure.',
    'Do not invent or remove any fields.',
    'Translate text fields naturally, preserving tone and meaning.',
  ].join('\n');
}

export function monthlyTranslationUserPrompt(
    lang: Lang,
    canonicalPayloadJson: string
) {
  return [
    'Translate this canonical monthly horoscope JSON from Spanish.',
    `targetLang: ${lang}`,
    `canonicalJson: ${canonicalPayloadJson}`,
    `Set languageCode to "${lang}" and keep all other non-text identity fields unchanged.`,
    'Return strict JSON only.',
  ].join('\n');
}
