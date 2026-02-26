import type {Lang} from '../config/env';

export type ZodiacSign =
  | 'aries' | 'taurus' | 'gemini' | 'cancer' | 'leo' | 'virgo'
  | 'libra' | 'scorpio' | 'sagittarius' | 'capricorn' | 'aquarius' | 'pisces';

export function horoscopeLangDocPath(dateIso: string, sign: ZodiacSign, lang: Lang) {
  return `horoscopeDaily/${dateIso}/signs/${sign}/langs/${lang}`;
}
