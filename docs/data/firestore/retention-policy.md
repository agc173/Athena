# Firestore retention and cleanup policy (audit/design, no deletion)

> Estado: diseño/auditoría previa a Google Play Closed Testing.
> Esta política **no activa TTL**, **no borra documentos**, **no despliega Functions** y **no cambia reglas**. Cualquier borrado futuro debe pasar primero por dry-run, revisión de producto/soporte/economía y rollout explícito.

## Resumen ejecutivo

La auditoría detecta tres familias de datos con crecimiento por tiempo o request que conviene controlar antes de producción abierta:

1. **Contenido generado por período**: `horoscopeDaily`, `horoscopeWeekly`, `horoscopeMonthly` crecen por fecha/semana/mes, signo e idioma. La UI actual lee el período solicitado, no un histórico completo; por tanto son buenos candidatos a TTL conservador cuando exista `expiresAt` y haya dry-run de conteos.
2. **Historial LLM y respuestas por usuario/request**: `oracleAnswers`, `tarotReadings`, `oracleRequests`, `economyRequests` y `llmUsageDaily` pueden crecer con cada pregunta/lectura o día operativo. Se recomienda distinguir historial visible/replay/idempotencia de datos puramente operativos antes de borrar.
3. **Logs operativos de push y uso**: `users/{uid}/pushNotificationSends`, `llmUsageDaily` y usage/counter docs por día/semana/mes acumulan trazas. Son candidatos a retención corta o media, siempre sin tocar tokens activos ni preferencias.

Recomendación para **Closed Testing**:

- **Antes de Closed Testing**: no activar TTL ni limpieza real; añadir únicamente observabilidad/dry-run read-only en entorno admin si se necesita medir volumen; documentar exclusiones críticas y revisar que nuevos writes puedan añadir `expiresAt` más adelante sin romper clientes.
- **Post-launch / después de datos reales**: implementar `expiresAt` en writes backend-owned, ejecutar dry-run por colección durante al menos una semana, validar conteos/costes, y recién entonces activar TTL nativo o scheduled cleanup por rutas de bajo riesgo.

## Alcance y archivos auditados

Se revisaron las fuentes de verdad y los puntos de lectura/escritura siguientes:

- Documentación Firestore: `docs/data/firestore/schema.md`, `docs/data/firestore/rules.md`, `docs/data/firestore/indexes.md`, `docs/data/firestore/examples.md`.
- Reglas fuente: `firestore.rules`.
- Generadores y paths de horóscopos: `functions/src/firestore/paths.ts`, `functions/src/generators/HoroscopeGenerator.ts`, `functions/src/generators/PeriodHoroscopeGenerator.ts`, `functions/src/admin/backfillPeriodHoroscopesCore.ts`.
- Oracle/Tarot legacy y Economy V2: `functions/src/oracle/callables/oracleAsk.ts`, `functions/src/oracle/callables/tarotDraw.ts`, `functions/src/oracle/firestore/paths.ts`, `functions/src/economy/*.ts`, `functions/src/economy/firestorePaths.ts`, `functions/src/economy/processingWatchdog.ts`.
- Premium/economía crítica: `functions/src/premium/service.ts`, `functions/src/economy/premiumStatus.ts`, `functions/src/economy/deckProgress.ts`.
- Push: `functions/src/notifications/callables/registerPushToken.ts`, `functions/src/notifications/callables/unregisterPushToken.ts`, `functions/src/notifications/schedulers/sendDailyHoroscopeNotifications.ts`.
- Cliente KMP/Data: `shared/data/src/commonMain/kotlin/com/agc/bwitch/data/astrology/horoscope/SyncHoroscopeDailyRepository.kt`, `shared/data/src/commonMain/kotlin/com/agc/bwitch/data/tarot/FirestoreTarotDeckCollectionRepository.kt`, `shared/data/src/commonMain/kotlin/com/agc/bwitch/data/userprofile/SyncUserProfileRepository.kt`, `shared/data/src/commonMain/kotlin/com/agc/bwitch/data/rituals/*`, `shared/data/src/commonMain/kotlin/com/agc/bwitch/data/account/FunctionsAccountDeletionRepository.kt`.

