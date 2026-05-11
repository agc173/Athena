# BWitch Economy v1.0 (modelo final cerrado)

Fecha de consolidación: 2026-05-11.

Este documento define la **única fuente de verdad económica** para backend Functions, shared/domain y UI antes de implementar Premium Billing real.

## Reglas globales

- Daily login: `+1 Luna` por día.
- Rewarded ad: `+1 Luna`.
- Rewarded ads máximo: `3/día`.
- Los anuncios sirven **solo** para ganar Lunas mediante `claimRewardedAd`.
- Los anuncios **nunca** desbloquean contenido directamente: cualquier `rewardedProof` / `AD_UNLOCK` legacy no debe gobernar la autorización de Tarot, Oráculo ni otro módulo.
- Premium no es ilimitado: tiene tramos incluidos y límites diarios/mensuales según módulo.
- Premium no debe depender de saldo de Lunas para su tramo incluido.

## Tabla por módulo

| Módulo | Free | Lunas | Premium |
|---|---|---|---|
| Horóscopo | Diario actual gratis | Futuro `1` Luna hasta 6 días; semanal `3` Lunas; mensual `5` Lunas | Todo desbloqueado sin coste |
| Esencia natal | `1` lifetime | Extra `5` Lunas; máx `2/día` | `1/mes` incluido; extras también cuestan `5` Lunas; máx `2/día` |
| Sinastría | `2/día` | `1` Luna = `+3 usos`; free máx `5` packs extra/día | `10/día` incluidos; máx `10` packs extra/día |
| Tarot 1 | `1/día` | Extra `1` Luna; máx `3` extras/día | `5/día` incluidos; extras por Lunas según regla Tarot 1 |
| Tarot 3 | `1/semana` | Extra `3` Lunas; máx `2` extras/día | `1/día` incluido; máx total `3/día` |
| Oráculo | `1/día` | Extra `3` Lunas; free máx `10` extras/día | `3/día` incluidos; máx `12` extras con Lunas y `15/día` total |
| Péndulo | `8/día` | `1` Luna = `+10 usos`; máx `50/día` total | `50/día` incluidos |

## Runtime economy v2

Producción debe usar economía backend v2 para todos los módulos críticos (`tarotEconomyV2Enabled`, `oracleEconomyV2Enabled`, `birthEssenceEconomyV2Enabled`, `synastryEconomyV2Enabled`, `pendulumEconomyV2Enabled`). Los defaults de backend son `true`; un documento `/config/economy` puede desactivar un módulo temporalmente solo para rollback controlado o pruebas de emulador.

## Notas de implementación

- Horóscopo premium no consume Lunas para desbloqueos incluidos.
- Esencia premium no tiene descuento: después de `1/mes`, cada extra cuesta `5` Lunas.
- Sinastría separa el tramo incluido (`2/día` free o `10/día` premium) de packs comprados con Lunas (`1 Luna = +3 usos`).
- Oráculo premium contabiliza `oraclePremiumUsed + oracleMoonUsed` para el máximo total `15/día`.
- No se implementa compra real de packs de Lunas en este PR.
- No se implementa Google Play Billing ni validación backend de compras en este PR.
