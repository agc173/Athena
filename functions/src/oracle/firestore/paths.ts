import {getFirestore} from 'firebase-admin/firestore';

export function dateIsoMadrid(now = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Madrid',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now);
}

export function oracleRef(collection: string, docId: string) {
  return getFirestore().collection(collection).doc(docId);
}

export function oracleSubRef(
    collection: string,
    docId: string,
    subCollection: string,
    subDocId: string
) {
  return getFirestore().collection(collection).doc(docId).collection(subCollection).doc(subDocId);
}