## Mapa de colecciones auditadas

| Ruta | Crecimiento | Quién escribe | Quién lee / uso real | UI actual | Criticidad | Backend-owned | Clasificación |
|---|---:|---|---|---|---|---|---|
| `/horoscopeDaily/{dateIso}/signs/{sign}` | Fecha × signo | Functions `HoroscopeGenerator`; backfill/admin | Cliente KMP como fallback legacy | Sí, día solicitado; no histórico global | Media: contenido regenerable pero visible | Sí | TTL posible, conservador |
| `/horoscopeDaily/{dateIso}/signs/{sign}/langs/{lang}` | Fecha × signo × idioma | Functions `HoroscopeGenerator`; traducciones/backfill | Cliente KMP prioriza idioma solicitado | Sí, día solicitado | Media | Sí | TTL posible, conservador |
| `/horoscopeWeekly/{weekKey}/signs/{sign}` | Semana × signo | Functions `PeriodHoroscopeGenerator`; backfill | Cliente KMP semanal, fallback para traducción | Sí, semana solicitada | Media | Sí | TTL posible, conservador |
| `/horoscopeWeekly/{weekKey}/signs/{sign}/langs/{lang}` | Semana × signo × idioma | Functions `PeriodHoroscopeGenerator`; backfill | Cliente KMP semanal por idioma | Sí | Media | Sí | TTL posible, conservador |
| `/horoscopeMonthly/{monthKey}/signs/{sign}` | Mes × signo | Functions `PeriodHoroscopeGenerator`; backfill | Cliente KMP mensual, fallback para traducción | Sí, mes solicitado | Media | Sí | TTL posible, conservador |
| `/horoscopeMonthly/{monthKey}/signs/{sign}/langs/{lang}` | Mes × signo × idioma | Functions `PeriodHoroscopeGenerator`; backfill | Cliente KMP mensual por idioma | Sí | Media | Sí | TTL posible, conservador |
| `/oracleAnswers/{uid}/items/{requestId}` | Usuario × request | `oracleAsk` cuando Economy V2 completa respuesta | No se encontró lectura cliente de historial; respuesta vuelve por callable | No visible como historial | Media: contiene pregunta/respuesta | Sí | TTL posible si no hay historial visible |
| `/tarotReadings/{uid}/items/{readingId}` | Usuario × request | `tarotDraw` tras lectura exitosa | No se encontró lectura cliente de historial; respuesta vuelve por callable | No visible como historial | Media: contiene pregunta/lectura | Sí | TTL posible si no hay historial visible |
| `/oracleRequests/{requestId}` | Request legacy global | `tarotDraw` legacy cuando Economy V2 está OFF | `tarotDraw` reusa para idempotencia/replay y evita requestId duplicado | Indirecta por retry/callable | Alta mientras legacy esté activo | Sí | Solo dry-run por ahora; TTL terminal futuro |
| `/economyRequests/{uid}/requests/{requestId}` | Usuario × request | Economy callables (`claim*`, tarot/oracle/birth essence, horoscope unlock, purchases) | Callables reusan para idempotencia/replay; watchdog consulta `collectionGroup('requests')` en `PROCESSING` | Indirecta por retry/callables | Alta: economía, refunds, soporte | Sí | Dry-run únicamente; TTL futuro muy conservador solo terminales seleccionados |
| `/economyRequests/{uid}` | Usuario | Economy completion marca `updatedAt` | Operativo por usuario | No | Media | Sí | No TTL directo; padre puede quedar como metadata |
| `/llmUsageDaily/{dateIso}/scopes/{scope}` | Día × scope | `reserveLlmCallOrThrow` / `addLlmTokens` | Backend usa el día actual para caps | No | Alta para día actual; baja histórico | Sí | TTL posible 90 días |
| `/llmUsageDaily/{dateIso}/providers/{provider}` | Día × proveedor | `oracleAsk` / `tarotDraw` tracking best-effort | Soporte/coste/debug | No | Baja-media | Sí | TTL posible 90 días |
| `/users/{uid}/pushNotificationSends/{dateIso_daily_horoscope}` | Usuario × día/campaña | Scheduler `sendDailyHoroscopeNotifications` | Scheduler usa create-idempotente para no duplicar el día | No | Media para idempotencia reciente | Sí | TTL posible 30-60 días |
| `/users/{uid}/pushTokens/{tokenHash}` | Usuario × dispositivo/token | Push register/unregister; scheduler actualiza éxitos/fallos | Scheduler consulta `collectionGroup('pushTokens')`; cliente/backend sincronizan token | Indirecta ajustes push | Alta: envío y privacidad | Sí | Cleanup manual posible solo inválidos; no TTL genérico |
| `/pushTokenIndex/{tokenHash}` | Token activo/global | Push register/unregister | Register/unregister resuelve ownership | No | Alta: evitar token compartido | Sí | No tocar mientras token activo; cleanup manual huérfanos solo con dry-run |
| `/oracleUserDaily/{dateIso}/users/{uid}` | Día × usuario | `tarotDraw` legacy quotas | `tarotDraw` legacy lee/actualiza cuotas del día | Indirecta | Media; solo día actual/retry | Sí | TTL posible si legacy sigue, después de ventana de soporte |
| `/economyUsageDaily/{dateIso}/users/{uid}` | Día × usuario | Economy resolvers/callables | Economy decide cuotas diarias | No directa | Alta para día actual; media histórico | Sí | TTL posible futuro conservador, nunca antes de auditoría |
| `/economyUsageWeekly/{weekKey}/users/{uid}` | Semana × usuario | Economy resolvers/callables | Economy decide cuotas semanales | No directa | Alta para semana actual | Sí | TTL posible futuro conservador |
| `/economyUsageMonthly/{monthKey}/users/{uid}` | Mes × usuario | Economy resolvers/callables | Economy decide cuotas mensuales | No directa | Alta para mes actual | Sí | TTL posible futuro conservador |
| `/economyBalances/{uid}` | Usuario | Economy callables | Economy status/balance | Sí/indirecta | Crítica: saldo | Sí | **No tocar** |
| `/economyBalances/{uid}/ledger/{entryId}` | Usuario × movimiento | Economy spends/refunds/claims | Auditoría de saldo/refunds/soporte | Indirecta | Crítica: ledger financiero | Sí | **No TTL** |
| `/purchaseTokenIndex/{hash}` | Compra/token | Premium/Moon Pack claim | Validación ownership compra | No | Crítica: compras/refunds | Sí | **No tocar** |
| `/userEntitlements/{uid}` | Usuario | Premium service | Backend/client premium status | Sí/indirecta | Crítica: acceso premium | Sí | **No TTL** |
| `/economyUnlocks/{uid}/horoscope/{unlockKey}` | Usuario × unlock | Horoscope unlock callables | Unlocks actuales/futuros; mantiene acceso comprado/desbloqueado | Sí/indirecta | Crítica: acceso desbloqueado | Sí | **No TTL** |
| `/users/{uid}/tarotDeckProgress/{trackId}` | Usuario × track | Deck progress economy | Cliente lee progreso de colección | Sí | Crítica: progreso/recompensas | Sí | **No TTL** |
| `/users/{uid}/tarotDeckProgress/{trackId}/unlockedCards/{cardId}` | Usuario × carta | Deck progress economy | Cliente lee cartas desbloqueadas | Sí | Crítica: acceso desbloqueado | Sí | **No TTL** |
| `/users/{uid}/tarotDeckProgressRequests/{requestId}` | Usuario × request de progreso | Deck progress economy | Idempotencia de grants | Indirecta | Alta: evita doble grant | Sí | No tocar por ahora; posible retención muy larga post-auditoría |
| `/economyLifetime/{uid}` | Usuario | Economy | Flags lifetime | Indirecta | Alta: evita duplicar beneficios lifetime | Sí | **No TTL** |
| `/users/{uid}/profile/current`, `/users/{uid}`, `/usernames/{name}` | Usuario | Cliente/callable perfil | Cliente perfil/username | Sí | Crítica identidad/perfil | Mixto/backend | **No TTL** |
| `/users/{uid}/birthEssence/current` | Usuario single doc | Cliente sync/callable birth essence | Cliente carta activa | Sí | Contenido activo usuario | Mixto | No TTL general |
| `/users/{uid}/dailyRitual/current`, `/users/{uid}/habits/current` | Usuario single doc | Cliente sync | Cliente estado actual | Sí | Estado actual | Mixto | No TTL general |
| `/ritualCategories/{categoryId}`, `/rituals/{ritualId}` | Catálogo | Admin/backend | Cliente catálogo | Sí | Catálogo | Backend/admin | No TTL; lifecycle editorial |
| `/posts`, `/comments`, `/reactions` | Comunidad | Futuro/placeholder | No auditado como feature activa | Potencial | Privacidad/moderación | Mixto | Fuera de cleanup automático hasta diseño de comunidad |
| `/userAccountStatus/{uid}` | Usuario | Account callables | Cliente bootstrap restore | Sí/indirecta | Alta: recuperación cuenta | Sí | No TTL en esta política; pertenece a borrado cuenta fase 2 |

