# Firestore schema (propuesta inicial)

> Source of truth para colecciones y campos.
> Si se cambian colecciones/campos, actualizar este documento.

## Colecciones

### /horoscopeDaily/{dateIso}/signs/{sign}
Documento legacy de horóscopo diario por signo (single-language, históricamente español).

Campos:
- text: string
- mood: string
- luckyNumber: number
- luckyColor: string
- shareText: string (opcional)
- updatedAtEpochMillis: number
- createdAtEpochMillis: number (opcional)
- generatorVersion: number (opcional)
- llmProvider: string (opcional)

Notas:
- Mantener como fallback de compatibilidad para clientes/cron que aún no usan subruta por idioma.

### /horoscopeDaily/{dateIso}/signs/{sign}/langs/{lang}
Documento canónico de horóscopo diario por signo e idioma.

Campos:
- languageCode: string (ISO corto, ej. `es`, `en`, `pt`)
- text: string
- mood: string
- luckyNumber: number
- luckyColor: string
- shareText: string (opcional)
- updatedAtEpochMillis: number
- createdAtEpochMillis: number (opcional)
- generatorVersion: number (opcional)
- llmProvider: string (opcional)

Notas:
- El cliente debe priorizar esta ruta por idioma.
- Solo si no existe documento en `langs/{lang}`, puede hacer fallback controlado al doc legacy sin idioma.

### /horoscopeWeekly/{weekKey}/signs/{sign}
Documento canónico semanal por signo en español.

Campos:
- sign: string (enum `ZodiacSign`, obligatorio)
- weekKey: string (ISO week `YYYY-Www`, obligatorio)
- languageCode: string (`es`, obligatorio)
- title: string
- overview: string
- loveAndRelationships: string
- workAndMoney: string
- spiritualEnergy: string
- weeklyAdvice: string
- mantra: string
- shareText: string
- createdAtEpochMillis: number
- updatedAtEpochMillis: number
- generatorVersion: number
- llmProvider: string

Notas:
- Escritura backend write-once (scheduler semanal).
- Fuente para traducciones en subcolección `langs`.

### /horoscopeWeekly/{weekKey}/signs/{sign}/langs/{lang}
Documento semanal traducido por idioma.

Campos:
- sign: string (enum `ZodiacSign`, obligatorio)
- weekKey: string (ISO week `YYYY-Www`, obligatorio)
- languageCode: string (ISO corto `es|en|pt|ru|fr|it|de`)
- title: string
- overview: string
- loveAndRelationships: string
- workAndMoney: string
- spiritualEnergy: string
- weeklyAdvice: string
- mantra: string
- shareText: string
- createdAtEpochMillis: number
- updatedAtEpochMillis: number
- generatorVersion: number
- llmProvider: string

### /horoscopeMonthly/{monthKey}/signs/{sign}
Documento canónico mensual por signo en español.

Campos:
- sign: string (enum `ZodiacSign`, obligatorio)
- monthKey: string (`YYYY-MM`, obligatorio)
- languageCode: string (`es`, obligatorio)
- title: string
- monthTheme: string
- loveAndRelationships: string
- workAndMoney: string
- personalGrowth: string
- ritualSuggestion: string
- mantra: string
- shareText: string
- createdAtEpochMillis: number
- updatedAtEpochMillis: number
- generatorVersion: number
- llmProvider: string

Notas:
- Escritura backend write-once (scheduler mensual).
- Fuente para traducciones en subcolección `langs`.

### /horoscopeMonthly/{monthKey}/signs/{sign}/langs/{lang}
Documento mensual traducido por idioma.

Campos:
- sign: string (enum `ZodiacSign`, obligatorio)
- monthKey: string (`YYYY-MM`, obligatorio)
- languageCode: string (ISO corto `es|en|pt|ru|fr|it|de`)
- title: string
- monthTheme: string
- loveAndRelationships: string
- workAndMoney: string
- personalGrowth: string
- ritualSuggestion: string
- mantra: string
- shareText: string
- createdAtEpochMillis: number
- updatedAtEpochMillis: number
- generatorVersion: number
- llmProvider: string

