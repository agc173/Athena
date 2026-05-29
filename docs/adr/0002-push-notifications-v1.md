# ADR-0002: Push Notifications v1

## Contexto
Se habilita Push Notifications v1 con enfoque Android-first usando FCM, registro de tokens por callable y preferencias de notificación administradas por backend.

## Decisión
- **Android-first (v1)**: el rollout inicial prioriza Android.
- **Proveedor push**: Firebase Cloud Messaging (FCM).
- **Preferencias backend-authoritative**: `notificationPreferences/current` se actualiza vía callable autenticada y App Check; no se escribe directo desde cliente.
- **Modelo de tokens**: `users/{uid}/pushTokens/{tokenHash}` usa `docId = sha256(token)` y mantiene `token` raw solo para envíos backend.
- **Ownership global de tokens**: `pushTokenIndex/{tokenHash}` evita que un token FCM quede activo bajo varios usuarios; el registro reasigna el owner y deshabilita el token en el usuario anterior.
- **Toggle global**: desactivar notificaciones (`globalEnabled=false`) no hace unregister inmediato del token.
- **QA callable**: `sendTestNotification` queda protegida por flag de entorno `PUSH_TEST_CALLABLE_ENABLED`.
- **Fuera de alcance**: scheduler/automatización de campañas no entra en este commit.

## Consecuencias
- Se reduce exposición de tokens al cliente y se centraliza control en backend.
- El backend puede deshabilitar/limpiar tokens inválidos sin intervención del cliente.
- Queda preparado `unregister token` para logout futuro sin acoplarlo al toggle de preferencias.


## Actualización 2026-05-27 — Scheduler diario v1 (horóscopo)
- El scheduler diario se ejecuta periódicamente cada 30 minutos (UTC) y evalúa cada token habilitado según `token.timezone`.
- Ventana de envío v1 fija: `09:30-09:59` hora local del usuario (`targetHour=9`, `targetMinute=30`, `windowMinutes=30`).
- Fallback de timezone: `Europe/Madrid` cuando `timezone` falta o es inválida.
- Idempotencia diaria por usuario se mantiene con `pushNotificationSends/{dateIso}_daily_horoscope`, donde `dateIso` se calcula en la zona local efectiva del usuario.


## Actualización 2026-05-29 — Hardening mínimo beta
- Android sincroniza `onNewToken` con backend o deja pendiente el registro si aún no hay usuario autenticado/permisos.
- En auth ready/login se reconcilia el token FCM actual cuando el permiso de notificaciones está concedido.
- Logout intenta `unregisterPushToken` antes de `signOut`; fallos de red/Auth/App Check quedan pendientes solo para el mismo `uid`, porque la callable requiere la sesión del owner original.
- Android release instala Play Integrity App Check provider. Requisito externo: registrar la app en Firebase Console App Check y habilitar/configurar Play Integrity API en el proyecto Play/Google Cloud correspondiente.
- Al abrir Settings, Android reconcilia permiso real: si fue revocado, actualiza preferencias y desregistra el token actual cuando hay sesión.