## Tabla de retención recomendada

| Ruta / familia | Retención inicial conservadora | Estado recomendado | Condiciones antes de aplicar |
|---|---:|---|---|
| `horoscopeDaily` canonical + `langs` | 90 días | TTL posible | Añadir `expiresAt`; dry-run por `createdAtEpochMillis`/docId; confirmar que UI no permite navegar a días vencidos sin fallback claro. |
| `horoscopeWeekly` canonical + `langs` | 180 días | TTL posible | Añadir `expiresAt`; dry-run por `weekKey`; comprobar que semanas antiguas no son producto desbloqueado persistente. |
| `horoscopeMonthly` canonical + `langs` | 365 días | TTL posible | Añadir `expiresAt`; dry-run por `monthKey`; confirmar que meses antiguos no son historial comprado visible. |
| `oracleAnswers/{uid}/items` | 90 días si no hay historial visible | TTL posible | Confirmar con producto que no habrá historial visible/restaurable; dry-run por `createdAt`; evaluar privacidad. |
| `tarotReadings/{uid}/items` | 90 días si no hay historial visible | TTL posible | Confirmar que `SettingsLastTarotReadingRepository` local no depende de Firestore para restaurar historial; dry-run por `createdAt`. |
| `llmUsageDaily` scopes/providers | 90 días | TTL posible | Mantener siempre día actual; dashboard/coste debe exportar agregados si requiere histórico mayor. |
| `users/{uid}/pushNotificationSends` | 30-60 días | TTL posible | Mantener ventana suficiente para idempotencia diaria, soporte de campaña y retries; no tocar `pushTokens`. |
| `oracleRequests` legacy terminales | 60-90 días | Solo dry-run por ahora | Solo `COMPLETED_SUCCESS`/`FAILED`; no borrar `PROCESSING` sin watchdog; revisar si legacy sigue activo. |
| `oracleUserDaily` legacy | 60-90 días | Cleanup/TTL posible futuro | Solo fechas cerradas; verificar que no hay retries que dependan de cuotas antiguas. |
| `economyRequests` terminales | 180-365 días, no activar aún | Solo dry-run por ahora | Excluir purchases/moon-pack/refunds o conservar mayor plazo; nunca borrar `PROCESSING`; revisar soporte/idempotencia. |
| `economyUsageDaily/Weekly/Monthly` | 180-365 días, no activar aún | Solo dry-run por ahora | Garantizar que período actual no se toca; revisar disputes, soporte y analytics. |
| `pushTokens` inválidos | Sin TTL genérico | Cleanup manual posible | Solo tokens `enabled=false` + `invalidatedAt` antiguo + sin índice activo; dry-run obligatorio. |
| Ledger/compras/premium/unlocks/progreso/perfil | Indefinido | No tocar | Excluidos por política. |

