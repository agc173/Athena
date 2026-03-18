# Firestore schema (propuesta inicial)

> Source of truth para colecciones y campos.
> Si se cambian colecciones/campos, actualizar este documento.

## Colecciones

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
- archetype: string (opcional)
- savedAtEpochMillis: number
- updatedAtEpochMillis: number

Notas:
- Solo existe un documento activo (`current`) por usuario.
- Cada nuevo guardado reemplaza la esencia activa anterior.

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

## Colecciones futuras (placeholder)
### /rituals/{ritualId}
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
Entitlements económicos del usuario.

Campos:
- isSubscriber: boolean (default false)
- updatedAt: timestamp (opcional)

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
