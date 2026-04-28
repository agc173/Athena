# Analytics Events v1 (Economía, monetización y funnels)

Esta versión define eventos tipados en `shared/domain` y tracking desde ViewModels en `shared/presentation`, sin acoplar lógica de negocio a Firebase.

## Implementación

- Contrato común: `AnalyticsTracker` + `AnalyticsEvent` tipado.
- Implementación por defecto: `NoOpAnalyticsTracker`.
- Android: `AndroidFirebaseAnalyticsTracker` (adapter por reflection; no rompe build si falta `firebase-analytics`).
- Integración de ViewModels: Economy, MoonStore, Horoscope, Tarot, OracleAsk, Settings.

### Política v1 de params desconocidos (anti-métricas falsas)

- Nunca usar valores centinela falsos (ej.: `-1`).
- Si un dato no es confiable:
  - se omite el param en el `map`, o
  - se usa `"unknown"` solo si está documentado explícitamente para ese evento.
- En esta v1:
  - `content_unlocked.balance_after` se omite si no existe dato real.
  - `content_unlock_attempt.has_enough_moons` se omite si el VM no tiene snapshot fiable.
  - `premium_purchase_completed.price/currency` se omiten si no vienen del flujo real de compra.

## Tabla de eventos

| Evento | Params | Cuándo se dispara |
|---|---|---|
| `economy_balance_viewed` | `balance`, `is_premium` | Al cargar snapshot usable de economía. |
| `moon_earned` | `source`, `amount`, `balance_after` | Claim diario/rewarded y recompensas de ad en tarot. |
| `moon_spent` | `module`, `cost`, `balance_after` | Unlock con coste local (tarot fallback legacy). |
| `rewarded_ad_cta_shown` | `placement`, `rewarded_ads_remaining` | Reservado para impresiones reales de CTA en UI (no por disponibilidad global). |
| `rewarded_ad_started` | `placement` | Inicio de claim rewarded ad. |
| `rewarded_ad_completed` | `placement`, `reward`, `balance_after` | Claim rewarded exitoso / grant de recompensa. |
| `rewarded_ad_failed` | `placement`, `reason` | Claim rewarded fallido o no-claimed. |
| `content_unlock_attempt` | `module`, `cost`, `has_enough_moons`, `is_premium` | Intentos explícitos de unlock (paywall/unlock actions). |
| `content_unlocked` | `module`, `method`, `cost_charged`, `balance_after` | Unlock exitoso de contenido. |
| `content_unlock_failed` | `module`, `reason` | Unlock fallido por backend/saldo. |
| `premium_cta_shown` | `placement` | CTA premium expuesto en Settings subscribe. |
| `premium_cta_clicked` | `placement` | Click explícito sobre CTA premium. |
| `premium_purchase_started` | `product_id` | Inicio de compra premium desde Settings. |
| `premium_purchase_completed` | `product_id`, `price`, `currency` | Flujo premium completado. |
| `premium_purchase_failed` | `product_id`, `reason` | Flujo premium con error. |
| `moon_pack_viewed` | `pack_id`, `moons`, `price` | Carga de packs en Moon Store. |
| `moon_pack_selected` | `pack_id`, `moons`, `price` | Selección de pack por usuario. |
| `moon_pack_purchase_started` | `pack_id` | Inicio de compra real de pack (pendiente de wiring en flujo real). |
| `moon_pack_purchase_failed` | `pack_id`, `reason` | Compra de pack no disponible actualmente. |
| `module_limit_reached` | `module`, `is_premium` | Errores de límite/economía en módulos (oracle/tarot/horoscope). |
| `paywall_shown` | `placement`, `module`, `reason` | Mostrar paywall por saldo insuficiente. |
| `paywall_action_clicked` | `placement`, `module`, `action` | Acción explícita en paywall (reservado para wiring UI adicional). |
| `module_used` | `module`, `action` | Uso explícito de módulos (p.ej. `tarot` new request, `oracle` ask). |

## Notas

- Para evitar saturación, el tracking se hace en acciones explícitas o cambios únicos de estado (no en recompositions Compose).
- Diferencias operativas:
  - `content_unlock_failed(reason="insufficient_moons")`: intento de unlock con saldo insuficiente.
  - `paywall_shown`: se mostró un paywall por contexto de monetización.
  - `module_limit_reached`: límites reales del módulo (quota/limit), no saldo insuficiente.
- Los eventos `*_shown` deben dispararse por impresión real (punto estable de UI), nunca por click.
- `moon_pack_purchase_completed` queda pendiente hasta integrar backend/SDK de compra real de packs.
- `paywall_action_clicked` queda preparado en el modelo; requiere wiring adicional en handlers UI específicos.
