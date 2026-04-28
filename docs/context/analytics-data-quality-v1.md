# Data Quality Guardrails — analytics-data-quality-v1

Fecha: 2026-04-28  
Versión base: `analytics-funnels-v1`

## Objetivo

Definir guardrails operativos de calidad de datos para garantizar que los funnels de monetización (`paywall`, `rewarded`, `unlock`, `premium`) se puedan reconstruir de forma confiable en reporting.

Estos guardrails no miden performance de negocio; miden **integridad y trazabilidad** de eventos/params críticos.

## Supuestos de consulta (BigQuery style)

- Export de eventos tipo GA4 o equivalente con `event_name`, `event_timestamp`, `user_pseudo_id`, `event_params`.
- Ventana móvil de observación recomendada: 7 días (alertas) y 30 días (tendencia).
- Se usa `SAFE_DIVIDE` para evitar divisiones por cero.

Snippet base:

```sql
WITH base AS (
  SELECT
    event_date,
    TIMESTAMP_MICROS(event_timestamp) AS ts,
    user_pseudo_id,
    event_name,
    (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'paywall_impression_id') AS paywall_impression_id,
    (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'unlock_flow_origin') AS unlock_flow_origin,
    (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'origin_placement') AS origin_placement
  FROM `project.dataset.events_*`
  WHERE _TABLE_SUFFIX BETWEEN '20260401' AND '20260430'
)
```

---

## Guardrails obligatorios

## 1) Cobertura de `paywall_impression_id` en `paywall_shown`

**Definición**  
Porcentaje de eventos `paywall_shown` que incluyen `paywall_impression_id` no nulo/no vacío.

**Eventos usados**  
- `paywall_shown`

**Umbral esperado**  
- **>= 95%**

**Interpretación si cae por debajo**  
Riesgo alto de no poder reconstruir funnels de paywall por impresión. Se degrada correlación con `paywall_action_clicked` y `rewarded_*`.

**Pseudo SQL (BigQuery style)**

```sql
SELECT
  SAFE_DIVIDE(
    COUNTIF(paywall_impression_id IS NOT NULL AND paywall_impression_id != ''),
    COUNT(*)
  ) AS paywall_shown_impression_id_coverage
FROM base
WHERE event_name = 'paywall_shown';
```

---

## 2) Cobertura de `paywall_impression_id` en `paywall_action_clicked`

**Definición**  
Porcentaje de eventos `paywall_action_clicked` que incluyen `paywall_impression_id` no nulo/no vacío.

**Eventos usados**  
- `paywall_action_clicked`

**Umbral esperado**  
- **>= 95%**

**Interpretación si cae por debajo**  
Los clicks no se pueden atribuir correctamente a la impresión de paywall de origen; el CTR y conversiones por impresión quedan sesgados.

**Pseudo SQL (BigQuery style)**

```sql
SELECT
  SAFE_DIVIDE(
    COUNTIF(paywall_impression_id IS NOT NULL AND paywall_impression_id != ''),
    COUNT(*)
  ) AS paywall_action_clicked_impression_id_coverage
FROM base
WHERE event_name = 'paywall_action_clicked';
```

---

## 3) Cobertura de `paywall_impression_id` en `rewarded_*` originados en paywall

**Definición**  
Porcentaje de eventos `rewarded_ad_started`, `rewarded_ad_completed`, `rewarded_ad_failed` atribuibles a flujo de paywall que incluyen `paywall_impression_id`.

**Eventos usados**  
- `rewarded_ad_started`
- `rewarded_ad_completed`
- `rewarded_ad_failed`
- Referencia de atribución: conjunto de `paywall_impression_id` observados en `paywall_shown`

**Umbral esperado**  
- **>= 95%**

**Interpretación si cae por debajo**  
Se rompe el enlace técnico entre paywall y resultado rewarded; no se distingue si la pérdida es de UX/ad-provider o de instrumentación.

**Pseudo SQL (BigQuery style)**

```sql
WITH paywall_ids AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'paywall_shown'
    AND paywall_impression_id IS NOT NULL
    AND paywall_impression_id != ''
),
rewarded_from_paywall AS (
  SELECT b.*
  FROM base b
  JOIN paywall_ids p
    ON b.paywall_impression_id = p.paywall_impression_id
  WHERE b.event_name IN ('rewarded_ad_started', 'rewarded_ad_completed', 'rewarded_ad_failed')
)
SELECT
  SAFE_DIVIDE(
    COUNTIF(paywall_impression_id IS NOT NULL AND paywall_impression_id != ''),
    COUNT(*)
  ) AS rewarded_from_paywall_impression_id_coverage
FROM rewarded_from_paywall;
```

---

## 4) Cobertura de `unlock_flow_origin` en `content_unlocked`

**Definición**  
Porcentaje de eventos `content_unlocked` que incluyen `unlock_flow_origin` válido (no nulo/no vacío).

