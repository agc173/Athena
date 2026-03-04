import {initializeApp} from 'firebase/app';
import {getAuth, signInAnonymously, connectAuthEmulator} from 'firebase/auth';
import {
  getFunctions,
  httpsCallable,
  connectFunctionsEmulator,
} from 'firebase/functions';
import {getFirestore, connectFirestoreEmulator} from 'firebase/firestore';

function randId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

async function main() {
  // Ajusta projectId si el tuyo es distinto (mira firebase.json o .firebaserc)
  const projectId = process.env.FIREBASE_PROJECT_ID ?? 'bwitch-4f01a';

  const app = initializeApp({projectId, apiKey: 'fake', appId: 'fake'});

  const auth = getAuth(app);
  connectAuthEmulator(auth, 'http://localhost:9099');

  const db = getFirestore(app);
  connectFirestoreEmulator(db, 'localhost', 8080);

  const functions = getFunctions(app, 'europe-west1');
  connectFunctionsEmulator(functions, 'localhost', 5001);

  // Auth (para que request.auth.uid exista)
  const cred = await signInAnonymously(auth);
  console.log('Signed in uid:', cred.user.uid);

  // 1) oracleGetStatus (global)
  const oracleGetStatus = httpsCallable(functions, 'oracleGetStatus');
  const statusRes = await oracleGetStatus({});
  console.log('oracleGetStatus:', statusRes.data);

  // 2) tarotDraw TAROT_1 (1 free)
  const tarotDraw = httpsCallable(functions, 'tarotDraw');
  const tarotReqId = randId('tarot1');
  const tarotRes1 = await tarotDraw({
    requestType: 'TAROT_1',
    requestId: tarotReqId,
    lang: 'es',
    question: '¿Qué energía me acompaña hoy?',
  });
  console.log('tarotDraw 1:', tarotRes1.data);

  // Idempotencia: repetir mismo requestId debe devolver lo mismo / completed
  const tarotRes2 = await tarotDraw({
    requestType: 'TAROT_1',
    requestId: tarotReqId,
    lang: 'es',
    question: '¿Qué energía me acompaña hoy?',
  });
  console.log('tarotDraw 1 (repeat):', tarotRes2.data);

  // 3) oracleAsk (0 free => requiere rewardedProof si no eres subscriber)
  const oracleAsk = httpsCallable(functions, 'oracleAsk');
  const oracleReqId = randId('oracle');
  const oracleRes = await oracleAsk({
    requestType: 'ORACLE_1Q',
    requestId: oracleReqId,
    lang: 'es',
    question: '¿Qué debería priorizar esta semana?',
    adUnlock: {rewardedProof: 'dev-test-proof'},
  });
  console.log('oracleAsk:', oracleRes.data);

  console.log('OK ✅ local test finished');
}

main().catch((e: any) => {
  console.error('Test failed ❌', e);
  if (e?.details) console.error('Error details:', e.details);
  process.exit(1);
});
