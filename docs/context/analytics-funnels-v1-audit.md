# Audit técnico — analytics funnels de monetización (v1)

Fecha: 2026-04-28  
Rama analizada: `feature/analytics-funnels-v1`

## Alcance de auditoría

Se revisó wiring real de emisión de eventos analytics relacionados con:
- paywall
- rewarded ads
- premium
- moon store
- content unlock
- module limits

Fuentes principales:
- Contrato de eventos tipados: `shared/domain/.../AnalyticsEvent.kt`
- Emisiones efectivas en ViewModels de `shared/presentation`
- Triggers de impresión/click en Compose UI (`composeApp/...`)

---

## 1) Inventario de eventos emitidos actualmente

## Paywall

- `paywall_shown(placement, module, reason)`
  - Emitido desde `EconomyViewModel.onMoonPaywallShown`.
  - `placement` fijo: `moon_paywall`.
  - `module` derivado de `request.source` (si vacío: `unknown`).
  - `reason` fijo: `insufficient_moons`.

- `paywall_action_clicked(placement, module, action)`
  - Emitido desde `EconomyViewModel.onMoonPaywallActionClicked`.
  - `action` observadas en UI: `dismiss`, `claim_daily`, `watch_ad`, `open_store`.

## Rewarded ads

- `rewarded_ad_cta_shown(placement, rewarded_ads_remaining)`
  - Emitido en impresiones reales de CTA desde:
    - `MoonPaywallDialog`
    - `MoonStoreScreen`
    - `HoroscopeScreen` overlay lock.

- `rewarded_ad_started(placement)`
  - Emitido en `EconomyViewModel.claimRewardedAd`.

- `rewarded_ad_completed(placement, reward, balance_after)`
  - Emitido en éxito de claim rewarded (`EconomyViewModel.claimRewardedAd`).
  - También emitido en flujo legacy de tarot (`grantOneMoonFromFutureRewardedAd`).

- `rewarded_ad_failed(placement, reason)`
  - Emitido en claim no exitoso o excepción de claim rewarded.

## Content unlock

- `content_unlock_attempt(module, cost, has_enough_moons, is_premium)`
  - Emitido en `EconomyViewModel.requireLunas` (pre-paywall) y en `HoroscopeViewModel` unlocks explícitos.

- `content_unlocked(module, method, cost_charged, balance_after)`
  - Emitido en unlock exitoso de Horoscope y en fallback legacy de Tarot.

- `content_unlock_failed(module, reason)`
  - Emitido en fallos de unlock de Horoscope.

## Premium

- `premium_cta_shown(placement)`
  - Emitido actualmente desde Settings (`settings_subscribe`).

- `premium_cta_clicked(placement)`
  - Emitido actualmente en Settings (`settings_primary`, `settings_subscribe`, `settings_catalog`).

- `premium_purchase_started(product_id)`
  - Emitido en Settings al iniciar compra premium.

- `premium_purchase_completed(product_id, price?, currency?)`
  - Emitido en Settings en resultado `Success`.

- `premium_purchase_failed(product_id, reason)`
  - Emitido en Settings en resultado `Failed`.

## Moon store

- `moon_pack_viewed(pack_id, moons, price)`
  - Emitido al cargar packs en `MoonStoreViewModel`.

- `moon_pack_selected(pack_id, moons, price)`
  - Emitido en click de pack (`onBuyPackClicked`).

- `moon_pack_purchase_failed(pack_id, reason)`
  - Emitido actualmente con `reason=not_available` (coming soon).

## Module limits

- `module_limit_reached(module, is_premium)`
  - Emitido en:
    - `OracleAskViewModel` cuando backend devuelve `ResourceExhausted` o `FailedPrecondition`.
    - `TarotViewModel` en `SpendMoonsResult.InsufficientBalance` para `tarot_extra_reading`.

---

## 2) Reconstructibilidad de funnels solicitados

## Funnel A: `paywall -> watch_ad -> rewarded_completed -> unlock`

**Estado:** parcial (reconstrucción heurística, no determinista al 100%).

- Sí existe:
  - `paywall_shown`
  - `paywall_action_clicked(action=watch_ad)`
  - `rewarded_ad_started/completed/failed`
  - `content_unlocked`
