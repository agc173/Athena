import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import type {PremiumStatus} from './types';

const ACTIVE_SUBSCRIPTION_STATUSES = new Set(['ACTIVE', 'GRACE_PERIOD']);

export async function getPremiumStatus(uid: string): Promise<PremiumStatus> {
  const db = getFirestore();
  const entitlementSnap = await db.doc(`userEntitlements/${uid}`).get();
  const data = entitlementSnap.data();
  const rawUpdatedAt = data?.updatedAt;
  const rawStatus = typeof data?.subscriptionStatus === 'string' ? data.subscriptionStatus : undefined;
  const hasModernStatus = rawStatus != null;

  return {
    isPremium: hasModernStatus ?
      data?.isSubscriber === true && ACTIVE_SUBSCRIPTION_STATUSES.has(rawStatus) :
      data?.isSubscriber === true,
    source: 'userEntitlements',
    updatedAt: rawUpdatedAt instanceof Timestamp ? rawUpdatedAt : undefined,
  };
}
