import {onSchedule} from 'firebase-functions/v2/scheduler';
import {initializeApp} from 'firebase-admin/app';
import {getFirestore} from 'firebase-admin/firestore';
import {DateTime} from 'luxon';

initializeApp();
const db = getFirestore();

const GENERATOR_VERSION = 1;

const ZODIAC_SIGNS = [
  'aries', 'taurus', 'gemini', 'cancer', 'leo', 'virgo',
  'libra', 'scorpio', 'sagittarius', 'capricorn', 'aquarius', 'pisces',
] as const;

type ZodiacSign = (typeof ZODIAC_SIGNS)[number];

function dateIsoMadrid(dayOffset: number): string {
  return DateTime.now().setZone('Europe/Madrid').plus({days: dayOffset}).toISODate()!;
}

/**
 * STUB (por ahora): luego lo cambiamos por el generador real (LLM o el que decidáis).
 * Mantiene la forma que tu app ya espera (text/mood/luckyNumber/luckyColor/shareText/updatedAtEpochMillis).
 */
function generateHoroscopeStub(sign: ZodiacSign, dateIso: string) {
  const now = Date.now();
  return {
    text: `Horóscopo de ${sign} para ${dateIso}. (stub)`,
    mood: 'neutral',
    luckyNumber: Math.floor((now / 1000) % 99) + 1,
    luckyColor: 'blue',
    shareText: `Hoy ${sign} vibra alto ✨ (${dateIso})`,
    updatedAtEpochMillis: now,
    generatorVersion: GENERATOR_VERSION,
  };
}

/**
 * Write-once REAL:
 * - ref.create(data) crea solo si NO existe
 * - si existe => ALREADY_EXISTS => skip
 */
async function writeOnce(dateIso: string, sign: ZodiacSign) {
  const ref = db
      .collection('horoscopeDaily')
      .doc(dateIso)
      .collection('signs')
      .doc(sign);

  const data = generateHoroscopeStub(sign, dateIso);

  try {
    await ref.create({
      ...data,
      createdAtEpochMillis: Date.now(),
    });
    return {created: true};
  } catch (e: any) {
    const code = e?.code;
    const msg = String(e?.message ?? '').toLowerCase();
    if (code === 6 || msg.includes('already exists')) {
      return {created: false, skipped: true};
    }
    throw e;
  }
}

/**
 * Ventana: hoy..hoy+7 (8 días)
 * Política: write-once, no toca días pasados.
 */
export const generateHoroscopesWindow = onSchedule(
    {
      schedule: 'every day 02:10',
      timeZone: 'Europe/Madrid',
      region: 'europe-west1',
      retryCount: 3,
    },
    async () => {
      let created = 0;
      let skipped = 0;

      for (let dayOffset = 0; dayOffset <= 7; dayOffset++) {
        const dateIso = dateIsoMadrid(dayOffset);

        for (const sign of ZODIAC_SIGNS) {
          const r = await writeOnce(dateIso, sign);
          if (r.created) created++;
          else skipped++;
        }
      }

      console.log('generateHoroscopesWindow done:', {created, skipped});
    }
);
