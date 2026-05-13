# KPIs de negocio — analytics-funnels-v1

Fecha: 2026-04-28  
Versión base: `analytics-funnels-v1`

## Objetivo

Definir métricas de negocio accionables usando **solo** el sistema actual de eventos/params de monetización:

Eventos:
- `paywall_shown`
- `paywall_action_clicked`
- `rewarded_ad_started`
- `rewarded_ad_completed`
- `rewarded_ad_failed`
- `content_unlock_attempt`
- `content_unlocked`
- `content_unlock_failed`
- `premium_cta_shown`
- `premium_cta_clicked`
- `premium_purchase_started`
- `premium_purchase_completed`
- `premium_purchase_failed`
- `premium_restore_clicked`
- `premium_restore_completed`
- `premium_restore_empty`

Params clave:
- `paywall_impression_id`
- `unlock_flow_origin`
- `origin_placement`

---

## Supuestos de consulta (BigQuery style)

- Dataset de eventos tipo GA4 export (o equivalente), con `event_name`, `event_timestamp`, `user_pseudo_id` y `event_params`.
- Ventana temporal explícita (ejemplo: últimos 30 días).
- Para funnels con secuencia, se recomienda usar:
  - correlación por `paywall_impression_id` cuando exista,
  - y/o secuencia temporal por usuario dentro de una ventana de sesión.

Snippet base para extraer params (referencia):

```sql
WITH base AS (
  SELECT
    event_date,
    TIMESTAMP_MICROS(event_timestamp) AS ts,
    user_pseudo_id,
    event_name,
    (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'paywall_impression_id') AS paywall_impression_id,
    (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'unlock_flow_origin') AS unlock_flow_origin,
    (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'origin_placement') AS origin_placement,
    (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'action') AS action
  FROM `project.dataset.events_*`
  WHERE _TABLE_SUFFIX BETWEEN '20260401' AND '20260430'
)
```

---

## Métricas clave (10)

## 1) Paywall → click en “watch_ad” (CTR de intención rewarded)

**Definición**  
Porcentaje de impresiones de paywall que generan click de acción `watch_ad`.

**Eventos usados**  
- Numerador: `paywall_action_clicked` con `action = 'watch_ad'`
- Denominador: `paywall_shown`
- Join recomendado: `paywall_impression_id`

**Cálculo (pseudo SQL)**

```sql
WITH pw AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'paywall_shown'
    AND paywall_impression_id IS NOT NULL
),
ad_click AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'paywall_action_clicked'
    AND action = 'watch_ad'
    AND paywall_impression_id IS NOT NULL
)
SELECT
  SAFE_DIVIDE(COUNT(ad_click.paywall_impression_id), COUNT(pw.paywall_impression_id)) AS paywall_to_watch_ad_ctr
FROM pw
LEFT JOIN ad_click USING (paywall_impression_id);
```

---

## 2) Conversión Paywall → Rewarded Started

**Definición**  
Porcentaje de impresiones de paywall que terminan en inicio de rewarded ad.

**Eventos usados**  
- `paywall_shown`
- `rewarded_ad_started`
- `paywall_impression_id`

**Cálculo**

```sql
WITH pw AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'paywall_shown'
    AND paywall_impression_id IS NOT NULL
),
rw_start AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'rewarded_ad_started'
    AND paywall_impression_id IS NOT NULL
)
SELECT SAFE_DIVIDE(COUNT(rw_start.paywall_impression_id), COUNT(pw.paywall_impression_id))
  AS paywall_to_rewarded_started_cr
FROM pw
LEFT JOIN rw_start USING (paywall_impression_id);
```

---

## 3) Conversión Paywall → Rewarded Completed

**Definición**  
Porcentaje de impresiones de paywall que resultan en rewarded completado.

**Eventos usados**  
- `paywall_shown`
- `rewarded_ad_completed`
- `paywall_impression_id`

**Cálculo**

```sql
WITH pw AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'paywall_shown'
    AND paywall_impression_id IS NOT NULL
),
rw_done AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'rewarded_ad_completed'
    AND paywall_impression_id IS NOT NULL
)
SELECT SAFE_DIVIDE(COUNT(rw_done.paywall_impression_id), COUNT(pw.paywall_impression_id))
  AS paywall_to_rewarded_completed_cr
FROM pw
LEFT JOIN rw_done USING (paywall_impression_id);
```

---

## 4) Conversión Rewarded Started → Rewarded Completed

**Definición**  
Tasa de éxito técnico/UX del flujo rewarded (completion rate).

**Eventos usados**  
- `rewarded_ad_started`
- `rewarded_ad_completed`
- correlación por `user_pseudo_id` + ventana temporal o `paywall_impression_id` cuando existe

**Cálculo (aprox por sesión/paywall_id)**

