import {HttpsError} from 'firebase-functions/v2/https';
import {DateTime} from 'luxon';
import {ENV} from '../../config/env';

const MADRID_ZONE = 'Europe/Madrid';

function baseMadridDate(): DateTime {
  return DateTime.now().setZone(MADRID_ZONE).plus({days: ENV.DATE_OFFSET_DAYS});
}

export function getCurrentAndNextWeekKeys(): {currentWeekKey: string; nextWeekKey: string} {
  const base = baseMadridDate().startOf('week');
  return {
    currentWeekKey: base.toFormat('kkkk-\'W\'WW'),
    nextWeekKey: base.plus({weeks: 1}).toFormat('kkkk-\'W\'WW'),
  };
}

export function getCurrentAndNextMonthKeys(): {currentMonthKey: string; nextMonthKey: string} {
  const base = baseMadridDate().startOf('month');
  return {
    currentMonthKey: base.toFormat('yyyy-MM'),
    nextMonthKey: base.plus({months: 1}).toFormat('yyyy-MM'),
  };
}

export function normalizeWeekKey(value: unknown): string {
  if (typeof value !== 'string') throw new HttpsError('invalid-argument', 'weekKey is required');
  const trimmed = value.trim();
  if (!/^\d{4}-W\d{2}$/.test(trimmed)) {
    throw new HttpsError('invalid-argument', 'weekKey format must be YYYY-Www');
  }

  const dt = DateTime.fromFormat(trimmed, 'kkkk-\'W\'WW', {zone: MADRID_ZONE, locale: 'en'});
  if (!dt.isValid) throw new HttpsError('invalid-argument', 'weekKey is invalid');

  if (dt.toFormat('kkkk-\'W\'WW') !== trimmed) {
    throw new HttpsError('invalid-argument', 'weekKey is invalid');
  }

  return trimmed;
}

export function normalizeMonthKey(value: unknown): string {
  if (typeof value !== 'string') throw new HttpsError('invalid-argument', 'monthKey is required');
  const trimmed = value.trim();
  if (!/^\d{4}-\d{2}$/.test(trimmed)) {
    throw new HttpsError('invalid-argument', 'monthKey format must be YYYY-MM');
  }

  const dt = DateTime.fromFormat(trimmed, 'yyyy-MM', {zone: MADRID_ZONE});
  if (!dt.isValid || dt.month < 1 || dt.month > 12) {
    throw new HttpsError('invalid-argument', 'monthKey is invalid');
  }

  if (dt.toFormat('yyyy-MM') !== trimmed) {
    throw new HttpsError('invalid-argument', 'monthKey is invalid');
  }

  return trimmed;
}

export function assertAllowedWeekKey(weekKey: string) {
  const {currentWeekKey, nextWeekKey} = getCurrentAndNextWeekKeys();
  if (weekKey !== currentWeekKey && weekKey !== nextWeekKey) {
    throw new HttpsError('failed-precondition', 'weekKey must be current week or next week');
  }
}

export function assertAllowedMonthKey(monthKey: string) {
  const {currentMonthKey, nextMonthKey} = getCurrentAndNextMonthKeys();
  if (monthKey !== currentMonthKey && monthKey !== nextMonthKey) {
    throw new HttpsError('failed-precondition', 'monthKey must be current month or next month');
  }
}
