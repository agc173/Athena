# ADR-0002: Push Notifications v1

## Contexto
Se habilita Push Notifications v1 con enfoque Android-first usando FCM, registro de tokens por callable y preferencias de notificación administradas por backend.

## Decisión
- **Android-first (v1)**: el rollout inicial prioriza Android.
- **Proveedor push**: Firebase Cloud Messaging (FCM).
- **Preferencias backend-authoritative**: `notificationPreferences/current` se actualiza vía callable autenticada y App Check; no se escribe directo desde cliente.
- **Modelo de tokens**: `users/{uid}/pushTokens/{tokenHash}` usa `docId = sha256(token)` y mantiene `token` raw solo para envíos backend.
- **Toggle global**: desactivar notificaciones (`globalEnabled=false`) no hace unregister inmediato del token.
- **QA callable**: `sendTestNotification` queda protegida por flag de entorno `PUSH_TEST_CALLABLE_ENABLED`.
- **Fuera de alcance**: scheduler/automatización de campañas no entra en este commit.

## Consecuencias
- Se reduce exposición de tokens al cliente y se centraliza control en backend.
- El backend puede deshabilitar/limpiar tokens inválidos sin intervención del cliente.
- Queda preparado `unregister token` para logout futuro sin acoplarlo al toggle de preferencias.
