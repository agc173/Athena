# Template: crear una feature (vertical slice)

## 1) Domain (shared/domain)
- Crear package: com.agc.bwitch.domain.<feature>
- Crear:
    - <Feature>Repository (interface)
    - Modelos de dominio (si aplica)
    - UseCase(s): GetXxxUseCase, SaveXxxUseCase, etc.

Checklist:
- Domain NO depende de Ktor/Firebase/Koin.
- Tipos de resultado: ApiResult/NetworkError.

## 2) Data (shared/data)
- Crear package: com.agc.bwitch.data.<feature>
- Crear:
    - <Feature>Api (usa BaseApi) o RemoteDataSource
    - DTOs (si aplica) + mappers DTO→Domain
    - <Feature>RepositoryImpl : <Feature>Repository

Registrar en Koin:
- dataKoinModule:
    - single<<Feature>Repository> { <Feature>RepositoryImpl(get(), ...) }

## 3) Presentation (shared/presentation)
- Crear:
    - <Feature>UiState
    - <Feature>ViewModel
    - (Opcional) UiEvent/UiEffect

Registrar en Koin:
- PresentationModule:
    - factory { <Feature>ViewModel(get(), ...) } o viewModel { ... } según setup

## 4) UI (composeApp)
- Crear screen:
    - <Feature>Screen (Compose)
- Consumir VM:
    - collectAsState / state hoisting
- Añadir navegación si aplica.

## 5) Done Definition
- Build OK: .\gradlew build
- Pantalla se ve en Android
- Reglas de capa respetadas
- Documentación actualizada si cambia arquitectura
