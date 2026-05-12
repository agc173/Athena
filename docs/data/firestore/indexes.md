# Firestore indexes (starter)

## Feed (posts)
- Query típica:
    - orderBy(createdAt desc)
    - optional where(topic == X)
      Requiere:
- índice compuesto si se filtra por topic y orderBy(createdAt)

## Comments
- orderBy(createdAt asc/desc) dentro de /posts/{postId}/comments
  Normalmente no necesita índice compuesto si no hay filters extra.


## Premium entitlements
- `/purchaseTokenIndex/{hash}` se lee/escribe por ID de documento desde Cloud Functions. No requiere índice compuesto.
- `/userEntitlements/{uid}` se lee/escribe por ID de documento desde Cloud Functions. No requiere índice compuesto.