```sql
WITH started AS (
  SELECT DISTINCT user_pseudo_id, COALESCE(paywall_impression_id, CONCAT(user_pseudo_id, '|', CAST(DATE(ts) AS STRING))) AS k
  FROM base
  WHERE event_name = 'rewarded_ad_started'
),
completed AS (
  SELECT DISTINCT user_pseudo_id, COALESCE(paywall_impression_id, CONCAT(user_pseudo_id, '|', CAST(DATE(ts) AS STRING))) AS k
  FROM base
  WHERE event_name = 'rewarded_ad_completed'
)
SELECT SAFE_DIVIDE(COUNT(completed.k), COUNT(started.k)) AS rewarded_start_to_completed_cr
FROM started
LEFT JOIN completed USING (user_pseudo_id, k);
```

---

## 5) Drop-off de Rewarded Ads

**Definición**  
Distribución de salidas del flujo rewarded después de `rewarded_ad_started`:
- completado,
- fallido,
- sin evento terminal observado.

**Eventos usados**  
- `rewarded_ad_started`
- `rewarded_ad_completed`
- `rewarded_ad_failed`

**Cálculo**

```sql
WITH starts AS (
  SELECT DISTINCT user_pseudo_id, COALESCE(paywall_impression_id, CONCAT(user_pseudo_id, '|', CAST(DATE(ts) AS STRING))) AS k
  FROM base
  WHERE event_name = 'rewarded_ad_started'
),
completed AS (
  SELECT DISTINCT user_pseudo_id, COALESCE(paywall_impression_id, CONCAT(user_pseudo_id, '|', CAST(DATE(ts) AS STRING))) AS k
  FROM base
  WHERE event_name = 'rewarded_ad_completed'
),
failed AS (
  SELECT DISTINCT user_pseudo_id, COALESCE(paywall_impression_id, CONCAT(user_pseudo_id, '|', CAST(DATE(ts) AS STRING))) AS k
  FROM base
  WHERE event_name = 'rewarded_ad_failed'
)
SELECT
  SAFE_DIVIDE(COUNTIF(completed.k IS NOT NULL), COUNT(*)) AS pct_completed,
  SAFE_DIVIDE(COUNTIF(failed.k IS NOT NULL), COUNT(*)) AS pct_failed,
  SAFE_DIVIDE(COUNTIF(completed.k IS NULL AND failed.k IS NULL), COUNT(*)) AS pct_no_terminal
FROM starts
LEFT JOIN completed USING (user_pseudo_id, k)
LEFT JOIN failed USING (user_pseudo_id, k);
```

---

## 6) Conversión Rewarded Completed → Content Unlocked

**Definición**  
Porcentaje de rewarded completados que terminan en unlock exitoso atribuido a `paywall_rewarded`.

**Eventos usados**  
- `rewarded_ad_completed`
- `content_unlocked` con `unlock_flow_origin = 'paywall_rewarded'`
- `paywall_impression_id` (preferido)

**Cálculo**

```sql
WITH rw_done AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'rewarded_ad_completed'
    AND paywall_impression_id IS NOT NULL
),
unlocks AS (
  SELECT DISTINCT paywall_impression_id
  FROM base
  WHERE event_name = 'content_unlocked'
    AND unlock_flow_origin = 'paywall_rewarded'
    AND paywall_impression_id IS NOT NULL
)
SELECT SAFE_DIVIDE(COUNT(unlocks.paywall_impression_id), COUNT(rw_done.paywall_impression_id))
  AS rewarded_to_unlock_cr
FROM rw_done
LEFT JOIN unlocks USING (paywall_impression_id);
```

---

## 7) Unlock rate por origen de flujo

**Definición**  
Porcentaje de intentos de unlock que terminan en éxito, segmentado por `unlock_flow_origin`.

**Eventos usados**  
- `content_unlock_attempt`
- `content_unlocked`
- `content_unlock_failed`
- `unlock_flow_origin`

**Cálculo**

```sql
WITH attempts AS (
  SELECT
    COALESCE(unlock_flow_origin, 'unknown') AS origin,
    COUNT(*) AS attempts
  FROM base
  WHERE event_name = 'content_unlock_attempt'
  GROUP BY 1
),
unlocks AS (
  SELECT
    COALESCE(unlock_flow_origin, 'unknown') AS origin,
    COUNT(*) AS unlocked
  FROM base
  WHERE event_name = 'content_unlocked'
  GROUP BY 1
)
SELECT
  a.origin,
  a.attempts,
  COALESCE(u.unlocked, 0) AS unlocked,
  SAFE_DIVIDE(COALESCE(u.unlocked, 0), a.attempts) AS unlock_rate
FROM attempts a
LEFT JOIN unlocks u USING (origin)
ORDER BY a.attempts DESC;
```

