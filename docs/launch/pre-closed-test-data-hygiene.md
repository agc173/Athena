# Auditoría pre-closed testing: higiene de datos, Auth y Storage

Fecha de auditoría: 2026-06-03  
Alcance: revisión estática del repositorio antes de pasar de Internal Testing a Closed Testing en Google Play.  
Restricciones aplicadas: no se borraron datos, no se tocó producción, no se hizo deploy, no se modificó economía ni UI.

> Nota importante: esta auditoría no incluye inspección directa de Firebase Console ni de datos reales. Las conclusiones sobre usuarios, objetos Storage y costes deben confirmarse manualmente en Console/Google Cloud con el checklist incluido.

---

## 1. Resumen ejecutivo

### Estado general

- **Firestore Rules están en modo deny-by-default** para rutas no listadas. El cliente solo puede leer horóscopos autenticado, escribir documentos user-owned concretos bajo `users/{uid}` con validación, leer su `userAccountStatus`, leer `usernames`, y leer progreso de mazos propio. Economy, premium, push backend-owned y la mayoría de historiales quedan cerrados al cliente.
- **No existe `storage.rules` versionado ni configuración Storage en `firebase.json`**. Esto es el principal hueco documental/operativo de esta auditoría: no se puede demostrar desde el repo que el bucket impida lectura/escritura cross-user, limite MIME type o limite tamaño.
- **La app sí puede crear usuarios email/password desde UI**, porque la pantalla de Auth expone botón de registro y el repositorio llama a `createUserWithEmailAndPassword`. No hay referencias hardcodeadas a `test@test.com` ni `test@example.com` encontradas en código fuente versionado.
- **El flujo de avatar sube a `users/{uid}/profile/avatar_<timestamp>.<ext>`** usando el uid autenticado. El cliente no valida tamaño antes de subir y solo deriva extensión desde MIME type; la seguridad real debe estar en Storage Rules.
- **Firestore tiene varias colecciones que crecen por fecha/request/log**: `horoscopeDaily`, `horoscopeWeekly`, `horoscopeMonthly`, `oracleRequests`, `oracleAnswers`, `tarotReadings`, `llmUsageDaily`, `economyUsageDaily/Weekly/Monthly`, `economyRequests`, ledger económico, push tokens/sends y purchase/entitlement indexes. No se detecta TTL documentado ni campo `expiresAt` generalizado para cleanup.

### Clasificación rápida

| Severidad | Hallazgo | Recomendación |
|---|---|---|
| **Bloqueante antes de closed testing** | Falta `storage.rules` versionado/configurado en repo; no se puede auditar owner-check, MIME ni tamaño para avatares. | Confirmar reglas actuales del bucket en Firebase Console. Si no son estrictas, crear `storage.rules` mínimo antes de ampliar testers. |
| **Recomendable antes de closed testing** | No hay política TTL/retención documentada para historiales/logs de request, LLM usage, push sends y horóscopos antiguos. | Activar TTL solo tras acordar campos y preservar economía/auditoría. Priorizar logs no usados por UI. |
| **Recomendable antes de closed testing** | Usuarios desconocidos `test@test.com` / `test@example.com` podrían venir de registro manual, pre-launch, internal testing o cuentas de prueba externas. | No borrar. Deshabilitar primero, observar, vincular UID contra Firestore/Storage y borrar solo tras confirmar. |
| **Puede esperar post-launch** | Cleanup best-effort de avatar anterior depende de URL previa y no limpia huérfanos si falla subida/guardado de perfil. | Job o callable backend de limpieza de avatares antiguos por uid con dry-run. |
| **Solo documentación** | No existe `docs/data/storage` para contrato de rutas Storage. | Añadirlo junto con reglas cuando se implemente el fix. |

---

## 2. Evidencia revisada

### Archivos/rutas principales

