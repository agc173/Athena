# Arquitectura del proyecto (Kotlin Multiplatform + Clean Architecture)

Este proyecto usa Kotlin Multiplatform (KMP) y Compose Multiplatform con una arquitectura modular basada en Clean Architecture.  
Objetivo: escalabilidad, testabilidad y separación estricta de responsabilidades.

---

## Visión general

- **UI compartida** en `composeApp` (Compose Multiplatform).
- **Lógica de negocio (domain)** aislada, sin dependencias de frameworks.
- **Implementación (data)**: networking, persistencia y repositorios concretos.
- **Presentation (shared)**: ViewModels/State compartidos (sin Compose para evitar acoplar UI).
- **DI (shared)**: inicialización y composición de módulos Koin.
- **Plataforma**: módulos por source set (`androidMain` / `iosMain`) para dependencias nativas (Ktor engines, dispatchers, flags debug).

---

## Estructura de módulos

### `composeApp/`
**Responsabilidad:** capa de aplicación y UI (Compose), navegación, temas, entrypoints.

Contiene:
- Pantallas Compose (`screens`, `components`)
- Navegación UI (cuando se elija lib: Voyager/Decompose/etc.)
- `MainActivity` (Android) / `MainViewController` (iOS host)
- `BWitchApplication` (Android Application)
- `platformModule(...)` por plataforma (Koin): engines Ktor, dispatchers, flags

No contiene:
- Reglas de negocio (eso está en `shared/domain`)
- Implementaciones de repositorios (eso está en `shared/data`)

---

### `shared/domain/`
**Responsabilidad:** reglas de negocio puras, sin frameworks.

Contiene:
- Modelos de dominio (`domain/model`)
- Interfaces de repositorio (`domain/...Repository`)
- Use cases (`domain/...UseCase`)
- Tipos de resultado/errores compartidos:
    - `ApiResult`
    - `NetworkError`

Regla: **domain no depende de nada** (ni Ktor, ni Koin, ni Firebase, ni Compose).

---

### `shared/data/`
**Responsabilidad:** implementación concreta (infraestructura).

Contiene:
- Networking:
    - `HttpClientFactory` (crea `HttpClient` usando `HttpClientEngine` inyectado)
    - `NetworkConfig`
    - `JsonProvider`
    - `BaseApi` (wrapper de requests con `safeApiCall`)
    - `safeApiCall` (mapea respuestas/errores a `ApiResult`)
- Implementaciones de repositorios (`...RepositoryImpl`) que implementan interfaces de domain
- Módulo Koin propio de data:
    - `dataKoinModule` (define `HttpClient`, `BaseApi`, config, etc.)

Regla: data depende de domain, pero **domain no depende de data**.

---

### `shared/presentation/`
**Responsabilidad:** estado y ViewModels compartidos (sin UI).

Contiene (cuando se implemente en serio):
- `UiState / UiEvent / UiEffect` (si se usa ese patrón)
- ViewModels por feature (sin Compose)
- Transformaciones de domain → state de UI

Nota: UI Compose vive en `composeApp`, para permitir a futuro UI nativa en iOS si se quisiera.

---

### `shared/di/`
**Responsabilidad:** inicialización y composición de DI (Koin) para KMP.

Contiene:
- `initKoin(additionalModules: List<Module>)`
- `DomainModule.kt` (use cases y wiring de domain)
- `PresentationModule.kt` (ViewModels y state)
- `AppModule.kt` (lista `appModules`)
- Importa módulos reales desde data (p. ej. `dataKoinModule`)

Importante: `shared:di` evita depender de Ktor/Json directamente.  
Los bindings de networking viven en `shared:data` y se importan como `Module`.

---

## DI / Koin

### Inicialización
- Android: `BWitchApplication` llama a:
    - `initKoin(additionalModules = listOf(platformModule(BuildConfig.DEBUG)))`
- iOS: se llamará a `initKoin(additionalModules = listOf(platformModule(true/false)))` desde el entry en Xcode.

### Módulos
- `platformModule(isDebug: Boolean)` (por plataforma)
    - Android:
        - `HttpClientEngine` = OkHttp
        - `CoroutineDispatcher` = Dispatchers.IO
        - `Boolean` debug flag
    - iOS:
        - `HttpClientEngine` = Darwin
        - `CoroutineDispatcher` = Dispatchers.Default
        - `Boolean` debug flag

- `dataKoinModule` (en `shared:data`)
    - `Json`
    - `NetworkConfig` (enableLogging usa el Boolean de plataforma)
    - `HttpClientFactory`
    - `HttpClient`
    - `BaseApi`

---

## Networking

- `HttpClientFactory` construye un `HttpClient` con:
    - ContentNegotiation (kotlinx.serialization Json)
    - Timeouts
    - Logging (según `NetworkConfig.enableLogging`)
    - Headers JSON por defecto

- `BaseApi` centraliza:
    - Construcción de URL (baseUrl + path)
    - Requests (ej. `get<T>`)
    - Manejo uniforme de errores usando `safeApiCall`

- `safeApiCall` devuelve siempre:
    - `ApiResult.Success<T>`
    - `ApiResult.Failure(NetworkError)`

Esto hace que los repositorios y use cases trabajen con un flujo consistente.

---

## Patrón validado: Repo → UseCase → (ViewModel) → UI

Ya se probó el wiring con un ejemplo mínimo (Ping):
- `PingRepository` (domain)
- `PingRepositoryImpl` (data)
- `PingUseCase` (domain)
- Registrado en Koin y compilando

---

## Reglas y convenciones

- **No mezclar UI con data/domain.**
- **Domain es puro.**
- Los engines (OkHttp/Darwin), dispatchers y flags de build se definen en source sets de plataforma.
- Los módulos Koin “de capa” viven con su capa (por ejemplo `dataKoinModule` vive en data).
- `shared:di` compone, no conoce detalles de infraestructura.

---

## Próximos pasos recomendados

1. Definir navegación (Voyager / Decompose).
2. Crear el primer vertical slice real:
    - Horóscopo diario o Tarot carta del día.
3. Añadir persistencia local (SQLDelight) cuando toque (historial, favoritos, rachas).
4. Definir reglas de Firestore + moderación para comunidad desde el inicio.
