# Firestore Security Rules (starter)

Objetivo:
- Usuarios solo pueden modificar su perfil (/users/{uid})
- Posts/comentarios: crear si autenticado, editar/borrar solo autor (o moderación)
- Reacciones: 1 por usuario por post

NOTA: Estas reglas son un borrador; deben revisarse antes de producción.

## Reglas (pseudo)
- /users/{uid}
    - read: authenticated
    - write: request.auth.uid == uid

- /posts/{postId}
    - read: true (o authenticated si quieres)
    - create: authenticated
    - update/delete: authenticated && request.auth.uid == resource.data.authorId
    - validar tamaño de texto y campos permitidos

- /posts/{postId}/comments/{commentId}
    - read: true
    - create: authenticated
    - update/delete: authenticated && request.auth.uid == resource.data.authorId

- /posts/{postId}/reactions/{uid}
    - read: true
    - create/delete: authenticated && request.auth.uid == uid
    - update: false


- /users/{uid}/birthEssence/current
    - read: authenticated && request.auth.uid == uid
    - write: authenticated && request.auth.uid == uid
    - validar tamaño de interpretation y signos permitidos


- /ritualCategories/{categoryId}
    - read: true
    - write: false (solo backend/admin)

- /rituals/{ritualId}
    - read: true
    - write: false (solo backend/admin)