- Gap clave:
  - no existe `correlation_id` común entre paywall/action/rewarded/unlock.
  - `content_unlocked` no referencia explícitamente si el unlock vino del flujo de paywall actual.

## Funnel B: `paywall -> open_store -> moon_pack_selected`

**Estado:** parcial.

- Sí existe:
  - `paywall_action_clicked(action=open_store)`
  - `moon_pack_selected`
- Gap clave:
  - falta correlación dura entre esa acción de paywall y la sesión de store subsiguiente.

## Funnel C: `paywall -> premium_cta_clicked -> purchase_started`

**Estado:** no reconstruible hoy para contexto paywall.

- `premium_*` está cableado en Settings.
- El paywall actual no emite ni enruta CTA premium.
- No hay puente `placement`/`origin` entre paywall y premium flow.

## Funnel D: `module_limit_reached -> paywall_shown`

**Estado:** no consistente.

- `module_limit_reached` se emite en Oracle/Tarot.
- `paywall_shown` se emite cuando `EconomyViewModel.requireLunas` abre moon paywall por saldo.
- No hay enlace explícito general entre ambos eventos.

## Funnel E: `rewarded_ad_cta_shown -> rewarded_ad_started -> completed/failed`

**Estado:** mayormente sí (por placement), con matices.

- Se puede reconstruir por `placement` y proximidad temporal en:
  - `moon_store`
  - `horoscope_period_lock_overlay`
  - flujo contextual de paywall (placement derivado de `source` o fallback).
- Gap:
  - sin `correlation_id`, puede haber ambigüedad con múltiples intentos concurrentes/repetidos.

---

## 3) Gaps detectados

1. **Placement/module no totalmente consistente**
   - `paywall_shown.placement` es constante `moon_paywall`, mientras rewarded usa placement variable (`source` o defaults).
   - En algunos puntos se usa `module` semántico y en otros `placement` como proxy de origen.

2. **Sin correlation id cross-event**
   - No hay parámetro común para unir con certeza una impresión de paywall, su click, el intento de rewarded y el unlock final.

3. **Origen de unlock incompleto para funneling**
   - `content_unlocked.method` existe (`moons`, etc.), pero no siempre permite distinguir si vino de:
     - unlock directo con saldo previo,
     - flujo paywall + rewarded,
     - flujo premium.

4. **Premium CTA fuera de paywall**
   - Eventos premium están en Settings; no hay CTA premium integrada en moon paywall.

5. **Moon Store packs en coming soon**
   - Se emite `moon_pack_selected` y luego `moon_pack_purchase_failed(not_available)`.
   - No existe compra real de pack (`moon_pack_purchase_started/completed` sin wiring efectivo).

6. **Module limit no conectado al paywall**
   - `module_limit_reached` ocurre en rutas que no disparan automáticamente `paywall_shown` con causalidad explícita.

---

## 4) Plan mínimo propuesto para `analytics-funnels-v1` (sin romper compatibilidad)

Objetivo: poder reconstruir funnels solicitados de forma confiable, manteniendo el dominio agnóstico de plataforma y minimizando cambios de contrato.

## Propuesta de eventos nuevos

**No agregar eventos nuevos en esta fase** (mínimo viable).  
Primero reforzar params en eventos existentes para correlación.

## Params a añadir (backward-compatible)

1. `paywall_shown`
   - agregar `paywall_impression_id` (usar `MoonPaywallRequest.impressionId` ya existente).
   - opcional: `origin` (valor de `source` normalizado).

2. `paywall_action_clicked`
   - agregar `paywall_impression_id`.

3. `rewarded_ad_cta_shown`, `rewarded_ad_started`, `rewarded_ad_completed`, `rewarded_ad_failed`
   - agregar opcional `paywall_impression_id` cuando el flujo nazca del paywall.
   - mantener null/omitido fuera de paywall.

4. `content_unlock_attempt`, `content_unlocked`, `content_unlock_failed`
   - agregar opcional `unlock_flow_origin` con catálogo mínimo:
     - `direct_balance`
     - `paywall_rewarded`
     - `paywall_store`
     - `premium`
     - `unknown`
   - agregar opcional `paywall_impression_id` cuando aplique.

