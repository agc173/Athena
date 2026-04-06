# Overview (BWitch)

BWitch es una app de misticismo/esoterismo con módulos como astrología, brujería, energía y comunidad.
Este repo está preparado con arquitectura limpia para escalar features sin acoplar UI/infra.

## Objetivos de arquitectura
- Escalar features por módulos
- Compartir lógica y UI en KMP
- Mantener domain puro y testeable
- Networking uniforme (BaseApi + ApiResult)
- DI limpia por capa (Koin)

## Módulos
- Astrología (horóscopo, carta astral, compatibilidades)
- Brujería (tarot, oráculo, lectura de manos)
- Energía (chakras, rituales, prácticas)
- Comunidad (foro, posts, reacciones, moderación)

## Principio clave
Las features se implementan como vertical slices:
Domain → Data → Presentation → UI

## Estado actual
- Vertical slice MVP implementado: **Horóscopo diario mock** (domain/data/presentation/UI) con tests básicos de use case y ViewModel.
- Base funcional de **Ritual del día** implementada en KMP (modelos de dominio, repositorio local, ViewModel compartido, pantalla Compose y navegación desde home de Rituales).
- MVP local/offline de **Hábitos** implementado como parte del módulo Rituales (3 intenciones diarias estables por fecha, progreso de ciclo de 60 acciones, contador de ciclos completados, pantalla Compose y navegación desde card de Rituales).
