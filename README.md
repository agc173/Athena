# ATHENA

**ATHENA** es una aplicación mobile multiplataforma orientada a astrología, tarot y autoconocimiento. El proyecto está desarrollado con **Kotlin Multiplatform (KMP)** y **Compose Multiplatform**, con una arquitectura modular pensada para separar lógica de dominio, acceso a datos, estado de presentación e interfaz de usuario.

El repositorio forma parte de una entrega académica de **Trabajo Fin de Máster (TFM)** y está preparado para revisión técnica, evaluación funcional y evolución posterior como producto mobile.

> **Estado de publicación:** la versión Android se encuentra actualmente en **Google Play Closed Testing**. La versión iOS está preparada parcialmente y queda pendiente de validación final en entorno Apple/Xcode y TestFlight.

---

## Descripción general

ATHENA propone una experiencia mobile de bienestar y autoconocimiento basada en contenido astrológico, dinámicas de tarot/oráculo, rituales y módulos de perfil personal. La app combina una interfaz compartida en Compose Multiplatform con servicios backend en Firebase y Cloud Functions para funcionalidades dinámicas, persistencia, autenticación, notificaciones y generación de contenido.

El objetivo técnico principal es demostrar una app mobile realista, modular y mantenible, capaz de compartir una parte significativa del código entre Android e iOS sin renunciar a integraciones nativas cuando son necesarias.

---

## Problema que resuelve

Las aplicaciones de astrología y autoconocimiento suelen mezclar contenido estático, experiencias poco personalizadas y arquitecturas difíciles de escalar. ATHENA aborda este problema mediante:

- Una base **multiplataforma** para reducir duplicidad entre Android e iOS.
- Separación estricta por capas para mantener el dominio independiente de infraestructura.
- Integración con Firebase para persistencia, autenticación, funciones backend y servicios cloud.
- Módulos extensibles que permiten incorporar nuevas experiencias de astrología, tarot, economía interna y personalización.
- Preparación para distribución real en stores, empezando por Android en Closed Testing.

---

## Funcionalidades principales

> Algunas funcionalidades dependen de configuración privada de Firebase, Cloud Functions y variables de entorno que no se versionan en el repositorio público.

- **Autenticación y perfil de usuario** mediante Firebase.
- **Horóscopo diario y por periodos** con soporte backend para generación/consulta de contenido.
- **Tarot y oráculo** como experiencias de consulta e introspección.
- **Rituales, hábitos y progreso** vinculados a dinámicas de autoconocimiento.
- **Economía interna** para desbloqueos, recompensas y progreso de usuario.
- **Notificaciones push** para recordatorios y contenido diario, condicionadas a configuración de Firebase/FCM.
- **Compras y monetización Android** mediante Google Play Billing y AdMob, con IDs inyectados fuera del código fuente.
- **Arquitectura preparada para iOS**, aunque varias integraciones nativas requieren validación final antes de distribución.

---

## Arquitectura

El proyecto sigue una aproximación de **Clean Architecture modular** en KMP:

```text
UI Compose / Entrypoints
        ↓
Presentation / ViewModels / UiState
        ↓
Domain / modelos / contratos / casos de uso
        ↓
Data / repositorios / APIs / Firebase / DTOs / mappers
```

Principios relevantes:

- `shared/domain` permanece puro: modelos, contratos, tipos base y casos de uso sin dependencias de Ktor, Firebase, Koin ni detalles de plataforma.
- `shared/data` implementa los contratos de dominio y concentra infraestructura: red, Firebase, repositorios concretos, DTOs y mappers.
- `shared/presentation` contiene estado y ViewModels compartidos, sin UI Compose.
- `shared/di` compone los módulos de Koin y delega bindings específicos a cada capa/plataforma.
- `composeApp` contiene la UI compartida Compose y los entrypoints Android/iOS.
- `functions` contiene Cloud Functions TypeScript para lógica backend, tareas programadas, callables y validaciones.

