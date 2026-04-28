# BWitch Economy v1.0 (modelo final cerrado)

Fecha de consolidación: 2026-04-28.

Este documento define la **única fuente de verdad económica** para backend Functions, shared/domain y UI.

## Reglas globales

- Daily login: `+1 Luna`.
- Rewarded ad: `+1 Luna`.
- Rewarded ads máximo: `3/día`.
- Premium también puede ver rewarded ads voluntarios.
- Los anuncios nunca son obligatorios.
- Prioridad de resolución económica: `FREE` → `PREMIUM_INCLUDED` → `MOON` → `REJECT`.

## Tabla por módulo

| Módulo | Free | Lunas | Premium |
|---|---|---|---|
| Horóscopo | Daily actual gratis | Future day `1`, Weekly `2`, Monthly `3` | Todo desbloqueado sin coste |
| Esencia natal | `1` lifetime | Extra normal `5`; extra premium `3`; máx total `2/día` | `1/mes` incluido |
| Sinastría | `2/día` | `1 Luna = +3 usos` | Incluido, con tope técnico `30/día` |
| Tarot 1 | `1/día` | Extra `1` Luna; máx total (free+moon) `3/día` | `5/día` incluido y hard cap `5/día` |
| Tarot 3 | `1/semana` | Extra `3` Lunas; máx con lunas `2/día` | `1/día` incluido y hard cap `3/día` |
| Oráculo | `1/día` | Extra `3` Lunas; máx con lunas `10/día` | `3/día` incluido y hard cap `15/día` |
| Péndulo | `8/día` | `1 Luna = +10 usos` | `50/día` (mismo tope técnico) |

## Notas de implementación

- Premium **no** debe depender de saldo de Lunas para su tramo incluido.
- Horóscopo premium no consume Lunas para desbloqueos incluidos.
- No se implementa compra real de packs de Lunas en v1.0.
- No se toca StoreKit/iOS en esta consolidación.