## Rutas que no se deben borrar

Quedan explícitamente fuera de cualquier TTL/cleanup automático de esta política:

- `economyBalances/{uid}`.
- `economyBalances/{uid}/ledger/{entryId}`.
- `purchaseTokenIndex/{hash}`.
- `userEntitlements/{uid}` y cualquier estado premium vigente o histórico necesario para restauración.
- `economyUnlocks/{uid}/horoscope/{unlockKey}`.
- Unlocks comprados/desbloqueados, incluyendo cartas de Tarot desbloqueadas.
- `users/{uid}/tarotDeckProgress/{trackId}`.
- `users/{uid}/tarotDeckProgress/{trackId}/unlockedCards/{cardId}`.
- `economyLifetime/{uid}`.
- Perfil de usuario: `users/{uid}`, `users/{uid}/profile/current`, `usernames/{normalizedUsername}`.
- Datos necesarios para restaurar compras, auditar cargos/refunds, mantener acceso del usuario o reconstruir saldo.

## Riesgos por propuesta

### TTL de horóscopos periódicos

- **Coste**: reduce crecimiento lineal por fecha/signo/idioma; riesgo bajo de lecturas extra si hay fallback/regeneración posterior.
- **Privacidad**: bajo; contenido no específico por usuario.
- **UI**: medio si la UI permite consultar fechas/semanas/meses antiguos y espera contenido remoto.
- **Economía/idempotencia**: medio para períodos desbloqueados: borrar contenido de un período que el usuario desbloqueó rompería el valor del unlock aunque el unlock persista.
- **Acceso comprado**: medio para weekly/monthly/daily future si están protegidos por unlock; no activar TTL hasta definir ventana de acceso al contenido desbloqueado.
- **Soporte/debug**: bajo-medio; conservar 90/180/365 días ayuda a investigar generación reciente.