La documentación de apoyo se encuentra en `docs/`, incluyendo contratos API, esquema Firestore, reglas, decisiones arquitectónicas y auditorías de lanzamiento.

---

## Stack tecnológico

### Mobile

- **Kotlin Multiplatform**
- **Compose Multiplatform**
- **Koin** para inyección de dependencias
- **Ktor Client** para networking multiplataforma
- **kotlinx-serialization** para JSON
- **GitLive Firebase SDK** para integración Firebase en código compartido
- **Google Play Billing** para compras Android
- **Google Mobile Ads / AdMob** para anuncios recompensados Android

### Backend / Cloud

- **Firebase Authentication**
- **Cloud Firestore**
- **Cloud Functions for Firebase** con TypeScript
- **Firebase Storage**
- **Firebase Cloud Messaging** para notificaciones
- **Firebase Emulators** para desarrollo local cuando aplica

### Tooling

- **Gradle Kotlin DSL**
- **Android Gradle Plugin**
- **Node.js 22** para Cloud Functions
- **TypeScript**
- **ESLint**

---

## Instalación

### Requisitos previos

- JDK compatible con Gradle/Android.
- Android Studio para ejecutar y depurar Android.
- SDK de Android instalado.
- Node.js **22** para `functions/`.
- Firebase CLI si se van a ejecutar emuladores o desplegar funciones.
- Xcode y entorno macOS para validación iOS.

### Clonar el repositorio

```bash
git clone <repository-url>
cd BWitch
```

### Instalar dependencias de Cloud Functions

```bash
npm --prefix functions install
```

---

## Configuración de entorno

Por seguridad, el repositorio no versiona credenciales ni configuración sensible. Para ejecutar todas las funcionalidades se requiere aportar la configuración privada correspondiente al entorno de evaluación, desarrollo o producción.

Archivos y secretos no versionados:

- `google-services.json`: configuración Firebase Android. No está versionado.
- `GoogleService-Info.plist`: configuración Firebase iOS. No está versionado.
- `.env` y `.env.*`: variables locales. No están versionadas.
- `DEEPSEEK_API_KEY`: se gestiona como secreto de Cloud Functions/Firebase y no debe incluirse en el repositorio.
- `ADMOB_APP_ID`: se inyecta mediante Gradle properties o variable de entorno para builds release.
- `ADMOB_REWARDED_AD_UNIT_ID`: se inyecta mediante Gradle properties o variable de entorno para builds release.
- Keystores, certificados, service accounts y claves privadas deben mantenerse fuera de Git.

Ejemplo de inyección local para release Android mediante variables de entorno:

```bash
export ADMOB_APP_ID="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"
export ADMOB_REWARDED_AD_UNIT_ID="ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz"
```

También pueden definirse como propiedades de Gradle en un entorno seguro, por ejemplo en `~/.gradle/gradle.properties` o mediante `-P` en CI.

---

## Ejecución local

### Android debug

Compila la variante debug de Android:

```bash
./gradlew :composeApp:assembleDebug
```

En Windows PowerShell:

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

### Android release / AAB

La variante release exige `ADMOB_APP_ID` y `ADMOB_REWARDED_AD_UNIT_ID` como Gradle properties o variables de entorno.

Generar APK release:

```bash
ADMOB_APP_ID="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" \
ADMOB_REWARDED_AD_UNIT_ID="ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz" \
./gradlew :composeApp:assembleRelease
```

Generar Android App Bundle (`.aab`) para distribución:

```bash
ADMOB_APP_ID="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" \
ADMOB_REWARDED_AD_UNIT_ID="ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz" \
./gradlew :composeApp:bundleRelease
```

> La firma de release y los ficheros de credenciales asociados deben configurarse fuera del repositorio.

### iOS

La app iOS se abre desde el proyecto `iosApp/` en Xcode. El estado actual es parcial: la estructura KMP y el host iOS existen, pero la validación final requiere macOS/Xcode, configuración Firebase iOS, signing/capabilities y pruebas en simulador/dispositivo.

Pasos orientativos:

```bash
open iosApp/iosApp.xcodeproj
```