- Firestore rules: `firestore.rules`.
- Config Firebase: `firebase.json`.
- Firestore docs: `docs/data/firestore/schema.md`, `docs/data/firestore/rules.md`.
- Auth cliente: `AuthScreen`, `SessionViewModel`, `FirebaseAuthRepository`.
- Avatar cliente: `FirebaseAvatarRepository`, `AvatarPicker.android.kt`, `AvatarPicker.ios.kt`, `UserProfileViewModel`, `OnboardingProfileViewModel`.
- Backend/Functions: horóscopos, Oracle/Tarot, economy, premium, push notifications, account deletion.

### Búsquedas ejecutadas

- Emails sospechosos y creación de usuarios: `rg -n "test@test\.com|test@example\.com|createUserWithEmailAndPassword|signUpWithEmail|signInWithEmail|password|email|demo|mock|seed" ...`
- Storage/avatar: `rg -n "avatar|photoUrl|FirebaseStorage|storage|profile/current|saveUserProfile|AvatarPicker|pick|camera|gallery|mime|contentType|putFile|putData|upload" ...`
- Colecciones de crecimiento: `rg -n "collection\(|\.doc\(|doc\(|oracleRequests|oracleAnswers|tarotReadings|economyRequests|usage|logs|push|notifications|horoscopes|users|profile|avatars|storage" ...`

---

## 3. Firestore data retention / costes

### 3.1 Colecciones auditadas y recomendación TTL

