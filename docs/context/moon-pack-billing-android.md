# Moon packs Android Billing (consumibles) — guía de pruebas

## Flujo server-authoritative
- Cliente Android lanza BillingFlow para INAPP (`bwitch_moons_pack_10|30|80`).
- Cliente envía `productId`, `purchaseToken`, `packageName` a callable `claimMoonPackPurchase`.
- Backend valida token en Android Publisher (`purchases.products.get`).
- Backend concede lunas en transacción Firestore y registra ledger `MOON_PACK_PURCHASE`.
- Cliente solo consume en Play cuando backend responde éxito (`shouldConsume=true`).

## Validaciones backend clave
- `packageName` debe igualar `ANDROID_PACKAGE_NAME`.
- `productId` debe ser uno de los packs soportados.
- `purchaseState` debe ser `PURCHASED`.
- Idempotencia por `purchaseTokenHash` en `/purchaseTokenIndex/{hash}`.
- Ownership estricto: un token no puede moverse entre UIDs.

## Mapeo economía
- `bwitch_moons_pack_10` => +10
- `bwitch_moons_pack_30` => +30
- `bwitch_moons_pack_80` => +80

## Checklist manual (license testers)
1. Publicar build en Internal Testing con mismos product IDs.
2. Comprar pack 10 con cuenta tester.
3. Verificar respuesta callable `CLAIMED` y `moonsGranted=10`.
4. Verificar en Firestore:
   - `economyBalances/{uid}.balance` incrementa.
   - `economyBalances/{uid}/ledger/moon-pack:{hash}` existe con `MOON_PACK_PURCHASE`.
   - `purchaseTokenIndex/{hash}` apunta al `uid`.
5. Reintentar claim con mismo token: debe devolver `ALREADY_CLAIMED` sin sumar balance.
6. Tras consumo cliente, recomprar mismo SKU y validar nuevo token + nuevo grant.
7. Pending purchase: debe fallar con `purchase_not_completed` hasta transición a `PURCHASED`.

## Pendiente para producción
- Añadir tests automáticos de callable con provider fake Android Publisher (unit/integration).
- Alinear cliente Android para consumir automáticamente solo en success backend.
- Añadir analytics de eventos de moon pack en launcher/UI.

## Internal Testing hardening (2026-05-25)

- Release/Internal rewarded ads now require `ADMOB_REWARDED_AD_UNIT_ID` (Gradle property or env var). If missing, app logs `unavailable ... blank_ad_unit_id` and UI shows visible feedback.
- Configure in CI/local:
  - `ADMOB_APP_ID=<admob app id>`
  - `ADMOB_REWARDED_AD_UNIT_ID=<rewarded ad unit id>`
- App Check on Android release uses `PlayIntegrityAppCheckProviderFactory` and requires Firebase Console App Check setup for package `com.agc.bwitch` with correct Play/Internal Testing signing SHA.
- Moon pack INAPP recovery now queries owned purchases (`queryPurchasesAsync(INAPP)`) when opening Moon Store and replays backend claim + consume-on-success to recover previously completed but unclaimed purchases.