Desde Xcode se debe seleccionar el target iOS, configurar equipo de firma, añadir `GoogleService-Info.plist` al target cuando corresponda y ejecutar en simulador o dispositivo.

### Cloud Functions

Instalar dependencias:

```bash
npm --prefix functions install
```

Compilar TypeScript:

```bash
npm --prefix functions run build
```

Ejecutar selftests:

```bash
npm --prefix functions run test:selftests
```

Ejecutar lint:

```bash
npm --prefix functions run lint
```

Levantar emulador de Functions usando el script existente:

```bash
npm --prefix functions run serve
```

Levantar emuladores Firebase definidos en `firebase.json`:

```bash
firebase emulators:start
```

O limitar a servicios concretos:

```bash
firebase emulators:start --only functions,firestore,auth,storage,pubsub
```

---

## Estructura del proyecto

```text
.
├── composeApp/              # UI Compose compartida y entrypoints Android/iOS
├── iosApp/                  # Proyecto host iOS en Xcode
├── shared/
│   ├── domain/              # Modelos, contratos, casos de uso y tipos base
│   ├── data/                # Repositorios, Firebase, networking, DTOs y mappers
│   ├── presentation/        # ViewModels, UiState y lógica de presentación compartida
│   └── di/                  # Composición de módulos Koin
├── functions/               # Cloud Functions TypeScript
├── docs/
│   ├── api/                 # Contratos OpenAPI
│   ├── data/                # Firestore/Storage schema, rules e índices
│   ├── adr/                 # Decisiones arquitectónicas
│   ├── launch/              # Auditorías y preparación de lanzamiento
│   └── qa/                  # Auditorías de pruebas/calidad
├── gradle/                  # Gradle Wrapper y catálogos
├── firebase.json            # Configuración Firebase/emuladores
└── README.md                # Documentación principal del repositorio
```

---

## Estado actual

- **Android:** en Google Play **Closed Testing**.
- **iOS:** preparado parcialmente; pendiente de validación final en macOS/Xcode, configuración Firebase iOS, signing/capabilities y QA específico.
- **Backend Firebase:** Cloud Functions y emuladores configurados en el repositorio; el funcionamiento completo depende de secretos y configuración privada.
- **Credenciales de evaluación:** no se publican en el repositorio por seguridad.
- **Repositorio público:** preparado para revisión académica y técnica sin exponer claves, credenciales demo ni secretos.

---

## Demo y acceso de evaluación

El acceso a la app para evaluación académica se proporciona mediante el canal o formulario académico correspondiente. Por seguridad, este README no incluye:

- Credenciales demo.
- Cuentas de prueba.
- Emails privados personales.
- Claves API.
- Secretos Firebase/Cloud Functions.
- IDs privados de monetización o firma.

La versión Android está disponible para testers autorizados a través de Google Play Closed Testing. El acceso iOS queda sujeto a la validación final y, en su caso, a la preparación de TestFlight.

---

## Roadmap

- Completar validación iOS en macOS/Xcode.
- Finalizar configuración Firebase iOS (`GoogleService-Info.plist`, providers, signing y capabilities).
- Preparar distribución iOS en TestFlight si procede.
- Reforzar QA automatizada de flujos críticos.
- Consolidar pruebas de Cloud Functions y emuladores.
- Revisar observabilidad, analítica, errores y métricas de producto.
- Evolucionar los módulos de astrología, tarot/oráculo, progreso y personalización.
- Completar hardening de release Android tras Closed Testing.

---

## Autor / contexto académico

Proyecto desarrollado como parte de un **Trabajo Fin de Máster (TFM)**. El repositorio se entrega con enfoque académico y profesional, priorizando:

- Claridad arquitectónica.
- Separación de responsabilidades.
- Seguridad en la gestión de secretos.
- Preparación para evaluación externa.
- Trazabilidad documental de API, Firestore, arquitectura y lanzamiento.

Las credenciales, accesos de prueba y datos sensibles se gestionan por canales académicos externos y no forman parte del repositorio público.
