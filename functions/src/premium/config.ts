import {ENV} from '../config/env';

export const PREMIUM_CONFIG = {
  androidPackageName: ENV.ANDROID_PACKAGE_NAME,
  googlePlayProductMonthly: ENV.GOOGLE_PLAY_PRODUCT_MONTHLY,
  googlePlayBasePlanMonthly: ENV.GOOGLE_PLAY_BASE_PLAN_MONTHLY,
} as const;