| Colección/ruta | Origen en código/docs | Uso probable UI/backend | Retención recomendada | Motivo |
|---|---|---|---|---|
| `horoscopeDaily/{dateIso}/signs/{sign}` y `langs/{lang}` | Functions escriben documentos diarios; Rules permiten lectura autenticada. | UI de horóscopo diario y unlocks de días futuros/pasados cercanos. | **90 días** como primera política conservadora; considerar 30/60 si UI no muestra histórico. | Crece por día × signo × idioma. Mantener indefinidamente aumenta lecturas/index storage sin valor si UI solo consume actual/futuro cercano. |
| `horoscopeWeekly/{weekKey}/signs/{sign}` y `langs/{lang}` | Functions escriben weekly. | UI weekly actual/futuro desbloqueado. | **180 días** o **90 días** si no hay histórico en UI. | Semanal crece más lento; puede ser útil para desbloqueos recientes. |
| `horoscopeMonthly/{monthKey}/signs/{sign}` y `langs/{lang}` | Functions escriben monthly. | UI monthly actual/futuro desbloqueado. | **365 días** o **180 días** si no hay histórico. | Mensual crece poco; coste bajo. TTL más largo reduce riesgo de borrar contenido premium reciente. |
| `oracleRequests/{requestId}` legacy | Tarot legacy usa request global para idempotencia. | Idempotencia/retry legacy, no UI directa. | **60-90 días**, solo para estados terminales. | Una vez completado y fuera de ventana de retry, el valor baja. No aplicar a `PROCESSING` sin watchdog/refund. |
| `oracleAnswers/{uid}/items/{requestId}` | Oracle escribe historial de respuestas. | Potencial historial de usuario; revisar si UI lo muestra. | **90 días** si no se muestra; **conservar** o pedir consentimiento/export si es historial visible. | Contiene pregunta/respuesta del usuario; coste y privacidad suben indefinidamente. |
| `tarotReadings/{uid}/items/{readingId}` | Tarot escribe tiradas. | Potencial historial de usuario; revisar UI actual. | **90 días** si no se muestra; **conservar** si es historial visible/premium. | Similar a Oracle: contenido generado y privado. |
| `llmUsageDaily/{dateIso}/scopes/{scope}` y `providers/{provider}` | Contadores de coste/caps. | Auditoría técnica/coste; no UI. | **90 días**. | Retener suficiente para análisis de coste/incidencias; no hace falta indefinido salvo reporting financiero externo. |
| `economyUsageDaily/{dateIso}/users/{uid}` | Límites diarios y claims. | Quotas actuales; auditoría de abusos. | **90 días** para uso diario; conservar snapshots agregados si se necesitan métricas. | Puede crecer por usuario/día. No borrar días recientes por soporte antifraude. |
| `economyUsageWeekly/{weekKey}/users/{uid}` | Límite semanal Tarot. | Quotas actuales. | **180 días**. | Baja caducidad funcional tras la semana, pero útil para soporte. |
| `economyUsageMonthly/{monthKey}/users/{uid}` | Límite mensual Birth Essence/premium. | Quotas mensuales. | **365 días** o **180 días mínimo**. | Puede ser necesario para soporte de beneficios premium. |
| `economyRequests/{uid}/requests/{requestId}` | Idempotencia y auditoría de economía v2. | Backend idempotency/refund/watchdog. | **Conservar 180-365 días para terminales**; **no TTL automático para `PROCESSING` sin watchdog**. | Contiene evidencia de cargos/refunds y respuesta estable. Borrar demasiado pronto rompe idempotencia y soporte. |
| `economyBalances/{uid}` | Saldo actual de Lunas. | Crítico economía. | **No TTL**. | Estado económico actual; no borrar. |
| `economyBalances/{uid}/ledger/{entryId}` | Ledger de movimientos. | Auditoría económica/premium/soporte. | **No TTL en MVP**; definir retención legal/financiera antes de limpiar. | Evidencia de compras, rewards, cargos y refunds. |
| `economyUnlocks/{uid}/horoscope/{unlockKey}` | Desbloqueos de horóscopo por período. | Acceso premium/economía. | **No TTL mientras el contenido desbloqueado pueda consultarse**; revisar junto a TTL de horóscopos. | Borrar unlocks puede quitar acceso comprado/desbloqueado. |
| `userEntitlements/{uid}` | Premium status. | Crítico premium. | **No TTL**. | Estado de suscripción/entitlement. |
| `purchaseTokenIndex/{hash}` | Índice ownership token compra. | Antifraude/restore. | **No TTL sin política legal definida**. | Evita reutilización de tokens y soporta restauraciones. |
| `users/{uid}/pushTokens/{tokenHash}` | Callable push. | Envío push. | **TTL/anonymization 30-90 días tras `enabled=false`/`invalidatedAt`**. | Token raw es sensible y no debe quedarse indefinidamente si inválido. |
| `pushTokenIndex/{tokenHash}` | Índice global push. | Ownership token activo. | **Borrar al unregister; TTL 30-90 días para índices huérfanos si aparecen**. | Ya se borra si owner coincide en unregister; revisar huérfanos. |
| `users/{uid}/pushNotificationSends/{dateIso_daily_horoscope}` | Scheduler diario. | Idempotencia de envío por día. | **30-60 días**. | Solo evita duplicados del día/campaña y sirve para debugging reciente. |
| `userAccountStatus/{uid}` | Eliminación suave. | Restore/cuenta. | **No TTL hasta implementar cleanup definitivo de cuenta**. | Controla ventana de restauración y futuro borrado/anonymization. |
| `users/{uid}/profile/current`, `birthEssence/current`, `dailyRitual/current`, `habits/current`, `tarotDeckProgress` | User-owned/estado activo. | UI actual. | **No TTL** para docs `current`; cleanup solo al borrar cuenta. | Estado activo del usuario. |

### 3.2 Datos que no deben borrarse por auditoría económica/premium

No aplicar TTL ni limpieza manual a estas rutas sin una decisión explícita de producto/legal y mecanismo de export/soporte:

- `economyBalances/{uid}` y subcolección `ledger`.
- `economyUnlocks/{uid}/horoscope/{unlockKey}` mientras esos unlocks den acceso a contenido.
- `userEntitlements/{uid}`.
- `purchaseTokenIndex/{hash}`.
- `economyRequests/{uid}/requests/{requestId}` relacionados con cargos/refunds premium o Moon spend, salvo política madura de retención terminal.
- Documentos de account deletion hasta que exista job definitivo y pruebas de restauración.

