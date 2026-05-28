import {FieldValue, Timestamp, getFirestore} from 'firebase-admin/firestore';

const ACCOUNT_STATUS_COLLECTION = 'userAccountStatus';
const RECOVERY_WINDOW_DAYS = 30;
const RECOVERY_WINDOW_MILLIS = RECOVERY_WINDOW_DAYS * 24 * 60 * 60 * 1000;

export type AccountDeletionCallableResponse = {
  success: true;
  pendingDeletion: boolean;
};

type RequestDeletionPatch = {
  pendingDeletion: true;
  deletionRequestedAt: Timestamp;
  scheduledDeletionAt: Timestamp;
  updatedAt: Timestamp;
};

type RestoreAccountPatch = {
  pendingDeletion: false;
  restoredAt: Timestamp;
  deletionRequestedAt: FieldValue;
  scheduledDeletionAt: FieldValue;
  updatedAt: Timestamp;
};

export function buildRequestDeletionPatch(now: Timestamp): RequestDeletionPatch {
  return {
    pendingDeletion: true,
    deletionRequestedAt: now,
    scheduledDeletionAt: Timestamp.fromMillis(now.toMillis() + RECOVERY_WINDOW_MILLIS),
    updatedAt: now,
  };
}

export function buildRestoreAccountPatch(now: Timestamp): RestoreAccountPatch {
  return {
    pendingDeletion: false,
    restoredAt: now,
    deletionRequestedAt: FieldValue.delete(),
    scheduledDeletionAt: FieldValue.delete(),
    updatedAt: now,
  };
}

export async function requestAccountDeletionForUid(uid: string): Promise<AccountDeletionCallableResponse> {
  const now = Timestamp.now();
  await getFirestore()
      .collection(ACCOUNT_STATUS_COLLECTION)
      .doc(uid)
      .set(buildRequestDeletionPatch(now), {merge: true});

  // TODO(account-delete-phase-2): a future cleanup job should permanently delete/anonymize
  // eligible accounts where pendingDeletion=true and scheduledDeletionAt <= now.
  return {success: true, pendingDeletion: true};
}

export async function restoreAccountForUid(uid: string): Promise<AccountDeletionCallableResponse> {
  const now = Timestamp.now();

  // Phase 1 intentionally allows restore even after scheduledDeletionAt because no
  // physical cleanup exists yet. TODO(account-delete-phase-2): once cleanup is live,
  // block restore only after the cleanup/anonymization job has made recovery unsafe.
  await getFirestore()
      .collection(ACCOUNT_STATUS_COLLECTION)
      .doc(uid)
      .set(buildRestoreAccountPatch(now), {merge: true});

  return {success: true, pendingDeletion: false};
}

export const __testables = {
  ACCOUNT_STATUS_COLLECTION,
  RECOVERY_WINDOW_DAYS,
  RECOVERY_WINDOW_MILLIS,
  buildRequestDeletionPatch,
  buildRestoreAccountPatch,
};
