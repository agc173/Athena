import {getFirestore} from 'firebase-admin/firestore';

export function dateIsoMadrid(now = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Madrid',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now);
}

export function weekKeyMadrid(now = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Madrid',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short',
  }).formatToParts(now);

  const year = Number(parts.find((part) => part.type === 'year')?.value ?? '1970');
  const month = Number(parts.find((part) => part.type === 'month')?.value ?? '01');
  const day = Number(parts.find((part) => part.type === 'day')?.value ?? '01');

  const madridDate = new Date(Date.UTC(year, month - 1, day));
  const dayOfWeek = (madridDate.getUTCDay() + 6) % 7;
  madridDate.setUTCDate(madridDate.getUTCDate() + 3 - dayOfWeek);

  const weekYear = madridDate.getUTCFullYear();
  const firstThursday = new Date(Date.UTC(weekYear, 0, 4));
  const firstWeekDay = (firstThursday.getUTCDay() + 6) % 7;
  firstThursday.setUTCDate(firstThursday.getUTCDate() + 3 - firstWeekDay);

  const diffMs = madridDate.getTime() - firstThursday.getTime();
  const week = 1 + Math.round(diffMs / (7 * 24 * 60 * 60 * 1000));

  return `${weekYear}-W${String(week).padStart(2, '0')}`;
}

export function monthKeyMadrid(now = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Madrid',
    year: 'numeric',
    month: '2-digit',
  }).format(now);
}

export function economyBalanceRef(uid: string) {
  return getFirestore().doc(`economyBalances/${uid}`);
}

export function economyLedgerRef(uid: string, entryId: string) {
  return getFirestore().doc(`economyBalances/${uid}/ledger/${entryId}`);
}

export function economyUsageDailyRef(dateIso: string, uid: string) {
  return getFirestore().doc(`economyUsageDaily/${dateIso}/users/${uid}`);
}

export function economyUsageWeeklyRef(weekKey: string, uid: string) {
  return getFirestore().doc(`economyUsageWeekly/${weekKey}/users/${uid}`);
}

export function economyUsageMonthlyRef(monthKey: string, uid: string) {
  return getFirestore().doc(`economyUsageMonthly/${monthKey}/users/${uid}`);
}

export function economyLifetimeRef(uid: string) {
  return getFirestore().doc(`economyLifetime/${uid}`);
}

export function economyRequestRef(uid: string, requestId: string) {
  return getFirestore().doc(`economyRequests/${uid}/requests/${requestId}`);
}

export function economyHoroscopeUnlockRef(uid: string, unlockKey: string) {
  return getFirestore().doc(`economyUnlocks/${uid}/horoscope/${unlockKey}`);
}
