# Firestore Security Rules

Archivo fuente de reglas: `firestore.rules`.

## Objetivo actual
- Permitir **read** únicamente a usuarios autenticados (`request.auth != null`) para horóscopos:
  - `/horoscopeDaily/{dateIso}/signs/{sign}`
  - `/horoscopeDaily/{dateIso}/signs/{sign}/langs/{lang}`
  - `/horoscopeWeekly/{weekKey}/signs/{sign}`
  - `/horoscopeWeekly/{weekKey}/signs/{sign}/langs/{lang}`
  - `/horoscopeMonthly/{monthKey}/signs/{sign}`
  - `/horoscopeMonthly/{monthKey}/signs/{sign}/langs/{lang}`
- Denegar **todas** las escrituras desde cliente en esas rutas.
- Mantener `deny by default` para el resto de documentos (incluyendo users/economy), evitando aperturas globales.

## Nota backend/admin
- Cloud Functions con Admin SDK **bypassean** Firestore Rules, por lo que la denegación de `write` al cliente no bloquea los procesos backend.