> Esta métrica cubre explícitamente el corte solicitado: `direct_balance` vs `paywall_rewarded` vs `premium`.

---

## 8) Mix de unlock exitoso por origen

**Definición**  
Distribución porcentual de todos los `content_unlocked` entre orígenes (`direct_balance`, `paywall_rewarded`, `premium`, etc.).

**Eventos usados**  
- `content_unlocked`
- `unlock_flow_origin`

**Cálculo**

```sql
SELECT
  COALESCE(unlock_flow_origin, 'unknown') AS origin,
  COUNT(*) AS unlocked,
  SAFE_DIVIDE(COUNT(*), SUM(COUNT(*)) OVER()) AS unlock_share
FROM base
WHERE event_name = 'content_unlocked'
GROUP BY 1
ORDER BY unlocked DESC;
```

---

## 9) Conversión CTA premium → Purchase Started

**Definición**  
Porcentaje de clicks en CTA premium que inician flujo de compra.

**Eventos usados**  
- `premium_cta_clicked`
- `premium_purchase_started`
- segmentación por `origin_placement`

**Cálculo**

```sql
WITH clicks AS (
  SELECT COALESCE(origin_placement, 'unknown') AS origin_placement, COUNT(*) AS cta_clicks
  FROM base
  WHERE event_name = 'premium_cta_clicked'
  GROUP BY 1
),
started AS (
  SELECT COALESCE(origin_placement, 'unknown') AS origin_placement, COUNT(*) AS purchase_started
  FROM base
  WHERE event_name = 'premium_purchase_started'
  GROUP BY 1
)
SELECT
  c.origin_placement,
  c.cta_clicks,
  COALESCE(s.purchase_started, 0) AS purchase_started,
  SAFE_DIVIDE(COALESCE(s.purchase_started, 0), c.cta_clicks) AS cta_to_purchase_started_cr
FROM clicks c
LEFT JOIN started s USING (origin_placement)
ORDER BY c.cta_clicks DESC;
```

---

## 10) Conversión Purchase Started → Purchase Completed (premium)

**Definición**  
Tasa de cierre del checkout premium una vez iniciado.

**Eventos usados**  
- `premium_purchase_started`
- `premium_purchase_completed`
- `premium_purchase_failed`
- idealmente por `product_id` + usuario + ventana temporal

**Cálculo**

```sql
WITH st AS (
  SELECT user_pseudo_id, event_date,
         (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'product_id') AS product_id
  FROM `project.dataset.events_*`
  WHERE event_name = 'premium_purchase_started'
),
ok AS (
  SELECT user_pseudo_id, event_date,
         (SELECT ep.value.string_value FROM UNNEST(event_params) ep WHERE ep.key = 'product_id') AS product_id
  FROM `project.dataset.events_*`
  WHERE event_name = 'premium_purchase_completed'
)
SELECT
  SAFE_DIVIDE(COUNT(ok.product_id), COUNT(st.product_id)) AS purchase_start_to_completed_cr
FROM st
LEFT JOIN ok
  USING (user_pseudo_id, event_date, product_id);
```

---

## Gaps de tracking detectados (sin inventar features)

1. **Cobertura parcial de `paywall_impression_id`**  
   Si no está presente en todos los eventos del flujo (`paywall_shown`, `paywall_action_clicked`, `rewarded_*`, `content_unlock_*`), se degrada la atribución y se recurre a heurísticas de tiempo/usuario.

2. **Ambigüedad en joins de rewarded sin clave única de intento**  
   Para sesiones con múltiples rewarded seguidos, `started/completed/failed` puede sobre-atribuir o sub-atribuir al no existir una clave explícita de intento de ad.

3. **`origin_placement` puede estar incompleto en premium**  
   Sin este param en todos los `premium_*` relevantes, se limita análisis por contexto (settings, paywall u otros placements).

4. **`unlock_flow_origin` faltante o `unknown` alto**  
   Reduce la calidad de métricas de mix/conversión por origen (`direct_balance` vs `paywall_rewarded` vs `premium`).

5. **No hay identificador de sesión estándar en el set mínimo descrito**  
   Para funnels multi-step sin `paywall_impression_id`, la precisión depende de ventanas temporales aproximadas.

6. **Riesgo de doble conteo por reintentos**  
   Métricas basadas en `COUNT(*)` pueden inflarse si no se normaliza por entidad de funnel (ej. impresión, intento o usuario).

---

## Recomendación operativa de lectura

- Reportar siempre cada KPI en dos vistas:
  - **Event-based** (volumen total), y
  - **Entity-based** (por `paywall_impression_id` o usuario único).
- Añadir corte por `origin_placement` y `unlock_flow_origin` como dimensiones por defecto.
- En dashboards, mostrar junto a cada KPI su `% de eventos con params clave presentes` (data quality guardrail).