---

### /users/{userId}
Perfil del usuario y progreso.

Campos:
- username: string
- zodiacSign: string (enum en domain)
- birthDate: timestamp (opcional)
- birthTime: string "HH:mm" (opcional)
- birthPlace: map (opcional)
    - city: string
    - country: string
    - lat: number
    - lon: number
- level: number (default 1)
- xp: number (default 0)
- createdAt: timestamp
- updatedAt: timestamp

Notas:
- userId = uid auth (Firebase)
- username único garantizado mediante `/usernames/{normalizedUsername}`

---


### /users/{userId}/profile/current
Perfil consumido por la app cliente actual.

Campos:
- displayName: string (opcional)
- photoUrl: string (opcional)
- email: string (opcional)
- username: string (opcional)
- birthDate: string ISO `YYYY-MM-DD` (opcional)
- zodiacSign: string (enum en domain, opcional)
- birthEssenceSummary: string (opcional, resumen de la esencia activa)
- updatedAtEpochMillis: number

Notas:
- Campos opcionales para mantener compatibilidad con perfiles antiguos.
- Si `zodiacSign` no existe, puede derivarse desde `birthDate` en capa domain/presentation.

---




### /users/{userId}/birthEssence/current
Esencia natal activa del usuario (única carta activa en V1).

Campos:
- sunSign: string (enum ZodiacSign, obligatorio)
- moonSign: string (enum ZodiacSign, obligatorio)
- risingSign: string (enum ZodiacSign, obligatorio)
- interpretation: string (lectura breve generada por LLM)
- languageCode: string (ISO corto `es|en|pt|ru|fr|it|de`, opcional en docs legacy)
- archetype: string (opcional)
- savedAtEpochMillis: number
- updatedAtEpochMillis: number

Notas:
- Solo existe un documento activo (`current`) por usuario.
- Cada nuevo guardado reemplaza la esencia activa anterior.
- Si `languageCode` no existe (documentos legacy), el cliente usa fallback explícito a `es`.

---

### /users/{userId}/dailyRitual/current
Estado sincronizado del ritual diario (single doc activo por usuario).

Campos:
- selectedDateIso: string ISO `YYYY-MM-DD` (opcional)
- selectedTemplateId: string (opcional)
- selectedTheme: string (enum `DailyRitualTheme`, opcional)
- dailyCompletionDateIso: string ISO `YYYY-MM-DD` (opcional)
- dailyCompleted: boolean
- lastCompletedDateIso: string ISO `YYYY-MM-DD` (opcional)
- streakCount: number
- updatedAtEpochMillis: number

Notas:
- Merge cliente por `updatedAtEpochMillis` (last-write-wins).
- Documento: `users/{uid}/dailyRitual/current`.

---

### /users/{userId}/habits/current
Snapshot sincronizado de Hábitos (single doc activo por usuario).

Campos:
- todayDateIso: string ISO `YYYY-MM-DD`
- selectedIntentionIds: array<string>
- completedIntentionIds: array<string>
- progressPoints: number
- completedCycles: number
- updatedAtEpochMillis: number

Notas:
- Estrategia local-first: Settings sigue siendo la fuente inmediata para lectura.
- Merge cliente por `updatedAtEpochMillis` (last-write-wins).
- Documento: `users/{uid}/habits/current`.

---

### /usernames/{normalizedUsername}
Índice dedicado para unicidad real de username.

Campos:
- uid: string (owner del username)
- username: string (normalized username, igual al id del documento)
- updatedAt: timestamp

Notas:
- `normalizedUsername` se calcula como `trim + removePrefix("@") + lowercase`.
- Validación backend: 3-30 chars con regex `^[a-z0-9._]+$`.
- Se actualiza de forma transaccional junto a `/users/{uid}/profile/current` para reservar/cambiar/liberar username sin colisiones.

---

### /posts/{postId}
Post del feed de comunidad.

