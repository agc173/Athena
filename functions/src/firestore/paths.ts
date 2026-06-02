import type {Lang} from '../config/env';
import type {ZodiacSign} from './zodiacSigns';
export type {ZodiacSign} from './zodiacSigns';

export function horoscopeSignDocPath(dateIso: string, sign: ZodiacSign) {
  return `horoscopeDaily/${dateIso}/signs/${sign}`;
}

export function horoscopeLangDocPath(dateIso: string, sign: ZodiacSign, lang: Lang) {
  return `horoscopeDaily/${dateIso}/signs/${sign}/langs/${lang}`;
}

export function horoscopeWeeklySignDocPath(weekKey: string, sign: ZodiacSign) {
  return `horoscopeWeekly/${weekKey}/signs/${sign}`;
}

export function horoscopeWeeklyLangDocPath(weekKey: string, sign: ZodiacSign, lang: Lang) {
  return `horoscopeWeekly/${weekKey}/signs/${sign}/langs/${lang}`;
}

export function horoscopeMonthlySignDocPath(monthKey: string, sign: ZodiacSign) {
  return `horoscopeMonthly/${monthKey}/signs/${sign}`;
}

export function horoscopeMonthlyLangDocPath(monthKey: string, sign: ZodiacSign, lang: Lang) {
  return `horoscopeMonthly/${monthKey}/signs/${sign}/langs/${lang}`;
}