### 3.3 Datos que probablemente pueden limpiarse si no se usan en UI

Confirmar primero en UI/analytics y hacer dry-run:

- `llmUsageDaily` más antiguo de 90 días.
- `users/{uid}/pushNotificationSends` más antiguo de 30/60 días.
- `oracleRequests` legacy terminales más antiguos de 60/90 días.
- `oracleAnswers` y `tarotReadings` más antiguos de 90 días **solo si la UI no ofrece historial** o si el producto acepta retención limitada de historial.
- `horoscopeDaily` antiguo si la UI no permite consultar fechas antiguas ni hay unlocks activos asociados.

### 3.4 Coste/riesgo de mantener indefinidamente

- **Coste directo**: almacenamiento de documentos, índices y subcolecciones por día/usuario/provider. El crecimiento por `dateIso` y por usuario es acumulativo.
- **Coste indirecto**: consultas de collection group o mantenimiento de índices más grandes pueden tardar más y requerir índices compuestos.
- **Privacidad**: Oracle/Tarot y perfiles pueden contener contenido sensible o personal; retener indefinidamente aumenta impacto ante incidente.
- **Soporte/economía**: borrar mal `ledger`, `entitlements`, `purchaseTokenIndex`, `economyRequests` o `economyUnlocks` puede romper restauraciones, idempotencia o evidencia de cargos/refunds.

---

## 4. Firebase Auth: usuarios desconocidos

### 4.1 Resultado de búsqueda estática

- No se encontraron referencias versionadas a `test@test.com` ni `test@example.com`.
- La app **sí permite registro manual email/password** desde Auth UI.
- `SessionViewModel.signUpWithEmail` llama al repositorio de Auth, y `FirebaseAuthRepository.signUpWithEmail` usa `createUserWithEmailAndPassword`.
- No se detectaron scripts versionados que creen esos emails concretos. Sí existen scripts/admin tooling para otras tareas, pero no hallazgo de seed de usuarios Auth con esos correos.
- Hay mocks/tests con emails genéricos como `user@example.com`, pero no los dos emails investigados como credenciales productivas.

### 4.2 ¿Puede la app crear esos usuarios desde código?

Sí, si alguien introduce esos emails y una contraseña válida en la UI de registro. La app no los crea automáticamente ni los hardcodea, pero el flujo normal de sign-up permite que cualquier tester/pre-launch cree usuarios email/password si el proveedor está habilitado.

### 4.3 Posibles orígenes plausibles

- **Tester humano** en Internal Testing que creó cuentas simples.
- **Google Play pre-launch report / testing automatizado**, si interactuó con la pantalla de registro y generó credenciales de prueba.
- **QA/manual local** usando el proyecto Firebase real por error.
- **Cuentas residuales antiguas** creadas antes de endurecer flujos/reglas.
- **Abuso externo menor**, si la app/proyecto permite registros email/password públicos y el binario/config se distribuyó.

No hay evidencia estática suficiente para atribuir origen.

### 4.4 Qué revisar en Firebase Auth

Para `test@test.com` y `test@example.com`, abrir Firebase Console → Authentication → Users y registrar:

1. **UID**.
2. **Provider(s)**: `password`, `google.com`, etc.
3. **Creation date**.
4. **Last sign-in**.
5. **Email verified**.
6. **Disabled** actual.
7. **Tenant/project** correcto.
8. **Metadata adicional**: si aparece displayName/photoURL de provider social.

### 4.5 Qué buscar por UID en Firestore/Storage

Para cada UID desconocido:

Firestore:

- `users/{uid}/profile/current`
- `users/{uid}/birthEssence/current`
- `users/{uid}/dailyRitual/current`
- `users/{uid}/habits/current`
- `users/{uid}/pushTokens/*`
- `users/{uid}/pushNotificationSends/*`
- `users/{uid}/notificationPreferences/current`
- `economyBalances/{uid}` y `economyBalances/{uid}/ledger/*`
- `economyRequests/{uid}/requests/*`
- `economyUsageDaily/*/users/{uid}`
- `economyUsageWeekly/*/users/{uid}`
- `economyUsageMonthly/*/users/{uid}`
- `economyLifetime/{uid}`
- `economyUnlocks/{uid}/horoscope/*`
- `oracleAnswers/{uid}/items/*`
- `tarotReadings/{uid}/items/*`
- `userEntitlements/{uid}`
- `userAccountStatus/{uid}`
- `purchaseTokenIndex/*` donde `uid == <uid>`

Storage:

- `users/{uid}/profile/*`
- Cualquier prefijo futuro `users/{uid}/...`

### 4.6 Protocolo seguro propuesto

1. **No borrar de entrada**.
2. Exportar/registrar evidencia manual: email, UID, provider, creation date, last login, paths Firestore/Storage encontrados.
3. Si no son cuentas conocidas, **deshabilitar el usuario en Firebase Auth**.
4. Observar durante 7-14 días:
   - errores de login reportados,
   - actividad de Firestore/Functions asociada al UID,
   - intentos de push/token refresh,
   - tickets de testers.
5. Si no hay actividad legítima ni datos económicos/premium, planificar borrado con dry-run:
   - borrar Auth solo tras decidir qué hacer con Firestore/Storage,
   - conservar o anonimizar ledger/evidencia si existió economía/premium,
   - borrar Storage únicamente si se confirma owner y no hay obligación de conservar.

---

## 5. Storage avatars / fotos inesperadas

### 5.1 Hallazgos de código

- El repositorio de avatar obtiene el uid autenticado y sube a `users/{uid}/profile/avatar_<timestamp>.<ext>`.
- La extensión se decide por MIME type (`image/png`, `image/webp`; resto `jpg`), pero la subida usa `putFile(file)` sin metadata explícita ni validación de tamaño desde cliente.
- Android usa `ActivityResultContracts.GetContent()` con filtro `image/*` y toma MIME desde `ContentResolver`.
- iOS abre Photo Library, toma la imagen original, la convierte a JPEG temporal con calidad `0.9` y reporta `image/jpeg`.
- Tras subir, se obtiene download URL y se intenta borrar best-effort el avatar anterior solo si la URL previa decodifica a un path que empieza por `users/{uid}/profile/avatar_`.
- En perfil principal, tras subir avatar, se guarda el `photoUrl` en `users/{uid}/profile/current`. En onboarding, el upload actualiza estado local y el perfil se persistirá cuando el usuario guarde el onboarding.

### 5.2 Lo que no se puede probar desde repo

- No hay `storage.rules` versionado.
- `firebase.json` no declara configuración de Storage rules.
- No hay `docs/data/storage` con contrato de rutas ni política de retención.

Por tanto, desde el repositorio no se puede afirmar que:

- solo el owner pueda escribir `users/{uid}/profile/*`,
- se bloquee escritura cross-user,
- se limite lectura a owner,
- se limiten uploads a `image/*`,
- exista límite de tamaño,
- se impida overwrite/delete fuera del patrón de avatar,
- se haga cleanup server-side de huérfanos.

### 5.3 Riesgos concretos

