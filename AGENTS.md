# AGENTS.md — Contexto para agentes (Copilot/Codex)

Este repo es una app KMP (Kotlin Multiplatform) con Compose Multiplatform y Clean Architecture modular.

## Stack
- Kotlin Multiplatform (KMP)
- Compose Multiplatform (UI compartida)
- DI: Koin
- Networking: Ktor (OkHttp Android, Darwin iOS)
- JSON: kotlinx-serialization
- Firebase: GitLive Firebase SDK

## Mapa de módulos (alto nivel)
- composeApp/  → UI Compose + entrypoints Android/iOS (host) + platform DI
- shared/domain/ → modelos + interfaces (repos) + use cases + tipos base (ApiResult)
- shared/data/   → implementaciones de repos + networking (HttpClientFactory, BaseApi, etc.)
- shared/presentation/ → ViewModels/state compartidos (sin UI Compose)
- shared/di/     → initKoin + composición de módulos Koin (sin depender de Ktor)

## Reglas de oro
1) domain es PURO:
    - No Ktor, no Firebase, no Koin, no coroutines “infra” (solo lógica).
2) data implementa interfaces de domain:
    - repos concretos, apis, dtos, mappers.
3) presentation contiene ViewModels/UiState:
    - NO Compose UI aquí.
4) composeApp contiene SOLO UI:
    - pantallas, navegación, temas; no lógica de negocio.
5) DI:
    - Los bindings de networking viven en shared/data (dataKoinModule).
    - shared/di solo compone módulos: appModules + platformModule(...)
6) Ktor:
    - El HttpClientEngine se inyecta desde platformModule (androidMain/iosMain).
    - No referenciar engines directamente desde commonMain.
7) API:
    - Los contratos de API están en docs/api/openapi.yaml (no inventar endpoints)
8) Firestore:
    - Firestore schema: docs/data/firestore/schema.md
    - Firestore rules: docs/data/firestore/rules.md

## Networking (estándar del repo)
- HttpClientFactory crea HttpClient con ContentNegotiation + timeouts + logging (según NetworkConfig)
- BaseApi centraliza requests y devuelve ApiResult<T>
- ApiResult/NetworkError viven en shared/domain

## Cómo ejecutar / build (Windows PowerShell)
- Build completo:
    - .\gradlew build
- Compilar un módulo:
    - .\gradlew :shared:data:compileCommonMainKotlinMetadata

## Cómo añadir una feature (checklist mínimo)
1) domain:
    - interface Repository (p.ej. HoroscopeRepository)
    - modelos dominio (si aplican)
    - UseCase(s)
2) data:
    - Api/RemoteDataSource usando BaseApi
    - DTOs + mappers (DTO → Domain)
    - RepositoryImpl implementando interface de domain
    - Registrar en dataKoinModule
3) presentation:
    - ViewModel + UiState + (UiEvent/UiEffect si procede)
    - Registrar en PresentationModule
4) composeApp:
    - Pantalla Compose consumiendo el VM
    - Navegación (si ya existe)

## Qué NO debe hacer un agente
- No mover capas ni inventar estructura nueva sin documentarlo (ADR).
- No añadir dependencias/gradle salvo necesidad real y documentada.
- No meter lógica de negocio en composeApp.
- No meter Ktor/Firebase en domain.

---

## Documentation Update Protocol

La documentación es obligatoria y forma parte del Definition of Done.

Antes de cerrar una feature o hacer merge:

1. Revisar impacto en:
    - API (docs/api/openapi.yaml)
    - Firestore schema
    - Arquitectura
    - Modelos públicos

2. Aplicar reglas de:
    - docs/context/07-doc-update-rules.md

3. Si hay decisión arquitectónica:
    - Crear nuevo ADR en docs/adr/

4. Si se utiliza un agente IA:
    - El agente debe analizar el diff
    - Actualizar únicamente documentación necesaria
    - No modificar código productivo

El código y la documentación deben estar sincronizados.

