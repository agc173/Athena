export type ZodiacSign =
  | 'aries' | 'taurus' | 'gemini' | 'cancer' | 'leo' | 'virgo'
  | 'libra' | 'scorpio' | 'sagittarius' | 'capricorn' | 'aquarius' | 'pisces';

export const ALL_ZODIAC_SIGNS: ZodiacSign[] = [
  'aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo',
  'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces',
];

const ZODIAC_SIGN_ALIASES: Record<string, ZodiacSign> = {
  aries: 'aries',
  tauro: 'taurus',
  taurus: 'taurus',
  gemini: 'gemini',
  géminis: 'gemini',
  geminis: 'gemini',
  cancer: 'cancer',
  cáncer: 'cancer',
  leo: 'leo',
  virgo: 'virgo',
  libra: 'libra',
  escorpio: 'scorpio',
  scorpio: 'scorpio',
  sagitario: 'sagittarius',
  sagittarius: 'sagittarius',
  capricornio: 'capricorn',
  capricorn: 'capricorn',
  acuario: 'aquarius',
  aquarius: 'aquarius',
  piscis: 'pisces',
  pisces: 'pisces',
};

function normalizeToken(raw: unknown): string {
  return String(raw ?? '')
      .trim()
      .toLowerCase()
      .normalize('NFC');
}

export function normalizeZodiacSign(raw: unknown): ZodiacSign | undefined {
  return ZODIAC_SIGN_ALIASES[normalizeToken(raw)];
}

export function isZodiacSign(raw: unknown): raw is ZodiacSign {
  return normalizeZodiacSign(raw) === raw;
}
