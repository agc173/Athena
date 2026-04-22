import {Timestamp, getFirestore} from 'firebase-admin/firestore';
import type {PremiumStatus} from './types';

export async function getPremiumStatus(uid: string): Promise<PremiumStatus> {
  const db = getFirestore();
  const entitlementSnap = await db.doc(`userEntitlements/${uid}`).get();

  const rawUpdatedAt = entitlementSnap.data()?.updatedAt;
  return {
    isPremium: entitlementSnap.data()?.isSubscriber === true,
    source: 'userEntitlements',
    updatedAt: rawUpdatedAt instanceof Timestamp ? rawUpdatedAt : undefined,
  };
}