5. `premium_cta_shown`, `premium_cta_clicked`, `premium_purchase_started`
   - agregar opcional `origin_placement` para distinguir `settings_*` vs `paywall_*` en el futuro.

> Compatibilidad: todos los params nuevos deben ser opcionales y omitibles; no cambiar nombres de eventos existentes.

## Archivos a tocar (propuesta)

### Dominio (contratos tipados)
- `shared/domain/src/commonMain/kotlin/com/agc/bwitch/domain/analytics/AnalyticsEvent.kt`
  - ampliar data classes con nuevos campos opcionales.
  - mantener serialización en `params()` omitiendo nulls.

### Presentation (wiring)
- `shared/presentation/src/commonMain/kotlin/com/agc/bwitch/presentation/economy/EconomyViewModel.kt`
  - propagar `MoonPaywallRequest.impressionId` a eventos paywall/action/rewarded.
  - setear `unlock_flow_origin` en rutas `requireLunas` y callbacks de éxito.

- `shared/presentation/src/commonMain/kotlin/com/agc/bwitch/presentation/astrology/horoscope/HoroscopeViewModel.kt`
- `shared/presentation/src/commonMain/kotlin/com/agc/bwitch/presentation/tarot/TarotViewModel.kt`
  - enriquecer `content_unlock_*` con origen cuando sea deducible.

- `shared/presentation/src/commonMain/kotlin/com/agc/bwitch/presentation/userprofile/SettingsViewModel.kt`
  - añadir `origin_placement` (inicialmente `settings`).

### Compose (orquestación UI)
- `composeApp/src/commonMain/kotlin/com/agc/bwitch/AppRoot.kt`
  - mantener wiring actual y pasar contexto de impression/origin a acciones.

- (fase posterior) `composeApp/src/commonMain/kotlin/com/agc/bwitch/ui/store/MoonPaywallDialog.kt`
  - si se habilita CTA premium en paywall, disparar premium events con `origin_placement=moon_paywall`.

## Tests mínimos a añadir

1. **EconomyViewModelTest**
   - validar que `paywall_shown` y `paywall_action_clicked` incluyen `paywall_impression_id`.
   - validar propagación de `paywall_impression_id` a rewarded events cuando nació desde paywall.

2. **Horoscope/Tarot tests**
   - validar `unlock_flow_origin` correcto en `content_unlock_*`.

3. **SettingsViewModelAnalyticsTest**
   - validar `origin_placement=settings_*` en premium events actuales.

4. **No-regression contract tests (domain)**
   - verificar que al serializar `params()` los nuevos campos null se omiten (compatibilidad).

---

## 5) Propuesta por commits

## Commit 1 — Contrato analytics backward-compatible
- Extender `AnalyticsEvent` con params opcionales de correlación/origen.
- Ajustar `params()` para omitir nulls.
- Agregar tests de contrato de params.

## Commit 2 — Wiring de correlación en paywall/rewarded/unlock
- En `EconomyViewModel` y `AppRoot` propagar `paywall_impression_id`.
- En unlock flows setear `unlock_flow_origin`.
- Actualizar tests de ViewModels afectados.

## Commit 3 — Premium origin normalization
- En `SettingsViewModel` agregar `origin_placement` estable para premium events.
- Preparar compatibilidad para futuro CTA premium en paywall (sin habilitar UI todavía).

## Commit 4 — Documentación
- Actualizar `docs/context/analytics-events-v1.md` con nuevos params opcionales.
- Añadir guía breve de reconstrucción de funnels (queries sugeridas por evento/param).

---

## Riesgos / trade-offs

- Añadir params opcionales incrementa cardinalidad si no se normalizan valores; se recomienda catálogo cerrado para `unlock_flow_origin`.
- Sin compra real de packs, funnel de store seguirá limitado a intención y fallo controlado.
- Sin CTA premium en paywall, ese funnel seguirá incompleto hasta fase UI posterior.

---

## Conclusión ejecutiva

El repo ya tiene una base sólida de eventos tipados y wiring real de impresiones/clicks para paywall y rewarded.  
Para `analytics-funnels-v1`, el mínimo efectivo no requiere eventos nuevos: basta con introducir **correlación transversal** (`paywall_impression_id`) y **origen explícito de unlock/premium** mediante params opcionales, manteniendo compatibilidad y sin acoplar dominio a plataforma.
