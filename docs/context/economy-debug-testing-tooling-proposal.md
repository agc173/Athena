# Economy debug/testing tooling proposal (BWitch)

## Objetivo
Habilitar inspección y reseteo controlado del estado económico **solo para cuentas de test** en entornos debug/dev, minimizando riesgo de tocar producción.

## Opción recomendada (segura)
Implementar un **Debug Economy Console** con dos capas:

1. **UI debug en app (solo debug build)**
   - Entrada oculta en `Settings` (por ejemplo 7 taps en versión).
   - Pantalla de solo lectura + acciones explícitas de testing.
   - Nunca compilada/mostrada en release.

2. **Callable Functions admin-dev dedicadas**
   - `getEconomyDebugSnapshot`
   - `resetEconomyUsageForTestUser`
   - `setEconomyTestBalance`
   - (opcional) `setTestEntitlementPremium`
   - Todas con guardas obligatorias:
     - auth requerida
     - allowlist de UID/email test
     - entorno `dev/staging` o emulador
     - rechazo explícito en producción

---

## Diseño funcional solicitado

### 1) Ver estado económico completo del usuario actual
`getEconomyDebugSnapshot` devuelve:
- `uid`
- `email`
- `balance`
- `isPremium` (fuente: `userEntitlements/{uid}.isSubscriber`)
- usage relevante:
  - `economyUsageDaily/{date}/users/{uid}`
  - `economyUsageWeekly/{week}/users/{uid}`
  - `economyUsageMonthly/{month}/users/{uid}`
  - `economyLifetime/{uid}`
- module previews para:
  - `ORACLE_1Q`, `TAROT_1`, `TAROT_3`, `HOROSCOPE_FUTURE_DAY`, `BIRTH_ESSENCE`, `SYNASTRY`, `PENDULUM`

### 2) Resetear cuotas test del usuario actual
`resetEconomyUsageForTestUser` con payload:
- `resetDaily: Boolean`
- `resetWeekly: Boolean`
- `resetMonthly: Boolean`
- `resetLifetime: Boolean` (default `false`; requiere confirmación extra)
- `reason: "TESTING_ONLY"`

Requisito UX:
- Doble confirmación para lifetime (checkbox + botón mantener pulsado).

### 3) Ajustar balance de lunas en dev/test
`setEconomyTestBalance`:
- `balance = 0 | 1 | 10` (whitelist de valores)
- registra entrada de ledger tipo `TEST_BALANCE_OVERRIDE`
- metadata: `actorUid`, `previousBalance`, `newBalance`, `timestamp`, `reason`

### 4) Simular premium
Preferencia:
- **Manual en Firestore** `userEntitlements/{uid}.isSubscriber` para trazabilidad.

Alternativa controlada:
- `setTestEntitlementPremium` solo en dev/staging + allowlist + audit trail.

---

## Requisitos de seguridad (enforcement)
- Debug Console expuesta solo con `BuildInfo.isDebug == true`.
- Functions debug bloqueadas en producción (`env != prod`).
- Operaciones prohibidas para usuarios no autenticados.
- Nunca mostrar tokens ni claims sensibles en UI.
- Toda mutación debe escribir auditoría (`debugAdminActions` o ledger con `testing=true`).
- No borrar ledger histórico; solo agregar entradas de ajuste.

---

## Archivos a modificar (propuesta mínima para implementar)

### App (KMP)
- `composeApp/src/commonMain/kotlin/com/agc/bwitch/presentation/navigation/Destination.kt`
  - agregar `Destination.EconomyDebug`.
- `composeApp/src/commonMain/kotlin/com/agc/bwitch/AppRoot.kt`
  - route a nueva pantalla debug.
- `composeApp/src/commonMain/kotlin/com/agc/bwitch/ui/userprofile/SettingsScreen.kt`
  - acceso oculto debug (solo debug).
- `composeApp/src/commonMain/kotlin/com/agc/bwitch/ui/economy/EconomyDebugScreen.kt` (nuevo)
  - snapshot + botones de reset/balance/premium helper.
- `composeApp/src/commonMain/kotlin/com/agc/bwitch/platform/BuildFlags.kt` (nuevo)
  - `expect fun isDebugBuild(): Boolean`.
- `composeApp/src/androidMain/kotlin/com/agc/bwitch/platform/BuildFlags.android.kt` (nuevo)
- `composeApp/src/iosMain/kotlin/com/agc/bwitch/platform/BuildFlags.ios.kt` (nuevo)

### Domain/Data
- `shared/domain/src/commonMain/kotlin/com/agc/bwitch/domain/economy/EconomyDebugRepository.kt` (nuevo)
- `shared/data/src/commonMain/kotlin/com/agc/bwitch/data/remote/economy/EconomyDebugRemoteDataSource.kt` (nuevo)
- `shared/data/src/commonMain/kotlin/com/agc/bwitch/data/economy/EconomyDebugRepositoryImpl.kt` (nuevo)
- `shared/data/src/commonMain/kotlin/com/agc/bwitch/data/di/DataKoinModule.kt`
  - registrar bindings debug repository.

### Functions (backend)
- `functions/src/economy/debug/getEconomyDebugSnapshot.ts` (nuevo)
- `functions/src/economy/debug/resetEconomyUsageForTestUser.ts` (nuevo)
- `functions/src/economy/debug/setEconomyTestBalance.ts` (nuevo)
- `functions/src/economy/debug/setTestEntitlementPremium.ts` (opcional)
- `functions/src/economy/index.ts`
  - export callables debug.

### Docs
- `docs/data/firestore/schema.md`
  - documentar `TEST_BALANCE_OVERRIDE` + auditoría.
- `docs/data/firestore/rules.md`
  - aclarar política debug/dev-only.
- `docs/changelog/2026-04.md`
  - entrada de cambio.

---

## Cómo usarlo para testear módulos

### Flujo base (repetible)
1. Entrar a Debug Economy Console.
2. `Refresh snapshot` y validar:
   - balance, premium, counters y previews.
3. `Set balance` (0/1/10) según escenario.
4. Ejecutar módulo.
5. Volver a snapshot y validar delta en usage + balance.

### Oracle
- Caso free: balance=0, premium=false, reset daily/weekly/monthly.
- Ejecutar 1 pregunta y revisar preview/counters de `ORACLE_1Q`.

### Tarot
- Probar `TAROT_1` y `TAROT_3` con balance 0/1/10.
- Verificar decremento de balance cuando corresponde y límites diarios.

### Horóscopo
- Probar unlock futuro con balance insuficiente/suficiente.
- Verificar `HOROSCOPE_FUTURE_DAY` preview y counters.

### Birth Essence
- Forzar balance=0 y luego balance=10 para validar ruta reject/moon.
- Confirmar counters del módulo y ledger de gasto/refund si aplica.

### Synastry
- Simular límite free con resets parciales.
- Validar transición a moon/premium según preview.

### Pendulum
- Validar consumo/counters según reglas configuradas (si está monetizado).
- Si no monetizado, el preview debe reflejar `FREE`/`NOT_CONFIGURED`.

---

## Patch mínimo aplicado en este cambio
Este PR **solo agrega documentación técnica de propuesta** para alinear implementación segura antes de tocar código productivo.
