import type {Lang} from '../config/env';

export type ZodiacSign =
  | 'aries' | 'taurus' | 'gemini' | 'cancer' | 'leo' | 'virgo'
  | 'libra' | 'scorpio' | 'sagittarius' | 'capricorn' | 'aquarius' | 'pisces';

export function horoscopeSignDocPath(dateIso: string, sign: ZodiacSign) {
  return `horoscopeDaily/${dateIso}/signs/${sign}`;
}

export function horoscopeLangDocPath(dateIso: string, sign: ZodiacSign, lang: Lang) {
  return `horoscopeDaily/${dateIso}/signs/${sign}/langs/${lang}`;
}
