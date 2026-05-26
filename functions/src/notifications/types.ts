export type PushPlatform = 'android' | 'ios';

export type RegisterPushTokenData = {
  token?: unknown;
  platform?: unknown;
  appVersion?: unknown;
  locale?: unknown;
  timezone?: unknown;
  permissionGranted?: unknown;
};

export type UnregisterPushTokenData = {
  token?: unknown;
  platform?: unknown;
};

export type UpdateNotificationPreferencesData = {
  globalEnabled?: unknown;
  dailyHoroscopeEnabled?: unknown;
  dailyRewardEnabled?: unknown;
  tarotOracleReminderEnabled?: unknown;
  ritualsEnabled?: unknown;
  habitsEnabled?: unknown;
};

export type EmptyOkResponse = { ok: true };
