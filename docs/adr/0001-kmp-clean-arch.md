# ADR-0001: KMP + Clean Architecture modular

## Contexto
Necesitamos una base escalable para una app KMP con múltiples features (astrología, brujería, comunidad).
La app debe permitir:
- crecer por módulos
- compartir lógica (y UI) entre Android e iOS
- evitar acoplar UI con infraestructura

## Decisión
Adoptar Clean Architecture modular:
- shared/domain: reglas de negocio puras
- shared/data: infraestructura (Ktor/Firebase) + repos impl
- shared/presentation: ViewModels/UiState compartidos
- shared/di: composición de módulos Koin e initKoin
- composeApp: UI Compose y entrypoints de plataforma

Ktor usa engines por plataforma:
- Android: OkHttp
- iOS: Darwin
  Inyectados vía platformModule(...) y consumidos desde shared/data.

## Alternativas consideradas
- Monolito en commonMain: descartado por acoplamiento y dificultad para escalar.
- UI nativa iOS y Compose solo Android: descartado de momento por velocidad; se mantiene posibilidad futura separando presentation de UI.

## Consecuencias
- Mayor disciplina de capas (domain puro).
- DI repartida: data define dataKoinModule, di solo compone.
- Menos fricción al añadir features (vertical slices).
