# Play Billing Premium sandbox integration

## Scope actual

- Plataforma: Android / Google Play Billing para internal testing sandbox.
- Producto habilitado: suscripción mensual `bwitch_premium_monthly`.
- Base plan esperado: `monthly`.
- Package name esperado: `com.agc.bwitch`.
- Fuera de alcance: anual, App Store, Moon packs reales, RTDN, trials, descuentos, múltiples base plans.

## Cloud Functions exportadas

La app usa Firebase Callable Functions en región `europe-west1`:

- `validateGooglePlayPurchase`
- `restoreGooglePlayPurchases`
- `refreshEntitlement`

Todas requieren `request.auth.uid`. App Check se fuerza salvo cuando `ALLOW_UNVERIFIED_APPCHECK_IN_DEV=true`.

## Contrato cliente → backend

`validateGooglePlayPurchase` recibe:

- `productId`
- `basePlanId`
- `purchaseToken`
- `purchaseState`
- `isAcknowledged`
- `orderId`
- `packageName`

`restoreGooglePlayPurchases` recibe una lista `purchases` con la misma forma de compra.

`refreshEntitlement` no requiere payload útil; usa el UID autenticado.

Validaciones locales estrictas:

- `productId == bwitch_premium_monthly`
- `basePlanId == monthly` si viene informado
- `packageName == com.agc.bwitch`
- `purchaseToken` no vacío
- `purchaseState == PURCHASED` para `validateGooglePlayPurchase`

Las respuestas compatibles incluyen:

- `active`
- `isActive`
- `status`
- `productId`
- `planType`

Cuando el backend devuelve activo para el producto mensual, la app lo interpreta como `SubscriptionStatus.ActiveMonthly`.

## Validación server-side Google Play

El backend valida compras con Google Play Developer API (`androidpublisher`) usando credenciales runtime / service account de Google Cloud y el scope:

- `https://www.googleapis.com/auth/androidpublisher`

La implementación actual usa `google-auth-library` y llama al endpoint REST `purchases.subscriptionsv2.get` para evitar hardcodear credenciales. No hay credenciales sensibles en código.

Comprueba:

- token aceptado por Google Play
- `subscriptionState == SUBSCRIPTION_STATE_ACTIVE`
- line item del producto `bwitch_premium_monthly`
- base plan `monthly`
- `expiryTime` presente y no expirado

Estados cancelados, expirados, tokens inválidos, productos/base plans distintos o line items sin `expiryTime` no activan Premium.

## Firestore backend-owned

Entitlement actual:

- `/userEntitlements/{uid}`

Campos mínimos escritos para Google Play activo:

- `isSubscriber`
- `status`
- `productId`
- `basePlanId`
- `source = google_play`
- `purchaseTokenHash`
- `packageName`
- `updatedAt`
- `expiresAt` si Google Play lo devuelve

Índice auxiliar de ownership:

- `/purchaseTokenIndex/{hash}`

Campos:

- `uid`
- `productId`
- `packageName`
- `updatedAt`

Si un token ya está asociado a otro UID, el backend rechaza la activación.

## Restore

`restoreGooglePlayPurchases` valida cada compra local enviada. Las compras no `PURCHASED` no activan Premium. Si alguna compra `PURCHASED` es activa y válida en Google Play, se escribe el entitlement; si ninguna lo es, devuelve inactive.

Nunca se activa Premium sin validación Google Play válida.

## Refresh

`refreshEntitlement` lee `/userEntitlements/{uid}` y devuelve active si `isSubscriber == true`, el producto es `bwitch_premium_monthly` y `expiresAt` no está vencido.

Scope mínimo actual: no revalida Google Play en refresh porque el token raw no se almacena; solo se conserva `purchaseTokenHash` para ownership/auditoría. Una revalidación periódica futura debería persistir un token revalidable de forma segura o incorporar RTDN.

## Acknowledge

Ownership actual del acknowledge: cliente Android, solo después de que backend devuelva Premium activo y solo si la compra no estaba acknowledged.

No hay acknowledge backend en esta fase. Si se añade en el futuro, debe ser idempotente y compatible con el acknowledge cliente ya existente.

## Configuración requerida

Variables de entorno con defaults esperados:

- `ANDROID_PACKAGE_NAME=com.agc.bwitch`
- `GOOGLE_PLAY_PRODUCT_MONTHLY=bwitch_premium_monthly`
- `GOOGLE_PLAY_BASE_PLAN_MONTHLY=monthly`

Google Play / GCP:

1. Vincular el proyecto Google Cloud usado por Firebase con Google Play Console.
2. Habilitar Google Play Android Developer API en el proyecto.
3. Usar el service account runtime de Cloud Functions o uno dedicado.
4. Conceder en Play Console permisos suficientes para leer suscripciones/pedidos de la app (por ejemplo, acceso a la app `com.agc.bwitch` y permisos de pedidos/suscripciones necesarios para Android Publisher API).
5. Desplegar Functions sin credenciales hardcoded; las credenciales deben venir del runtime/service account.