| Riesgo | Estado actual en repo | Impacto |
|---|---|---|
| Escritura cross-user | Depende 100% de reglas reales del bucket, no versionadas. | Un usuario podría subir o reemplazar fotos en carpeta ajena si reglas son permisivas. |
| Lectura pública | Download URLs con token permiten acceso a quien tenga URL; reglas deciden `get/list` directos. | Si reglas son públicas, se podrían listar/ver fotos ajenas. |
| MIME spoofing | Cliente no valida contenido real; reglas deben comprobar `request.resource.contentType.matches('image/.*')`. | Subida de binarios no imagen o contenido abusivo. |
| Tamaño excesivo | Cliente no limita tamaño; iOS convierte a JPEG 0.9 potencialmente grande. | Coste Storage/egress y mala UX. |
| Huérfanos | Cleanup anterior es best-effort y solo tras upload; si falla guardado de perfil, queda objeto no referenciado. | Coste incremental y confusión al investigar fotos. |
| Foto “equivocada” | Picker usa imagen seleccionada por el usuario/sistema; no hay scripts demo que suban assets detectados. | Más probable: selección accidental, cuenta compartida/test, UID equivocado o reglas permisivas. |

### 5.4 Reglas Storage mínimas recomendadas (no aplicadas)

Si en Console no existen reglas estrictas, proponer un fix mínimo versionado antes de closed testing:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    function isSignedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return isSignedIn() && request.auth.uid == uid;
    }

    match /users/{uid}/profile/{fileName} {
      allow read: if isOwner(uid);
      allow create, update: if isOwner(uid)
        && fileName.matches('^avatar_[0-9]+\\.(jpg|jpeg|png|webp)$')
        && request.resource.size < 5 * 1024 * 1024
        && request.resource.contentType.matches('image/(jpeg|jpg|png|webp)');
      allow delete: if isOwner(uid);
    }

    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

Notas:

- Decidir si `read` debe ser owner-only o autenticado. Si la app muestra avatares en comunidad/feed futuro, puede requerir lectura autenticada o URLs firmadas; hoy el uso auditado es perfil propio.
- Si se usa download URL con token como `photoUrl`, cualquier persona con la URL puede ver el objeto por token. Esto no equivale a listado público, pero sí a URL compartible.
- Añadir `storage.rules` y `firebase.json.storage.rules` en el mismo cambio, con tests de emulador si es posible.

### 5.5 Cómo investigar el origen de una foto concreta

Para un objeto `users/{uid}/profile/avatar_....jpg`:

1. En Firebase Console / Google Cloud Storage, abrir metadata del objeto:
   - `name/path`,
   - `bucket`,
   - `size`,
   - `contentType`,
   - `timeCreated`,
   - `updated`,
   - `md5Hash`/`crc32c`,
   - custom metadata si existe,
   - download tokens (`firebaseStorageDownloadTokens`) si visible.
2. Confirmar si el path UID coincide con el Auth UID del usuario que reclama no haber subido la foto.
3. Comparar `timeCreated` con:
   - `users/{uid}/profile/current.updatedAt` / `updatedAtEpochMillis`,
   - logs de Functions si `saveUserProfile` se invocó cerca,
   - Auth `last sign-in`,
   - eventos de app/testing.
4. Buscar otros objetos en `users/{uid}/profile/`:
   - si hay varios `avatar_` cercanos, puede ser reintentos o cambios de avatar,
   - si hay objetos con nombres fuera de patrón, sospechar reglas permisivas o tooling externo.
5. Revisar si la URL `photoUrl` actual apunta exactamente a ese path.
6. No borrar inmediatamente; primero descargar metadata/captura para trazabilidad.

---

## 6. Revisión de reglas Firestore/Storage

### 6.1 Firestore Rules

Estado observado:

- Helpers `isAuthenticated()` e `isOwner(uid)` están definidos.
- Horóscopos daily/weekly/monthly permiten read a usuarios autenticados y niegan writes.
- `users/{uid}/profile/current`, `birthEssence/current`, `dailyRitual/current`, `habits/current` permiten read/write solo al owner y validan campos.
- `users/{uid}/tarotDeckProgress/{trackId}` y `unlockedCards` son read/list owner-only y write denied.
- `userAccountStatus/{uid}` permite `get` owner-only, niega list/write.
- `usernames/{username}` permite read autenticado y niega write.
- `match /{document=**}` niega todo lo demás.