Campos:
- authorId: string (userId)
- text: string
- topic: string (opcional: astrology | tarot | energy | ritual | ...)
- createdAt: timestamp
- updatedAt: timestamp
- likeCount: number (denormalizado)
- commentCount: number (denormalizado)
- status: string (active | deleted | flagged)  // opcional para moderación

---



### /posts/{postId}/comments/{commentId}
Comentarios por post.

Campos:
- authorId: string
- text: string
- createdAt: timestamp
- status: string (active | deleted | flagged)

---



### /posts/{postId}/reactions/{userId}
Reacciones (like) por usuario (evita duplicados).

Campos:
- type: string (like)  // extensible
- createdAt: timestamp

---

## Catálogo de rituales

### /ritualCategories/{categoryId}
Catálogo de categorías de rituales para UI de exploración.

Campos:
- id: string (opcional; fallback al `categoryId`)
- type: string (`love` | `prosperity` | `protection` | `cleansing`)
- title: string
- subtitle: string
- sortOrder: number (opcional)
- isActive: boolean (default true)

### /rituals/{ritualId}
Catálogo de rituales versionado en backend (lectura pública).

Campos:
- id: string (opcional; fallback al `ritualId`)
- categoryId: string (opcional)
- categoryType: string (`love` | `prosperity` | `protection` | `cleansing`, opcional si viene `categoryId`)
- title: string
- subtitle: string
- intention: string
- materials: array<string>
- preparation: string (opcional)
- action: string
- closing: string
- optionalNote: string (opcional)
- materialsHint: string (opcional)
- sortOrder: number (opcional)
- isActive: boolean (default true)
- isPremium: boolean (default false)

---

## Colecciones futuras (placeholder)
### /readings/{readingId}
### /userContent/{userId}/saved/{itemId}

---

## Oracle backend (v1)

### /oracleSystemStatus/current
Estado operativo global del backend oracle.

Campos:
- mode: string (`NORMAL` | `DEGRADED` | `EMERGENCY`)
- updatedAt: timestamp (opcional)

### /userEntitlements/{uid}
Entitlements económicos del usuario. Documento backend-owned escrito por Cloud Functions.

Campos:
- isSubscriber: boolean (default false)
- status: string (`active` | `inactive`; fase Premium Google Play escribe `active` cuando Play valida una suscripción vigente)
- productId: string (opcional; fase actual solo `bwitch_premium_monthly`)
- basePlanId: string (opcional; fase actual solo `monthly`)
- source: string (opcional; fase actual `google_play`)
- purchaseTokenHash: string (opcional; SHA-256 del token, no token raw)
- packageName: string (opcional; fase actual `com.agc.bwitch`)
- updatedAt: timestamp (opcional)
- expiresAt: timestamp (opcional; cuando Google Play devuelve fecha de expiración)

Notas:
- Premium no debe activarse por estado local de Billing: requiere validación backend contra Google Play Developer API.
- El token raw no se documenta ni se almacena en este documento; `refreshEntitlement` actual lee el entitlement persistido y no revalida Play todavía.

### /purchaseTokenIndex/{hash}
Índice backend-owned para ownership de tokens Google Play (`hash` = SHA-256 del `purchaseToken`).

Campos:
- uid: string
- productId: string
- packageName: string
- updatedAt: timestamp

Notas:
- Cloud Functions rechaza activar una compra si el hash del token ya está asociado a otro `uid`.

### /oracleUserDaily/{dateIso}/users/{uid}
Cuotas diarias por usuario y día (timezone Europe/Madrid).

Campos:
- freeTarot1Remaining: number
- adUnlockRemaining: number
- maxRequestsRemaining: number
- tarot3Remaining: number
- createdAt: timestamp
- updatedAt: timestamp

### /oracleRequests/{requestId}
Control de idempotencia y estado de ejecución por request.

