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
- Permitir **read/write** solo al dueño autenticado (`request.auth.uid == uid`) en rutas de usuario usadas por cliente:
  - `/users/{uid}/profile/current`
  - `/users/{uid}/birthEssence/current`
  - `/users/{uid}/dailyRitual/current`
  - `/users/{uid}/habits/current`
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
   - `A` puede leer/escribir `users/A/profile/current` ✅
   - `A` puede leer/escribir `users/A/birthEssence/current` ✅
   - `A` puede leer/escribir `users/A/dailyRitual/current` ✅
   - `A` puede leer/escribir `users/A/habits/current` ✅
   - `A` **no** puede leer/escribir `users/B/profile/current` ❌
   - `A` puede leer `usernames/{any}` ✅
   - `A` **no** puede escribir `usernames/{any}` ❌
3. Probar callable `saveUserProfile` y revisar logs sanitizados para errores inesperados.

## Política recomendada — Push Notifications v1
- Cliente **NO** puede leer `/users/{uid}/pushTokens`.
- Cliente **NO** escribe directamente en `/users/{uid}/pushTokens/{tokenHash}` ni `/users/{uid}/notificationPreferences/current`.
- Altas/bajas/updates se realizan vía Cloud Functions callable con `auth` + App Check.
- Backend con Admin SDK opera con privilegios y puede mantener estado de tokens (success/failure/invalidation).
- `notificationPreferences/current` se trata como backend-authoritative (escritura mediada por callable).