Conclusión: **Firestore Rules son razonablemente estrictas para cliente**. Las colecciones backend-owned de economía, premium, push raw token y purchase indexes no están abiertas al cliente por rules.

Riesgos/remarques:

- `profile/current.photoUrl` permite cualquier `https://...`, no solo Firebase Storage propio. Esto es flexible para Google profile photos, pero permite que el cliente guarde URL externa. Si se quiere limitar avatares propios, habría que endurecer con allowlist o callable backend.
- La lectura autenticada de `usernames` puede permitir enumeración limitada de usernames; aceptable para availability checks, pero revisar si se considera dato sensible.
- Los horóscopos son legibles por cualquier usuario autenticado; si algún contenido de futuro/premium no debería leerse sin unlock, la protección debe estar en callable/economía/UI o rules específicas. Actualmente rules no validan entitlement para leer horóscopos.

### 6.2 Storage Rules

Estado observado:

- **No hay archivo `storage.rules` versionado**.
- **`firebase.json` no configura rules de Storage**.
- No existe doc `docs/data/storage/*`.

Conclusión: **bloqueante operativo** hasta confirmar reglas reales del bucket. El repositorio no garantiza owner-check, MIME type, tamaño ni deny-by-default para Storage.

---

## 7. Checklist manual en Firebase Console / Google Cloud

### 7.1 Antes de closed testing

#### Storage

- [ ] Abrir Firebase Console → Storage → Rules y copiar las reglas actuales en una nota interna.
- [ ] Verificar `match /users/{uid}/profile/{fileName}` con `request.auth.uid == uid`.
- [ ] Verificar deny-by-default para el resto.
- [ ] Verificar límite `request.resource.contentType` a `image/jpeg|png|webp`.
- [ ] Verificar límite de tamaño recomendado: 5 MB inicial (o menos si se comprime cliente).
- [ ] Verificar que no existe `allow read, write: if true` ni `if request.auth != null` global sin owner-check.
- [ ] Para fotos inesperadas, exportar metadata antes de tocar objetos.

#### Auth

- [ ] Localizar `test@test.com` y `test@example.com`.
- [ ] Registrar UID, provider, created, last login, disabled, email verified.
- [ ] Confirmar si creation/last login coincide con Play pre-launch/internal testing.
- [ ] Deshabilitar primero si son desconocidos.
- [ ] Observar 7-14 días antes de borrar.

#### Firestore por UID sospechoso

- [ ] Buscar docs `users/{uid}/...`.
- [ ] Buscar economía: balances, ledger, requests, usage, unlocks, entitlements, purchase indexes.
- [ ] Buscar Oracle/Tarot history.
- [ ] Buscar push tokens y notification sends.
- [ ] No borrar docs económicos/premium aunque se borre Auth, sin plan de anonimización.

#### Retención/TTL

- [ ] Confirmar qué pantallas muestran historial Oracle/Tarot.
- [ ] Confirmar si horóscopos antiguos/futuros comprados deben permanecer accesibles.
- [ ] Elegir campo TTL (`expiresAt`) por colección; Firestore TTL requiere campo timestamp en cada doc.
- [ ] Hacer dry-run con conteos por colección antes de activar TTL.
- [ ] Excluir ledger/entitlements/purchase indexes/balances.

---

## 8. Acciones recomendadas antes de closed testing

### Bloqueante antes de closed testing

1. **Auditar reglas reales de Firebase Storage en Console**.
2. Si no son estrictas, implementar cambio mínimo versionado:
   - crear `storage.rules`,
   - añadir `firebase.json.storage.rules`,
   - owner-only para `users/{uid}/profile/avatar_*`,
   - MIME image-only,
   - tamaño máximo,
   - deny-by-default.
3. Probar con emulador o Rules Playground:
   - UID A puede subir/leer/borrar `users/A/profile/avatar_...jpg`,
   - UID A no puede escribir/leer `users/B/profile/...`,
   - no imagen rechazada,
   - >5 MB rechazado,
   - path fuera de patrón rechazado.

