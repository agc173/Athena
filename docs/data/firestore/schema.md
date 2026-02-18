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
- username único (si se requiere, ver estrategia en `rules/indexes`)

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
