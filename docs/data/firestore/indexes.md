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

## Economy request watchdog
- Scheduler `recoverTimedOutEconomyRequestsScheduled` consulta `collectionGroup('requests')` con:
    - `where(status == PROCESSING)`
    - `where(updatedAt <= cutoff)`
    - `limit(200)`
  Puede requerir índice de collection group para `requests` por `status` + `updatedAt` según el proyecto Firebase.