Campos:
- uid: string
- requestId: string
- requestType: string (`TAROT_1` | `TAROT_3`)
- lang: string
- topic: string (opcional)
- question: string (opcional, truncada)
- dateIso: string (`YYYY-MM-DD`, Europe/Madrid)
- intent: string (`FREE_DAILY` | `AD_UNLOCK` | `SUBSCRIPTION`)
- status: string (`PROCESSING` | `COMPLETED_SUCCESS` | `FAILED`)
- systemMode: string (`NORMAL` | `DEGRADED` | `EMERGENCY`)
- readingId: string (opcional)
- responsePayload: map (opcional, para replay idempotente)
- llmMeta: map (opcional)
- error: map (opcional)
- createdAt: timestamp
- updatedAt: timestamp

### /tarotReadings/{uid}/items/{readingId}
Histórico de lecturas exitosas por usuario.

Campos:
- requestId: string
- requestType: string (`TAROT_1` | `TAROT_3`)
- lang: string
- question: string (opcional, truncada)
- draw: array (id + orientation + position)
- reading: map (interpretación)
- createdAt: timestamp
- llmMeta: map (provider/tokens/cost/duration)

### /llmUsageDaily/{dateIso}/providers/{provider}
Agregación diaria de uso por proveedor.

Campos:
- calls: number
- success: number
- failed: number
- inputTokens: number
- outputTokens: number
- costUsd: number
- latencyMsTotal: number
- updatedAt: timestamp

---

## Economy backend (fase 5: rewarded ads claim backend)

### /config/economy
Flags runtime para rollout gradual de economía backend.

Campos:
- tarotEconomyV2Enabled: boolean (default `false`)
- oracleEconomyV2Enabled: boolean (default `false`)
- birthEssenceEconomyV2Enabled: boolean (default `false`)

### /economyBalances/{uid}
Saldo de Lunas del usuario.

Campos:
- balance: number (default 0)
- updatedAt: timestamp (opcional)

### /economyBalances/{uid}/ledger/{entryId}
Libro mayor de movimientos de Lunas.

Campos:
- type: string (`DAILY_LOGIN_CLAIM` | `REWARDED_AD_CLAIM` | `TAROT_1_MOON_SPEND` | `TAROT_3_MOON_SPEND` | `ORACLE_1Q_MOON_SPEND` | `BIRTH_ESSENCE_MOON_SPEND` | `HOROSCOPE_FUTURE_DAY_MOON_SPEND` | `HOROSCOPE_WEEKLY_MOON_SPEND` | `HOROSCOPE_MONTHLY_MOON_SPEND` | `REFUND`)
- amount: number
- requestId: string
- dateIso: string (`YYYY-MM-DD` Europe/Madrid)
- createdAt: timestamp
- placement: string (opcional, rewarded ads)
- targetDateIso: string opcional (daily unlock)
- weekKey: string opcional (weekly unlock)
- monthKey: string opcional (monthly unlock)
- unlockKey: string opcional
- module: string opcional
- source: string opcional

### /economyUsageDaily/{dateIso}/users/{uid}
Uso diario de economía por usuario.

Campos (fase 1):
- dailyLoginClaimed: boolean
- dailyLoginClaimedAt: timestamp (opcional)
- rewardedAdsClaimed: number (default 0)
- counters por módulo (opcionales, default 0)
  - tarot1FreeUsed, tarot1PremiumUsed, tarot1MoonUsed
  - tarot3FreeUsed, tarot3PremiumUsed, tarot3MoonUsed
  - oracleFreeUsed, oraclePremiumUsed, oracleMoonUsed
  - birthEssenceTotalUsed, birthEssenceMoonUsed, birthEssencePremiumExtraMoonUsed
  - birthEssencePremiumIncludedUsed (legacy/compat opcional)
  - synastryFreeUsed, synastryMoonPacksUsed, synastryPremiumUsed
  - pendulumFreeUsed, pendulumMoonPacksUsed, pendulumPremiumUsed
  - horoscopeFutureDayMoonUsed, horoscopeWeeklyMoonUsed, horoscopeMonthlyMoonUsed
- updatedAt: timestamp (opcional)

### /economyUsageWeekly/{weekKey}/users/{uid}
Uso semanal de economía por usuario.