### TTL de `oracleAnswers` y `tarotReadings`

- **Coste**: reduce documentos por usuario/request y payloads LLM.
- **Privacidad**: favorable; preguntas/respuestas pueden contener texto sensible.
- **UI**: alto si se añade historial visible/restaurable; hoy no se encontró UI de historial Firestore.
- **Economía/idempotencia**: bajo-medio; `economyRequests`/`oracleRequests` conservan replay/idempotencia, pero respuestas podrían estar duplicadas en `responsePayload`.
- **Acceso comprado**: medio si una lectura pagada debe poder revisitarse; requiere decisión de producto.
- **Soporte/debug**: medio-alto; borrar limita análisis de incidentes LLM.

### TTL de `llmUsageDaily`

- **Coste**: bajo volumen relativo, pero crecimiento diario constante.
- **Privacidad**: bajo; agregados sin pregunta raw.
- **UI**: nulo.
- **Economía/idempotencia**: alto solo para el día actual porque se usa para caps; histórico bajo.
- **Acceso comprado**: nulo.
- **Soporte/debug**: medio; coste/latencia por proveedor se pierde tras 90 días si no se exporta.

### TTL de `pushNotificationSends`

- **Coste**: crecimiento usuario × día; TTL 30-60 días contiene volumen.
- **Privacidad**: favorable; reduce trazas de campañas por usuario.
- **UI**: nulo.
- **Economía/idempotencia**: bajo fuera del día/campaña actual; alto si se borra el mismo día porque permite duplicados.
- **Acceso comprado**: nulo.
- **Soporte/debug**: medio; limitará investigación de entregas antiguas.