**Eventos usados**  
- `content_unlocked`

**Umbral esperado**  
- **>= 95%**

**Interpretación si cae por debajo**  
No se puede separar con fiabilidad unlock directo vs paywall_rewarded vs premium; se degrada lectura de mix de monetización.

**Pseudo SQL (BigQuery style)**

```sql
SELECT
  SAFE_DIVIDE(
    COUNTIF(unlock_flow_origin IS NOT NULL AND unlock_flow_origin != ''),
    COUNT(*)
  ) AS content_unlocked_origin_coverage
FROM base
WHERE event_name = 'content_unlocked';
```

---

## 5) Cobertura de `origin_placement` en `premium_cta_clicked` y `premium_purchase_started`

**Definición**  
Porcentaje de eventos premium críticos (`premium_cta_clicked`, `premium_purchase_started`) que incluyen `origin_placement` no nulo/no vacío.

**Eventos usados**  
- `premium_cta_clicked`
- `premium_purchase_started`

**Umbral esperado**  
- **>= 95%**

**Interpretación si cae por debajo**  
No se puede atribuir de forma consistente el origen del intento de compra (ej. settings vs paywall), afectando decisiones de placement.

**Pseudo SQL (BigQuery style)**

```sql
SELECT
  SAFE_DIVIDE(
    COUNTIF(origin_placement IS NOT NULL AND origin_placement != ''),
    COUNT(*)
  ) AS premium_origin_placement_coverage
FROM base
WHERE event_name IN ('premium_cta_clicked', 'premium_purchase_started');
```

---

## Alertas recomendadas

Alertas para monitoreo diario (rolling 24h) con validación de tendencia 7d:

1. **paywall_impression_id coverage < 95%**
   - Métrica objetivo: cobertura combinada en `paywall_shown` + `paywall_action_clicked`.
   - Severidad: alta.

2. **unlock_flow_origin unknown > 20%**
   - Métrica objetivo: proporción de `content_unlocked` con `unlock_flow_origin = 'unknown'`.
   - Severidad: media-alta (puede ocultar regresiones de wiring).

3. **rewarded_ad_started sin terminal completed/failed > 15%**
   - Métrica objetivo: starts sin evento terminal observable en ventana razonable (por ejemplo 30 min / misma sesión).
   - Severidad: alta (riesgo técnico o pérdida de eventos terminales).

4. **premium_purchase_started sin origin_placement > 5%**
   - Métrica objetivo: missing `origin_placement` en `premium_purchase_started`.
   - Severidad: media-alta.

Pseudo SQL de referencia para alertas clave:

```sql
-- 1) paywall_impression_id coverage (paywall_shown + paywall_action_clicked)
SELECT
  SAFE_DIVIDE(
    COUNTIF(paywall_impression_id IS NOT NULL AND paywall_impression_id != ''),
    COUNT(*)
  ) AS paywall_impression_id_coverage
FROM base
WHERE event_name IN ('paywall_shown', 'paywall_action_clicked');

-- 2) unlock_flow_origin unknown ratio en content_unlocked
SELECT
  SAFE_DIVIDE(
    COUNTIF(unlock_flow_origin = 'unknown'),
    COUNT(*)
  ) AS unlock_flow_origin_unknown_ratio
FROM base
WHERE event_name = 'content_unlocked';

-- 3) rewarded_ad_started sin terminal (aprox por user+paywall_id)
WITH started AS (
  SELECT DISTINCT
    user_pseudo_id,
    COALESCE(paywall_impression_id, CONCAT(user_pseudo_id, '|', CAST(DATE(ts) AS STRING))) AS k
  FROM base
  WHERE event_name = 'rewarded_ad_started'
),
terminal AS (
  SELECT DISTINCT
    user_pseudo_id,
    COALESCE(paywall_impression_id, CONCAT(user_pseudo_id, '|', CAST(DATE(ts) AS STRING))) AS k
  FROM base
  WHERE event_name IN ('rewarded_ad_completed', 'rewarded_ad_failed')
)
SELECT
  SAFE_DIVIDE(COUNTIF(terminal.k IS NULL), COUNT(*)) AS rewarded_started_without_terminal_ratio
FROM started
LEFT JOIN terminal USING (user_pseudo_id, k);

-- 4) premium_purchase_started sin origin_placement
SELECT
  SAFE_DIVIDE(
    COUNTIF(origin_placement IS NULL OR origin_placement = ''),
    COUNT(*)
  ) AS premium_purchase_started_missing_origin_placement_ratio
FROM base
WHERE event_name = 'premium_purchase_started';
```

## Notas operativas

- Recomendado publicar estos guardrails en un dashboard de observabilidad separado de KPIs de negocio.
- Antes de abrir incidencias de producto, validar primero integridad de instrumentación con estos guardrails.
- Si un guardrail cae, priorizar revisión de emisiones/params en eventos upstream antes de interpretar cambios de conversión como señales de negocio.
