import test from 'node:test';
import assert from 'node:assert/strict';
import {FieldValue, Timestamp} from 'firebase-admin/firestore';
import {__testables} from './service';

test('request deletion patch marks account pending for 30 days', () => {
  const now = Timestamp.fromMillis(1_700_000_000_000);
  const patch = __testables.buildRequestDeletionPatch(now);

  assert.equal(patch.pendingDeletion, true);
  assert.equal(patch.deletionRequestedAt.toMillis(), now.toMillis());
  assert.equal(
      patch.scheduledDeletionAt.toMillis(),
      now.toMillis() + __testables.RECOVERY_WINDOW_MILLIS
  );
  assert.equal(patch.updatedAt.toMillis(), now.toMillis());
});

test('restore patch clears pending deletion timestamps', () => {
  const now = Timestamp.fromMillis(1_700_000_000_000);
  const patch = __testables.buildRestoreAccountPatch(now);

  assert.equal(patch.pendingDeletion, false);
  assert.equal(patch.restoredAt.toMillis(), now.toMillis());
  assert.ok(patch.deletionRequestedAt instanceof FieldValue);
  assert.ok(patch.scheduledDeletionAt instanceof FieldValue);
  assert.equal(patch.updatedAt.toMillis(), now.toMillis());
});