### Cleanup de `oracleRequests` legacy

- **Coste**: reduce request docs globales antiguos.
- **Privacidad**: favorable porque contiene pregunta truncada y payload/error.
- **UI**: indirecto por retry/replay.
- **Economía/idempotencia**: alto si se borra un requestId todavía reusable por cliente; solo terminales antiguos.
- **Acceso comprado**: bajo-medio según intent (`SUBSCRIPTION`/`AD_UNLOCK`).
- **Soporte/debug**: medio.

### Dry-run de `economyRequests` y usage económico

- **Coste**: potencialmente relevante a escala.
- **Privacidad**: medio; puede contener pregunta truncada/response payload y pruebas de ads.
- **UI**: indirecto por callables.
- **Economía/idempotencia**: alto; estos documentos protegen contra doble gasto/doble grant y soportan refunds/timeouts.
- **Acceso comprado**: alto si se borra request asociado a compra/unlock/reward.
- **Soporte/debug**: alto; conservar 180-365 días mínimo y no activar TTL aún.

## Diseño técnico futuro (sin aplicar)

### Campo TTL propuesto

Usar `expiresAt: Timestamp` como campo TTL nativo en documentos backend-owned que sean seguros para expirar. No reutilizar `updatedAt` ni `createdAt` como campo TTL porque:

- `updatedAt` se mueve en retries/compensaciones y podría extender o acortar retención accidentalmente.
- `createdAt` puede ser requerido para auditoría y no expresa intención de borrado.
- `expiresAt` permite opt-in por documento y facilita excluir documentos críticos.

Para documentos con `createdAtEpochMillis` numérico (horóscopos), añadir `expiresAt` como `Timestamp` además de conservar los campos existentes por compatibilidad cliente.

### Writes que deberían añadir `expiresAt` más adelante

- `HoroscopeGenerator` y `PeriodHoroscopeGenerator`: calcular `expiresAt` por tipo (`+90d`, `+180d`, `+365d`) al crear/reparar docs canonical y `langs`.
- `oracleAsk`: añadir `expiresAt = createdAt + 90d` en `oracleAnswers/{uid}/items/{requestId}` si producto confirma que no hay historial persistente.
- `tarotDraw`: añadir `expiresAt = createdAt + 90d` en `tarotReadings/{uid}/items/{readingId}` bajo la misma condición.
- `usageDaily.ts`, `oracleAsk`, `tarotDraw`: añadir `expiresAt = dateIso + 90d` en `llmUsageDaily/{dateIso}/scopes|providers/*`.
- `sendDailyHoroscopeNotifications`: añadir `expiresAt = sentAt/dateIso + 30-60d` en `pushNotificationSends`.
- `oracleRequests` legacy: solo terminales (`COMPLETED_SUCCESS`, `FAILED`) y solo si legacy sigue necesitando una ventana acotada.
- `economyRequests`: **no añadir TTL inicial**; si se decide, usar TTL solo en terminales de bajo riesgo, con exclusiones por `type` y retención mínima 180-365 días.

### Colecciones que requieren dry-run antes de cleanup

- Todas las colecciones bajo `economyRequests`, `economyUsageDaily`, `economyUsageWeekly`, `economyUsageMonthly`.
- `oracleRequests` y `oracleUserDaily` legacy.
- `oracleAnswers` y `tarotReadings` por contener payloads de usuario.
- `pushTokens` inválidos u huérfanos.
- Cualquier colección group (`requests`, `items`, `pushNotificationSends`, `unlockedCards`) antes de operar a escala.

### Colecciones excluidas del TTL nativo

No configurar TTL en documentos críticos ni en sus subcolecciones de acceso/auditoría:

- `economyBalances`, `ledger`, `purchaseTokenIndex`, `userEntitlements`, `economyUnlocks`, `economyLifetime`.
- `tarotDeckProgress`, `unlockedCards`, `tarotDeckProgressRequests` hasta tener retención legal/producto explícita.
- `users/profile`, `usernames`, `userAccountStatus`, `pushTokenIndex`.

### Firestore TTL nativo vs scheduled cleanup Function

**Firestore TTL nativo** conviene para rutas simples en las que cada documento seguro tiene `expiresAt` y no requiere lógica de negocio: horóscopos, `llmUsageDaily`, `pushNotificationSends`, potencialmente `oracleAnswers`/`tarotReadings`.

**Scheduled cleanup Function** conviene cuando se requiere lógica condicional, exclusiones por estado/tipo o auditoría previa: `economyRequests`, legacy `oracleRequests`, tokens inválidos, usage económico. Debe ejecutarse primero en modo dry-run y después con batch pequeño, logs estructurados y allowlist explícita.

### Cómo auditar conteos antes de borrar

1. Ejecutar dry-run read-only por ruta/familia con `olderThanDays`, `limit` y filtros de estado.
2. Emitir JSON con: ruta, filtros, `candidateCount`, `sampleDocIds`, `oldestCreatedAt`, `newestCreatedAt`, `statusBreakdown`, `typeBreakdown`, `estimatedBytes` si se puede calcular.
3. Guardar salida en un artefacto operativo fuera del cliente, no en Firestore productivo salvo que exista colección admin auditada.
4. Revisar muestras manualmente antes de activar TTL o cleanup.
5. Comparar conteos durante varios días para detectar documentos `PROCESSING` atascados o crecimiento inesperado.

## Propuesta de script dry-run read-only

No se implementa script en esta iteración para evitar añadir tooling admin sin validar credenciales/colecciones reales. El diseño seguro es:

- Ubicación futura sugerida: `functions/scripts/firestoreRetentionDryRun.ts` o `functions/src/admin/firestoreRetentionDryRun.ts` si se integra con tooling admin existente.
- Debe usar Firebase Admin SDK con credenciales locales/ADC.
- Debe ser **read-only**: prohibido llamar `delete`, `update`, `set`, `writeBatch` o TTL APIs.
- Argumentos mínimos:
  - `--collection=<path|collectionGroup>`
  - `--olderThanDays=<number>`
  - `--limit=<number>`
  - opcional `--status=COMPLETED_SUCCESS,FAILED`
  - opcional `--type=...`
  - opcional `--field=createdAt|updatedAt|sentAt|createdAtEpochMillis|dateKey`
  - opcional `--format=json|table`

Ejemplos de uso futuro esperados:

```bash
npm --prefix functions run retention:dry-run -- --collectionGroup=pushNotificationSends --olderThanDays=60 --limit=100 --field=sentAt --format=table
npm --prefix functions run retention:dry-run -- --collectionGroup=items --parentCollection=tarotReadings --olderThanDays=90 --limit=100 --field=createdAt --format=json
npm --prefix functions run retention:dry-run -- --collectionGroup=requests --parentCollection=economyRequests --olderThanDays=365 --status=COMPLETED_SUCCESS,FAILED --limit=100 --field=updatedAt --format=json
```

Prompt futuro exacto para implementarlo:

> Implementa un script admin read-only de dry-run de retención Firestore en `functions/scripts/firestoreRetentionDryRun.ts`. Debe aceptar `collection` o `collectionGroup`, `olderThanDays`, `limit`, `field`, filtros opcionales `status`/`type`, emitir JSON y tabla, y fallar si algún código intenta borrar/escribir. No activar TTL, no hacer deploy, no borrar documentos. Añadir tests unitarios del parser y documentación de uso en `docs/data/firestore/retention-policy.md`.

## Checklist antes de activar TTL o cleanup real

