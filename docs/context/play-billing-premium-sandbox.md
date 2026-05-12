# Play Billing Premium sandbox integration

## Scope actual

- Plataforma: Android / Google Play Billing para internal testing sandbox.
- Producto habilitado: suscripción mensual `bwitch_premium_monthly`.
- Base plan esperado: `monthly`.
- Fuera de alcance: anual, App Store, Moon packs reales, RTDN, trials, descuentos, múltiples base plans.

## Contrato cliente → backend propuesto

La app usa Firebase Callable Functions para reconciliar Premium. Los nombres esperados son:

- `validateGooglePlayPurchase`
- `restoreGooglePlayPurchases`
- `refreshEntitlement`

`validateGooglePlayPurchase` recibe, como mínimo:

- `productId`
- `basePlanId`
- `purchaseToken`
- `purchaseState`
- `isAcknowledged`
- `orderId`
- `packageName`

`restoreGooglePlayPurchases` recibe una lista `purchases` con la misma forma de compra.

Las respuestas deben indicar entitlement activo con alguno de estos campos compatibles:

- `active`
- `isActive`
- `isPremium`
- `premium.isPremium`

Cuando el backend devuelva activo para el producto mensual, la app lo interpreta como `SubscriptionStatus.ActiveMonthly`.

## Reglas de activación Premium

- La app no activa Premium por una compra local de Billing.
- Una compra `PURCHASED` solo activa Premium tras `validateGooglePlayPurchase` con entitlement activo.
- Una compra `PENDING` no activa Premium, no emite `premium_purchase_completed` y no refresca economía.
- Restore consulta compras Google Play locales para conservar `purchaseToken`, pero solo activa Premium tras `restoreGooglePlayPurchases` con entitlement activo.
- `refreshEntitlement` es la fuente backend para status inicial/refresh.

## Acknowledge

El cliente puede hacer acknowledge con `BillingClient.acknowledgePurchase` únicamente después de que backend confirme entitlement activo y solo si la compra aún no estaba acknowledged.

Bloqueo de sandbox real: si el backend también hace acknowledge, debe mantenerse idempotente. Si el backend no lo hace, el acknowledge cliente posterior a validación backend es obligatorio para evitar compras sin acknowledge.