### Recomendable antes de closed testing

1. Deshabilitar temporalmente usuarios Auth desconocidos si no pertenecen a QA.
2. Registrar evidencia de esos usuarios y buscar UID en Firestore/Storage.
3. Definir política TTL inicial para:
   - `llmUsageDaily`: 90 días,
   - `pushNotificationSends`: 30/60 días,
   - `oracleRequests` legacy terminales: 60/90 días,
   - `oracleAnswers`/`tarotReadings`: 90 días si no hay UI de historial,
   - `horoscopeDaily`: 90 días si no hay histórico.
4. Crear doc `docs/data/storage/rules.md` cuando se versionen reglas.

### Puede esperar post-launch

1. Job cleanup de avatares huérfanos por uid con dry-run.
2. Compresión/resizing cliente antes de upload para reducir tamaño.
3. Dashboard mensual de coste por colección.
4. Política formal de retención/anonymization para cierre de cuenta.

### Solo documentación

1. Documentar contrato Storage (`users/{uid}/profile/avatar_<epoch>.<ext>`).
2. Documentar TTL por colección en `docs/data/firestore/schema.md` cuando se implemente.
3. Añadir changelog interno si se implementan reglas/TTL.

---

## 9. Prompts futuros para implementar TTL/cleanup

### 9.1 Prompt: versionar Storage Rules mínimo

```text
Implementa reglas Storage mínimas para avatares antes de closed testing.

Alcance:
- Crear storage.rules.
- Añadir configuración storage.rules en firebase.json.
- Permitir solo owner auth en users/{uid}/profile/avatar_<epoch>.(jpg|jpeg|png|webp).
- Limitar contentType a image/jpeg, image/png, image/webp.
- Limitar tamaño a 5 MB.
- Deny-by-default.
- Actualizar docs/data/storage/rules.md y changelog.

No tocar UI ni economía. No deploy.
Añadir tests o instrucciones de Rules Playground/emulator.
```

### 9.2 Prompt: preparar TTL de logs no económicos

```text
Prepara implementación TTL para colecciones no económicas de bajo riesgo.

Alcance:
- Añadir campo expiresAt server-side a nuevos docs en llmUsageDaily, pushNotificationSends y oracleRequests legacy terminales.
- No tocar economyBalances, ledger, purchaseTokenIndex, userEntitlements ni economyUnlocks.
- Documentar en docs/data/firestore/schema.md y docs/data/firestore/rules.md si aplica.
- Añadir script dry-run que cuente candidatos antiguos sin borrar.
- No activar TTL ni borrar datos.
```

### 9.3 Prompt: dry-run de avatares huérfanos

```text
Crear herramienta admin dry-run para detectar avatares huérfanos.

Alcance:
- Listar Storage users/{uid}/profile/avatar_*.
- Leer users/{uid}/profile/current.photoUrl.
- Reportar objetos que no coinciden con photoUrl actual y tienen más de N días.
- No borrar objetos.
- Output CSV/JSON con uid, path, createdAt, size, contentType.
- Documentar protocolo de revisión manual.
```

### 9.4 Prompt: auditoría de usuarios Auth desconocidos

```text
Crear checklist/script admin read-only para usuarios Auth sospechosos.

Input: lista de emails.
Output: uid, providerData, createdAt, lastSignIn, disabled, y conteos de Firestore/Storage por rutas conocidas.
Restricciones: read-only, no borrar, no deshabilitar, no deploy.
Documentar cómo usarlo con credenciales seguras locales/emulador.
```

---

## 10. Decisión recomendada

No pasar a Closed Testing hasta completar al menos la verificación de **Storage Rules reales**. Si las reglas ya son estrictas en Console, versionarlas en repo es altamente recomendado antes del siguiente deploy. Si son permisivas, el fix debe considerarse mínimo, seguro y prioritario.
