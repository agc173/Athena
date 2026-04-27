# Auditoría y diseño v2 — variabilidad de horóscopo diario

Fecha: 2026-04-27
Estado: propuesta lista para implementación (sin cambios productivos en este documento)

## 1) Diagnóstico del estado actual

### 1.1 Prompt diario: ausencia de seed determinista explícita
- El prompt diario actual (`horoscopeCanonicalUserPrompt`) solo envía `date` y `sign`.
- No existe un campo `seed` explícito ni instrucciones de variabilidad fuertes para daily.
- Weekly/Monthly sí envían `seed` con versión (`weekly-v1`, `monthly-v1`) y contexto adicional.

**Conclusión**: no hay mecanismo explícito de diversificación controlada en daily comparable al de weekly/monthly.

### 1.2 Historial previo: no se usa en daily
- `HoroscopeGenerator.generateCanonical` no consulta Firestore para recuperar documentos previos del mismo signo.
- En contraste, `PeriodHoroscopeGenerator` sí carga `previous` (N=1) para weekly/monthly.

**Conclusión**: daily carece de memoria corta anti-repetición.

### 1.3 Blacklist de clichés: no existe en daily
- Prompt diario no incluye frases prohibidas/banlist.
- Weekly/Monthly sí incluyen una instrucción genérica de “avoid repeated cliches”, pero no lista concreta.

**Conclusión**: el modelo no recibe guardrails concretos para evitar fórmulas repetidas.

### 1.4 Estructura tonal: muy estable
- El esquema diario exige siempre: `text`, `mood`, `luckyNumber`, `luckyColor`, `shareText`.
- Esa estructura fija no es problema en sí, pero sin “angle/tema del día” tiende a producir una voz parecida cada día.

**Conclusión**: falta un driver de tema diario para variar enfoque manteniendo contrato.

### 1.5 Temperatura / maxTokens
- Daily usa `ENV.LLM_TEMPERATURE` (default 0.4) y `ENV.LLM_MAX_TOKENS` (default 350).
- 0.4 favorece consistencia; combinado con prompt corto y sin contexto dinámico favorece similitud semántica.

**Conclusión**: la configuración actual privilegia estabilidad por encima de novedad.

### 1.6 Canonical ES + traducciones
- Flujo actual: generar canónico en español y luego traducir a otros idiomas.
- Esto hace que todos los idiomas compartan el mismo contenido base (esperado y deseable para coherencia).

**Conclusión**: no es la causa del problema de repetición intradiaria; sí garantiza consistencia cross-lang.

---

## 2) Causa raíz (resumen)

La repetición viene de la combinación de:
1. Prompt daily minimalista sin seed/version/angle.
2. Ausencia de historial previo del mismo signo.
3. Temperatura relativamente conservadora (0.4).
4. Sin validación ligera anti-overlap antes de persistir.

---

## 3) Diseño propuesto (v2, bajo coste)

## 3.1 Objetivos
- Mantener arquitectura actual (canónico ES + traducciones).
- Subir variabilidad sin aumentar coste de forma significativa.
- Evitar segunda llamada LLM salvo JSON inválido/estructura inválida.

## 3.2 Cambios funcionales

### A) generatorVersion daily v2
- Introducir constante local para daily en generador, p.ej.:
  - `const DAILY_GENERATOR_VERSION = 2;`
- Persistir `generatorVersion: 2` para nuevos documentos daily canónicos y traducciones derivadas.

### B) Seed determinista requerida por prompt
- Semilla propuesta:
  - `seed = "${sign}|${dateIso}|daily-v2"`
- Añadir `seed` al user prompt canónico daily.

### C) Historial compacto (mismo signo)
- Cargar 1-2 días previos del mismo signo, preferiblemente solo canónico ES:
  - `N=1` recomendado por coste mínimo.
  - `N=2` opcional si la query sigue simple.
- Formato compacto enviado al prompt:
  - `[{dateIso, mood, luckyColor, shareText}]`

### D) Daily angle determinista
- Derivar `dailyAngle` con función pura basada en `dateIso + sign` y una lista corta de temas.
- Ejemplos de temas: `focus`, `relationships`, `boundaries`, `creativity`, `money-prudence`, `rest-recovery`, `communication`.
- Enviar `dailyAngle` en prompt para variar enfoque.

### E) Blacklist de frases comodín
- Añadir lista breve de frases prohibidas en system prompt daily, p.ej. (ES):
  - “confía en tu intuición”
  - “se abren nuevas puertas”
  - “sal de tu zona de confort”
  - “todo fluirá”
- Mantener lista pequeña (8-12) para no sobrerrestrigir.

### F) Validación ligera anti-repetición (sin 2º LLM)
- Antes de guardar canónico:
  - Comparar contra último doc (o últimos 2) del mismo signo.
  - Rechazar si `shareText` exact match o similitud de prefijo alta en `text`.