Campos (fase 1):
- tarot3FreeUsed: number (opcional)

### /economyUsageMonthly/{monthKey}/users/{uid}
Uso mensual de economía por usuario.

Campos (fase 1):
- birthEssencePremiumIncludedUsed: number (opcional)

### /economyLifetime/{uid}
Flags lifetime de economía por usuario.

Campos (fase 1):
- birthEssenceFreeClaimed: boolean (opcional)

### /economyRequests/{uid}/requests/{requestId}
Control de idempotencia para callables de economía.

Campos (fase 6):
- requestId: string
- type: string (`CLAIM_DAILY_LOGIN` | `CLAIM_REWARDED_AD` | `TAROT_1` | `TAROT_3` | `ORACLE_1Q` | `BIRTH_ESSENCE` | `HOROSCOPE_UNLOCK_DAY` | `HOROSCOPE_UNLOCK_WEEKLY` | `HOROSCOPE_UNLOCK_MONTHLY`)
- result: string (`CLAIMED` | `DAILY_LIMIT_REACHED` | `ALREADY_CLAIMED` | `RESERVED` | `COMPLETED_SUCCESS` | `REFUNDED` | `FAILED`)
- status: string opcional (`PROCESSING` | `FAILED` | `COMPLETED_SUCCESS`)
- decisionSource: string opcional (`FREE` | `PREMIUM_INCLUDED` | `MOON` | `REJECT`)
- moonCostCharged: number opcional
- usageApplied.dailyCounter: string opcional
- usageApplied.dailyCounters: string[] opcional
- usageApplied.weeklyCounter: string opcional
- usageApplied.monthlyCounter: string opcional
- usageApplied.lifetimeFlag: string opcional
- dateIso: string opcional
- weekKey: string opcional
- monthKey: string opcional
- lang: string opcional
- question: string opcional (truncada)
- response: map opcional (payload estable para `claimDailyLogin` / `claimRewardedAd`)
- responsePayload: map opcional (payload estable para Tarot/Oracle/Birth Essence v2)
- llmMeta: map opcional
- refundedAt: timestamp opcional
- error: map opcional
- createdAt: timestamp
- updatedAt: timestamp

Notas:
- En fase 5, `claimRewardedAd` guarda `responsePayload.adProof` como evidencia mínima estructural para idempotencia/auditoría.
- La validación de `adProof` en esta fase es preparatoria (presencia + formato básico), sin verificación criptográfica/SSV real.

### /economyUnlocks/{uid}/horoscope/{unlockKey}
Unlocks de horóscopo por fecha/período.

`unlockKey` soportados:
- `daily:{dateIso}`
- `weekly:{weekKey}`
- `monthly:{monthKey}`

Campos:
- unlockKey: string
- type: string (`daily` | `weekly` | `monthly`)
- dateIso: string opcional (`YYYY-MM-DD`) para daily
- weekKey: string opcional (`YYYY-Www`) para weekly
- monthKey: string opcional (`YYYY-MM`) para monthly
- createdAt: timestamp
- requestId: string
- costCharged: number
- premiumIncluded: boolean
- contextSign: string opcional (auditoría; unlock sigue siendo por período)

### /tarotDeckTracks/{trackId}
Configuración backend-owned de tracks de progresión de mazos (sin asignación de cartas en esta fase).

- enabled: boolean
- moonsPerUnlock: number
- rewardType: string (p.ej. TAROT_CARD)
- rewardPoolId: string (p.ej. classic_arcana_v1)
- createdAt: timestamp
- updatedAt: timestamp

### /users/{uid}/tarotDeckProgress/{trackId}
Acumulado server-side del progreso por track.

- totalMoonSpend: number
- carryOverMoons: number
- unlocksGranted: number
- updatedAt: timestamp

### /users/{uid}/tarotDeckProgressRequests/{requestId}
Idempotencia de aplicación de progreso por gasto real moon.

- applied: boolean
- moonCostCharged: number
- source: string
- createdAt: timestamp
