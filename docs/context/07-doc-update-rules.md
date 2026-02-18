# Documentation Update Rules (Obligatorio)

Este documento define cuándo y cómo se debe actualizar la documentación.

La documentación es parte del Definition of Done.

---

# 1. Principio general

Si un cambio afecta a:

- Contratos API
- Esquema Firestore / Base de datos
- Arquitectura / estructura de módulos
- Naming público
- Flujo de autenticación
- Modelos de dominio expuestos

→ La documentación debe actualizarse en el mismo PR.

---

# 2. Reglas específicas por tipo de cambio

## 2.1 API (OpenAPI)

Si se:
- Añade endpoint
- Cambia request/response
- Añade o elimina campos
- Cambia códigos de error
- Cambia auth o headers

Entonces actualizar:
- docs/api/openapi.yaml
- docs/api/README.md (si afecta uso)

No inventar endpoints fuera de openapi.yaml.

---

## 2.2 Firestore

Si se:
- Añade colección
- Añade o elimina campo
- Cambia tipo de campo
- Cambia reglas de acceso
- Cambia estructura de subcolecciones

Entonces actualizar:
- docs/data/firestore/schema.md
- docs/data/firestore/examples.md
- docs/data/firestore/rules.md (si aplica)
- docs/data/firestore/indexes.md (si aplica)

---

## 2.3 Arquitectura

Si se:
- Cambia estructura de módulos
- Añade nuevo módulo compartido
- Cambia patrón (ej: MVVM → MVI)
- Cambia estrategia de networking
- Cambia DI significativa

Entonces:
- Actualizar ARCHITECTURE.md
- Crear nuevo ADR en docs/adr/
  Formato: 000X-titulo.md

---

## 2.4 Feature nueva

Al añadir una nueva feature:

- Verificar que sigue 06-feature-template.md
- Añadir entrada breve en:
    - docs/context/00-overview.md (si es módulo principal)
    - docs/context/02-domain-model.md (si existe)

---

## 2.5 Changelog interno

Cada sesión de trabajo significativa debe actualizar:

docs/changelog/<YYYY-MM>.md

Formato:
- Fecha
- Qué se hizo
- Impacto arquitectónico (si aplica)
- Pendientes

---

# 3. Protocolo para agentes IA

Cuando se use un agente:

1. Revisar git diff.
2. Determinar impacto en documentación.
3. Actualizar únicamente archivos bajo:
    - docs/
    - AGENTS.md (si es necesario)
4. No modificar código productivo.
5. No inventar información no presente en el diff.

---

# 4. Prohibiciones

- No modificar documentación si el cambio no afecta contratos/estructura.
- No introducir campos en documentación que no existan en código.
- No crear ADR innecesarios.