- Si falla anti-repetición: marcar warning en log y **guardar igualmente** con flag interno opcional, o endurecer política con “retry único” **solo si se acepta coste extra**.
- Recomendación bajo coste: validar + log, sin retry adicional.

### G) Mantener traducciones desde canónico ES
- No cambiar estrategia actual.
- Traducciones siguen reflejando exactamente el mismo contenido semántico por idioma.

---

## 4) Coste estimado

- Firestore extra por signo/día:
  - +1 lectura (N=1) recomendada.
  - +2 lecturas máximo si N=2.
- LLM:
  - 1 llamada para canónico ES (igual que hoy).
  - traducciones igual que hoy.
  - sin segunda llamada por repetición (solo retry por JSON/estructura inválida, ya existente por patrón de periodos).

---

## 5) Archivos a tocar (propuesta de implementación)

1. `functions/src/llm/prompts/horoscopePrompt.ts`
   - Extender prompt canónico daily con `seed`, `dailyAngle`, `previousCompact`, `clicheBlacklist`.
2. `functions/src/generators/HoroscopeGenerator.ts`
   - Añadir `DAILY_GENERATOR_VERSION = 2`.
   - Añadir helpers:
     - `deriveDailyAngle(dateIso, sign)`.
     - `loadPreviousDailyCompact(dateIso, sign, limit=1|2)`.
     - `passesLightAntiRepetition(current, previous)`.
   - Integrar nuevos campos al construir prompt en `generateCanonical`.
3. `functions/src/index.ts`
   - Sin cambios funcionales obligatorios para scheduler; solo opcional logging de versión daily v2.
4. `docs/data/firestore/schema.md`
   - Sin cambios de schema obligatorios (campo `generatorVersion` ya existe).

---

## 6) Diff sugerido (orientativo, no aplicado aquí)

```diff
--- a/functions/src/generators/HoroscopeGenerator.ts
+++ b/functions/src/generators/HoroscopeGenerator.ts
@@
+const DAILY_GENERATOR_VERSION = 2;
@@
- content: horoscopeCanonicalUserPrompt(dateIso, sign)
+ const seed = `${sign}|${dateIso}|daily-v2`;
+ const dailyAngle = deriveDailyAngle(dateIso, sign);
+ const previous = await loadPreviousDailyCompact(dateIso, sign, 1);
+ content: horoscopeCanonicalUserPrompt(dateIso, sign, seed, dailyAngle, previous)
@@
- generatorVersion: ENV.GENERATOR_VERSION,
+ generatorVersion: DAILY_GENERATOR_VERSION,
```

```diff
--- a/functions/src/llm/prompts/horoscopePrompt.ts
+++ b/functions/src/llm/prompts/horoscopePrompt.ts
@@
-export function horoscopeCanonicalUserPrompt(dateIso: string, sign: ZodiacSign) {
+export function horoscopeCanonicalUserPrompt(
+  dateIso: string,
+  sign: ZodiacSign,
+  seed: string,
+  dailyAngle: string,
+  previousCompact: Array<{dateIso: string; mood: string; luckyColor: string; shareText: string}>
+) {
   return [
     'Generate the canonical daily horoscope in Spanish for:',
     `date: ${dateIso}`,
     `sign: ${sign}`,
+    `seed: ${seed}`,
+    `dailyAngle: ${dailyAngle}`,
+    `previousHoroscopesCompact: ${JSON.stringify(previousCompact)}`,
+    'Avoid cliché phrases from the blacklist and avoid repeating previous phrasing.',
     'Output JSON with all required fields.',
   ].join('\n');
 }
```

---

## 7) Plan de validación (cuando se implemente)

1. Build/lint/typecheck en `functions`.
2. Test unitario de `deriveDailyAngle` (determinista por `dateIso+sign`).
3. Test unitario de `loadPreviousDailyCompact` (respeta N y signo).
4. Test unitario de `passesLightAntiRepetition`.
5. Dry-run local de generación para 7 días x 12 signos y revisar dispersión de `shareText`.
6. Validar que traducciones mantienen `luckyNumber` del canónico.

---

## 8) Riesgos y mitigaciones

- Riesgo: blacklist excesiva degrade naturalidad.
  - Mitigación: lista corta y revisión semanal.
- Riesgo: anti-repetición muy estricta rechace demasiado.
  - Mitigación: umbral conservador y modo “log-only” inicial.
- Riesgo: diferencia entre `ENV.GENERATOR_VERSION` global y versión local daily.
  - Mitigación: usar constante local explícita en daily y documentar en changelog.

---

## 9) Recomendación final

Implementar v2 en una sola iteración técnica pequeña:
- Prompt enriquecido (seed + angle + previous + blacklist).
- 1 lectura Firestore adicional por signo (N=1).
- Validación anti-repetición en modo log-only.
- `daily generatorVersion = 2`.

Esto maximiza mejora percibida con impacto mínimo en coste y sin tocar UI ni economía.
