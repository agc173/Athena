import {getFirestore} from 'firebase-admin/firestore';

export type WriteOnceResult = 'created' | 'skipped';

export async function createDocIfAbsent(path: string, data: Record<string, unknown>): Promise<WriteOnceResult> {
  const db = getFirestore();
  const ref = db.doc(path);

  try {
    await ref.create(data);
    return 'created';
  } catch (e: unknown) {
    const err = e as {code?: number | string; message?: string};
    const code = err.code;
    if (code === 6 || code === 'already-exists' || String(err.message ?? '').includes('ALREADY_EXISTS')) {
      return 'skipped';
    }
    throw e;
  }
}
