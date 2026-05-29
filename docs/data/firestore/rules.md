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
- Permitir **read** solo al dueño autenticado (`request.auth.uid == uid`) y **write** solo al dueño con schema validado en rutas de usuario usadas por cliente:
  - `/users/{uid}/profile/current`: claves permitidas, strings acotados, email básico, `photoUrl` HTTPS, `zodiacSign` enum y timestamps epoch/timestamp válidos.
  - `/users/{uid}/birthEssence/current`: claves permitidas, signos enum, idioma enum, arquetipo enum y texto de interpretación acotado.
  - `/users/{uid}/dailyRitual/current`: claves permitidas, fechas ISO, tema enum, booleanos/contadores acotados.
  - `/users/{uid}/habits/current`: claves permitidas, fecha ISO, arrays string acotados y contadores acotados.
- `userAccountStatus/{uid}`:
  - Permitir **get** solo al dueño autenticado (`request.auth.uid == uid`) para que el bootstrap de sesión detecte eliminación pendiente.
  - Denegar **list/write** desde cliente; las escrituras son backend-owned vía callables `requestAccountDeletion`/`restoreAccount`.
- `usernames`:
  - Permitir **read** autenticado (`/usernames/{username}`)
  - Denegar **write** desde cliente (el índice lo mantiene backend)
- Mantener `deny by default` para el resto de documentos (incluyendo economy, `/userEntitlements/{uid}` y `/purchaseTokenIndex/{hash}` backend-owned).

## Nota backend/admin
- Cloud Functions con Admin SDK **bypassean** Firestore Rules, por lo que la denegación de `write` al cliente no bloquea procesos backend como `saveUserProfile`.

## Validación recomendada (Emulator)
1. Levantar emuladores:
   - `firebase emulators:start --only auth,firestore,functions`
2. Casos mínimos con usuario autenticado UID `A`:
   - `A` puede leer y escribir payload válido en `users/A/profile/current` ✅
   - `A` puede leer y escribir payload válido en `users/A/birthEssence/current` ✅
   - `A` puede leer y escribir payload válido en `users/A/dailyRitual/current` ✅
   - `A` puede leer y escribir payload válido en `users/A/habits/current` ✅
   - `A` no puede escribir campos arbitrarios ni tipos/longitudes fuera de contrato en esas rutas ❌
   - `A` **no** puede leer/escribir `users/B/profile/current` ❌
   - `A` puede leer `userAccountStatus/A` ✅
   - `A` no puede leer `userAccountStatus/B` ❌
   - `A` no puede escribir `userAccountStatus/A` ❌
   - `A` puede leer `usernames/{any}` ✅
   - `A` **no** puede escribir `usernames/{any}` ❌
3. Probar callable `saveUserProfile` y revisar logs sanitizados para errores inesperados.

## Política recomendada — Push Notifications v1
- Cliente **NO** puede leer `/users/{uid}/pushTokens`.
- Cliente **NO** escribe directamente en `/users/{uid}/pushTokens/{tokenHash}` ni `/users/{uid}/notificationPreferences/current`.
- Altas/bajas/updates se realizan vía Cloud Functions callable con `auth` + App Check.
- Backend con Admin SDK opera con privilegios y puede mantener estado de tokens (success/failure/invalidation).
- `notificationPreferences/current` se trata como backend-authoritative (escritura mediada por callable).