- [ ] Confirmar con producto qué historiales serán visibles/restaurables (Oracle/Tarot/horóscopos desbloqueados).
- [ ] Confirmar con soporte/legal el plazo mínimo para economía, purchases, refunds y auditoría.
- [ ] Añadir `expiresAt` solo en writes backend-owned de colecciones allowlisted.
- [ ] No añadir `expiresAt` a rutas excluidas.
- [ ] Ejecutar dry-run read-only con muestras y breakdown por estado/tipo.
- [ ] Revisar que ningún candidato tenga `status=PROCESSING`, unlock comprado, ledger, entitlement o purchase token asociado.
- [ ] Validar índices necesarios para collection group dry-run sin afectar producción.
- [ ] Validar en emulator/staging con datos sintéticos.
- [ ] Publicar plan de rollback/observabilidad antes de cleanup real.
- [ ] Activar TTL nativo únicamente en colección/campo allowlisted y documentar fecha exacta.
- [ ] Después de activar, monitorizar errores de UI/callables, coste Firestore y tickets de soporte.

## Hallazgos por severidad

### Alta

- `economyRequests` y `economyBalances/*/ledger` están estrechamente ligados a idempotencia, refunds y saldo. Cualquier cleanup agresivo puede duplicar cargos/grants o romper auditoría. Acción: no TTL; solo dry-run terminal muy conservador en `economyRequests` cuando existan datos reales.
- `economyUnlocks`, `userEntitlements`, `purchaseTokenIndex` y progreso de mazos mantienen acceso comprado/desbloqueado. Acción: excluir permanentemente de TTL automático.
- `pushTokens` y `pushTokenIndex` contienen token raw/ownership para notificaciones. Acción: no TTL genérico; cleanup solo de inválidos con dry-run y validación del índice.

### Media

- Horóscopos por fecha/semana/mes crecerán linealmente por signo e idioma. Acción: preparar `expiresAt` futuro y TTL conservador, pero validar acceso a períodos desbloqueados.
- `oracleAnswers` y `tarotReadings` guardan contenido generado por usuario sin historial visible identificado. Acción: retención 90 días es razonable si producto confirma que no será historial restaurable.
- `pushNotificationSends` crece usuario × día y solo necesita idempotencia reciente. Acción: TTL 30-60 días después de añadir `expiresAt`.

### Baja

- `llmUsageDaily` es agregado operativo diario. Acción: TTL 90 días o exportación a analytics/coste antes de expirar.
- Catálogos (`rituals`, `ritualCategories`, `tarotDeckTracks`, `tarotDeckRewardPools`) no son crecimiento temporal de usuario; requieren lifecycle editorial, no TTL.

## Prompts futuros para implementar retención real

### 1. Añadir `expiresAt` a writes allowlisted

> Añade campo `expiresAt: Timestamp` solo a writes backend-owned allowlisted: horóscopos daily/weekly/monthly, `oracleAnswers`, `tarotReadings`, `llmUsageDaily` y `pushNotificationSends`. No modificar economía crítica, purchases, entitlements, unlocks, ledger, perfil ni progreso de mazos. No activar TTL ni borrar documentos. Actualiza schema/docs y tests.

### 2. Implementar cleanup programado con modo dry-run obligatorio

> Implementa una scheduled/admin Function de cleanup con `dryRun=true` por defecto y allowlist explícita. Debe rechazar rutas críticas, filtrar solo documentos terminales cuando aplique, emitir logs JSON con conteos/muestras, soportar batch pequeño y no borrar nada salvo que `dryRun=false` y `ALLOW_RETENTION_DELETE=true`. No hacer deploy.

### 3. Activar TTL nativo en una ruta concreta

> Prepara instrucciones y checklist para activar Firestore TTL nativo en `<ruta>` usando campo `expiresAt`. No activar en producción desde el código. Incluir preflight dry-run, métricas, plan de rollback y actualización de `docs/data/firestore/retention-policy.md` con fecha/alcance.
