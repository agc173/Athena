# Premium Subscriptions v1 — backend entitlement foundation

## Scope del PR 2

Este documento describe la base backend inicial para Premium Entitlements con Google Play. Quedan fuera de este PR:

- UI Premium.
- Flujo completo Android Billing en cliente.
- Moon packs reales.
- App Store.
- Plan anual.
- RTDN (Real-time Developer Notifications).

## Decisiones

1. **Backend source of truth**: la economía y cualquier unlock Premium deben consultar `/userEntitlements/{uid}.isSubscriber` y campos asociados. El cliente nunca debe conceder Premium únicamente por un purchase local de Billing.
2. **Google Play como fuente comercial inicial**: la validación entra por callables backend y un adapter aislado (`premium/googlePlayValidator.ts`).
3. **Firestore como fuente operativa**: `/userEntitlements/{uid}` resume el entitlement activo; `/purchaseReceipts/{uid}/items/{purchaseTokenHash}` conserva el recibo normalizado; `/purchaseTokenIndex/{purchaseTokenHash}` evita vincular el mismo token a múltiples usuarios.
4. **Sin token plano**: v1 no persiste ni devuelve `purchaseToken`. Solo se guarda HMAC-SHA256 (`purchaseTokenHash`) usando `PURCHASE_TOKEN_HASH_SECRET`. Para refresh server-side futuro hará falta cifrar el token con KMS/Secret Manager o incorporar RTDN; eso queda pendiente.
5. **Cancelación no corta periodo pagado**: en Google Play, `SUBSCRIPTION_STATE_CANCELED` significa que la renovación está cancelada; si `lineItems.expiryTime` sigue en el futuro, v1 mantiene Premium activo hasta `premiumUntil` con `autoRenewing=false`.
6. **Refresh stale requiere restore**: como no se guarda token plano, `refreshEntitlement(force=true)` o un entitlement stale puede devolver `needsRestore=true` para que el cliente invoque restore con tokens locales disponibles.
7. **No RTDN en v1**: no hay webhook de Google Play en este PR; la sincronización se hace por validate/restore/refresh.
8. **Modo mock seguro para dev/emulator**: `GOOGLE_PLAY_VALIDATION_MODE=mock` permite simular estados sin credenciales ni tokens reales. No usar mock en producción.
9. **Google Play real pendiente de credenciales operativas**: el adapter real está encapsulado contra Android Publisher `purchases.subscriptionsv2.get`, pero requiere credenciales/token de acceso backend configurado.

## Callables

- `validateGooglePlaySubscription`: valida un purchase Google Play autenticado, upsertea receipt/index/entitlement y devuelve un `PremiumEntitlement` saneado.
- `restoreGooglePlayPurchases`: recibe un array de purchases, valida cada token de forma idempotente y devuelve el entitlement más favorable vigente junto con contadores de restore.
- `refreshEntitlement`: lee el entitlement operativo. Si está stale y no puede revalidar server-side por ausencia de token plano, devuelve `needsRestore=true` sin conceder Premium nuevo.

## Cliente KMP/Android Billing — PR 3

- `BillingClient` en Android es proveedor local de catálogo (`BillingProduct`) y tokens (`BillingPurchaseToken`) únicamente. No decide Premium, no desbloquea features y no escribe `subscription_status` como autoridad.
- El catálogo visible de v1 consulta solo el producto mensual `bwitch_premium_monthly` (`ProductType.SUBS`). El plan anual queda reservado para una fase posterior y no se muestra en UI.
- `launchPurchaseFlow` distingue `Purchased(token)`, `Pending(token)`, `Cancelled`, `Failed(reason/code)` y `Unsupported`; `Purchased` significa token local listo para backend, no entitlement activo, y `Pending` no debe tratarse como compra completada.
- La capa presentation ya puede transportar tokens en `SubscriptionPurchaseOutcome.Purchased/Pending`, pero PR 3 no invoca backend validation; PR 4 debe conectar `purchaseToken` con `validateGooglePlaySubscription`.
- Restore usa `queryPurchasesAsync(ProductType.SUBS)` y devuelve tokens restaurables para que PR 4 invoque `restoreGooglePlayPurchases`; restore local no promueve Premium ni escribe estado activo.
- La cache legacy `subscription_status` se conserva solo como migración/información no autoritativa: valores activos locales no deben mostrarse como Premium real.
- Analytics no debe emitir `premium_purchase_completed` hasta que la validación backend confirme el entitlement.

## Variables de entorno

- `PURCHASE_TOKEN_HASH_SECRET`: secreto HMAC-SHA256 obligatorio fuera de dev/mock. Debe tener al menos 16 caracteres y rotarse como secreto backend.
- `GOOGLE_PLAY_VALIDATION_MODE`: `real` por defecto; `mock` solo para emulator/dev.
- `GOOGLE_PLAY_PRODUCT_ALLOWLIST`: CSV de productos aceptados. En v1 solo mensual Premium (por defecto `bwitch_premium_monthly,premium_monthly`).
- `GOOGLE_PLAY_PACKAGE_NAME`: package Android esperado; fallback temporal `com.bwitch.app`.
- `GOOGLE_PLAY_ACCESS_TOKEN`: token de acceso para el adapter real actual. Pendiente sustituir por credenciales de servicio/ADC gestionadas en despliegue.
