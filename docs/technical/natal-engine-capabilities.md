# Natal engine capabilities

## 1. Objetivo del motor

Este documento describe la capacidad **real y actualmente integrada** del motor de carta natal de ATHENA. Su propósito es fijar el alcance técnico existente para evitar asumir funcionalidades astrológicas que todavía no están implementadas.

El motor actual cubre una **carta natal básica local**: calcula longitudes eclípticas y signos para Sol, Luna y Ascendente cuando se proporcionan fecha/hora UTC y, para Ascendente, ubicación geográfica.

Este documento es solo descriptivo: no propone soluciones, no cambia comportamiento y no amplía el alcance funcional de la aplicación.

## 2. Dependencia utilizada

El cálculo astronómico Android usa Astronomy Engine mediante la dependencia Gradle:

```kotlin
implementation("com.github.cosinekitty:astronomy:v2.1.17")
```

En código, el paquete utilizado es:

```kotlin
io.github.cosinekitty.astronomy
```

La implementación Android actual importa y usa estas APIs:

- `Time`
- `sunPosition(...)`
- `eclipticGeoMoon(...)`
- `siderealTime(...)`

## 3. Flujo del cálculo

La implementación productiva actual está en:

```text
shared/data/src/androidMain/kotlin/com/agc/bwitch/data/astrology/natal/BasicNatalChartCalculator.kt
```

Flujo actual:

1. Recibe un `BirthDateTimeUtc`.
2. Convierte ese valor a `io.github.cosinekitty.astronomy.Time`.
3. Calcula la longitud eclíptica del Sol con `sunPosition(time).elon`.
4. Calcula la longitud eclíptica de la Luna con `eclipticGeoMoon(time).lon`.
5. Si existe `BirthLocation`, calcula la longitud del Ascendente mediante:
   - `siderealTime(time)` para obtener tiempo sidéreo de Greenwich.
   - longitud geográfica para obtener tiempo sidéreo local.
   - latitud geográfica.
   - oblicuidad media fija `23.4392911°`.
   - fórmula trigonométrica local y normalización a `[0, 360)`.
6. Convierte cada longitud disponible a signo zodiacal con `longitudeToZodiacSign(...)`.
7. Devuelve un `NatalChartResult`.

## 4. Datos que calcula actualmente

El resultado público actual (`NatalChartResult`) contiene únicamente estos campos:

- `sunLongitudeDegrees`
- `sunSign`
- `moonLongitudeDegrees`
- `moonSign`
- `ascendantLongitudeDegrees`
- `ascendantSign`

Por tanto, la capacidad integrada actualmente es:

| Dato | Estado actual |
|---|---|
| Longitud eclíptica del Sol | Calculada |
| Signo solar | Calculado desde la longitud solar |
| Longitud eclíptica de la Luna | Calculada |
| Signo lunar | Calculado desde la longitud lunar |
| Longitud del Ascendente | Calculada cuando hay `BirthLocation` |
| Signo Ascendente | Calculado desde la longitud del Ascendente cuando está disponible |

## 5. Datos que NO calcula

El motor actualmente integrado **no calcula**:

- Mercurio
- Venus
- Marte
- Júpiter
- Saturno
- Urano
- Neptuno
- Plutón
- Nodo Norte
- Quirón
- Casas astrológicas
- MC / IC
- Aspectos
- Dominantes
- Elementos
- Modalidades
- Interpretación
- Rueda natal

Estos datos no forman parte del resultado público actual y no deben asumirse como disponibles.

## 6. Validación existente

Existen tests Android del `BasicNatalChartCalculator` validados contra AstroSeek para tres ubicaciones:

- Madrid
- New York
- Beijing

Los tests validan Sol, Luna y Ascendente con tolerancia de `0.25°` (`LongitudeToleranceDegrees = 0.25`).

También existe un test para el caso sin ubicación, donde se calculan Sol y Luna y el Ascendente queda ausente (`null`).

## 7. Estado Android

Android tiene implementación funcional.

La implementación está en `shared/data/src/androidMain/.../BasicNatalChartCalculator.kt` y usa Astronomy Engine dentro de `shared/data` Android. En Android, la carta natal básica puede calcular:

- Sol
- Luna
- Ascendente cuando se proporciona ubicación

## 8. Estado iOS

iOS no soporta actualmente el cálculo de carta natal.

El archivo:

```text
composeApp/src/iosMain/kotlin/com/agc/bwitch/ui/astrology/BasicNatalChartUiCalculator.ios.kt
```

implementa el `actual` de iOS devolviendo:

```kotlin
error("Basic natal chart calculation is currently available on Android only.")
```

Por tanto, aunque existe una entrada `actual` para compilar el código multiplataforma, la carta natal actualmente **no está soportada en iOS**.

## 9. Implicaciones para futuras features

### Carta básica

La carta básica puede apoyarse únicamente en los datos existentes:

- Sol
- Luna
- Ascendente, solo cuando haya ubicación válida

No debe presentar como disponibles otros planetas, casas, aspectos, MC/IC, elementos, modalidades, dominantes, rueda natal ni interpretación si no se implementan por separado.

### Esencia Natal

Cualquier uso de datos natales para Esencia Natal debe considerar que el motor local actual solo produce Sol, Luna y Ascendente. No existe cálculo local actual de planetas adicionales, casas, aspectos ni interpretación astrológica completa.

Además, el soporte actual de cálculo natal local es Android-only; iOS no dispone de cálculo funcional.

### Carta completa

Una carta completa no está soportada por el motor actual. El estado actual no cubre los elementos mínimos normalmente esperados para una carta completa, como planetas, casas, MC/IC, aspectos, dominantes, elementos, modalidades, rueda natal o interpretación.

Cualquier planificación futura de carta completa debe partir de esta limitación documentada y no asumir que esos datos ya existen en `NatalChartResult`.

## 10. Auditoría KMP Android+iOS de Astronomy Engine

### Declaración Gradle actual

La dependencia `com.github.cosinekitty:astronomy:v2.1.17` no está declarada en `gradle/libs.versions.toml`; aparece como coordenada literal en `shared/data/build.gradle.kts` dentro de `androidMain.dependencies`.

Estado observado:

- `settings.gradle.kts` habilita `mavenCentral()` y `maven("https://jitpack.io")`, por lo que la coordenada se resuelve desde los repositorios globales del proyecto cuando el entorno puede acceder a ellos.
- `shared/data` es KMP y tiene targets Android e iOS, pero Astronomy Engine está enlazada únicamente al source set Android.
- `commonMain` no tiene acceso a `io.github.cosinekitty.astronomy`.
- `iosMain` tampoco tiene un binding equivalente.

### Portabilidad de `com.github.cosinekitty:astronomy:v2.1.17`

Con la integración actual del repo, la librería debe tratarse como **Android/JVM-only para ATHENA**. No hay evidencia local de publicación KMP consumible desde `commonMain`/Kotlin Native:

- La dependencia se consume como artefacto Maven/JitPack literal, no como alias version catalog KMP ni como familia de artefactos por target.
- El código que importa `io.github.cosinekitty.astronomy` vive en `androidMain`, no en `commonMain`.
- No existe implementación iOS en `shared/data/src/iosMain` que use esa librería.
- La entrada actual de iOS en `composeApp` falla explícitamente en runtime con `error("Basic natal chart calculation is currently available on Android only.")`.

Conclusión práctica: **el motor actual no puede compartirse con iOS tal como está integrado**. Para considerar portable esta dependencia haría falta confirmar una publicación Kotlin Multiplatform/Kotlin Native real (`.module` con variantes metadata/ios o artefactos `*-metadata`/`*-ios*`) y compilar `commonMain`/`ios*` contra ella. Esa evidencia no está presente en el repo.

### Prueba segura realizada

Se hizo una prueba local no persistida moviendo temporalmente la coordenada:

```kotlin
implementation("com.github.cosinekitty:astronomy:v2.1.17")
```

desde `androidMain.dependencies` a `commonMain.dependencies` en `shared/data/build.gradle.kts`, y después se restauró el archivo para no dejar cambios funcionales.

Comando ejecutado:

```bash
./gradlew :shared:data:compileCommonMainKotlinMetadata --no-daemon
```

Resultado: la compilación no llegó a validar la portabilidad de Astronomy Engine porque el build falló antes durante la resolución del plugin `org.jetbrains.kotlin.plugin.serialization:2.3.0`. El error fue de infraestructura/resolución Gradle, no una prueba concluyente de compatibilidad de Astronomy Engine.

### Qué bloquea iOS hoy

Los bloqueos reales para iOS son:

1. **Dependencia astronómica Android-only en la integración actual**: `BasicNatalChartCalculator` depende de `io.github.cosinekitty.astronomy` desde `androidMain`.
2. **No hay implementación shared/common del cálculo**: Sol, Luna y Ascendente se calculan solo en `shared/data/src/androidMain`.
3. **El actual iOS falla explícitamente**: `BasicNatalChartUiCalculator.ios.kt` no calcula; aborta con error.
4. **No hay test iOS/common del cálculo natal básico**: la validación existente es Android.

### Opciones técnicas para conseguir cálculo básico iOS

Opciones realistas, de menor a mayor cambio estructural:

1. **Confirmar si Astronomy Engine publica variante KMP real**
   - Verificar metadata Gradle/Maven del artefacto en un entorno con acceso a Maven/JitPack.
   - Si existen variantes `commonMain`/iOS, mover la dependencia a `commonMain` y trasladar el cálculo a código común.
   - Añadir tests `commonTest`/iOS para Sol, Luna y Ascendente con las tolerancias actuales.
   - Riesgo: si el artefacto es JVM-only, esta vía queda descartada.

2. **Portar a Kotlin común solo el subconjunto necesario**
   - Implementar o adaptar fórmulas/algoritmos para Sol, Luna y tiempo sidéreo en `commonMain`, manteniendo el resultado público actual.
   - Reutilizar la fórmula local del Ascendente, sustituyendo únicamente las llamadas a Astronomy Engine por funciones comunes.
   - Validar contra los mismos casos de Madrid/New York/Beijing y tolerancia `0.25°`.
   - Riesgo: responsabilidad de precisión/mantenimiento pasa al repo. Conviene documentar fuente algorítmica y tolerancias.

3. **Usar librería astronómica KMP alternativa**
   - Evaluar una dependencia que publique Kotlin Native/iOS y cubra longitud eclíptica solar, lunar y tiempo sidéreo.
   - Mantener un adaptador interno para no acoplar dominio/UI a la librería.
   - Riesgo: precisión, licencia, tamaño binario, mantenimiento y disponibilidad real de targets iOS.

4. **Implementación native iOS separada con `expect/actual`**
   - Mantener Android con Astronomy Engine y crear una implementación iOS nativa equivalente.
   - Riesgo: dos motores que pueden divergir; requiere tests cruzados estrictos y documentación de diferencias.

5. **Cálculo backend/remoto para iOS**
   - Calcular Sol/Luna/Ascendente fuera del cliente y devolver el resultado al app.
   - Riesgo: cambia arquitectura offline/local, introduce latencia, coste, privacidad y contrato API. Requeriría diseño específico y documentación OpenAPI/seguridad.

### Decisión técnica del spike `feature/natal-basic-multiplatform`

Estado al cierre del spike: **b) no portable en la integración actual; requiere un port común o una librería KMP real**.

La opción **a) portable con Astronomy Engine** no queda aprobada para este repo porque no se ha podido demostrar que `com.github.cosinekitty:astronomy:v2.1.17` publique variantes Kotlin Multiplatform/Kotlin Native consumibles desde `commonMain`/iOS. Con el código actual, el único uso compilable y validado de Astronomy Engine sigue siendo Android/JVM desde `androidMain`.

La opción **c) alternativa recomendada** para desbloquear únicamente Sol/Luna/Ascendente en Android+iOS es:

1. Mantener `NatalChartResult` sin ampliarlo.
2. Crear un motor interno en `shared/data/src/commonMain` limitado a:
   - longitud eclíptica solar;
   - longitud eclíptica lunar;
   - tiempo sidéreo necesario para la fórmula actual del Ascendente.
3. Reutilizar la fórmula de Ascendente ya validada, sustituyendo solo las llamadas Android/JVM a Astronomy Engine por funciones comunes.
4. Mover los casos Madrid/New York/Beijing y el caso sin ubicación a `commonTest` cuando exista el motor común.
5. Mantener la tolerancia actual `0.25°` y documentar la fuente algorítmica/tolerancias antes de retirar el motor Android-only.

Hasta que ese port común compile en targets iOS, iOS debe seguir considerándose **no soportado** para cálculo natal local. No se debe cambiar el mensaje de error actual por un fallback silencioso ni prometer soporte iOS en producto.

### Evidencia de build/auditoría del spike

Comandos ejecutados durante este spike:

```bash
./gradlew :shared:data:compileCommonMainKotlinMetadata --no-daemon
curl -I -L https://repo1.maven.org/maven2/com/github/cosinekitty/astronomy/v2.1.17/astronomy-v2.1.17.pom
```

Resultados:

- `compileCommonMainKotlinMetadata` falla antes de compilar código común por resolución del plugin `org.jetbrains.kotlin.plugin.serialization:2.3.0`; por tanto, este entorno no permite validar una migración real de Astronomy Engine a `commonMain`.
- La consulta Maven directa queda bloqueada por el entorno con `CONNECT tunnel failed, response 403`; por tanto, tampoco se obtiene metadata remota concluyente desde este contenedor.
- No se movió código productivo ni se amplió alcance a carta completa, casas, aspectos, planetas adicionales, UI, economía, Esencia Natal, CSV de ciudades ni ranking de ciudades.

### Recomendación técnica

Para desbloquear Esencia Natal en Android+iOS sin prometer carta completa, el siguiente paso recomendado es un spike de implementación separado:

1. Resolver primero el bloqueo de Gradle/plugin en el entorno de build.
2. Verificar metadata real de `com.github.cosinekitty:astronomy:v2.1.17` en un entorno con acceso Maven/JitPack funcional.
3. Si no hay KMP/iOS, descartar mover la librería a `commonMain` y portar el subconjunto Sol/Luna/sidéreo a Kotlin común.
4. Mantener el alcance en Sol, Luna y Ascendente; no ampliar a planetas/casas/aspectos sin una decisión técnica separada.

## 11. Port mínimo de Astronomy Engine para Sol/Luna/Ascendente

### Fuente inspeccionada

No se encontró una copia local de `com.github.cosinekitty:astronomy:v2.1.17` en el caché Gradle del contenedor (`~/.gradle/caches`). La inspección se hizo contra la fuente Kotlin/JVM publicada en el tag upstream `v2.1.17`:

```text
https://raw.githubusercontent.com/cosinekitty/astronomy/v2.1.17/source/kotlin/src/main/kotlin/io/github/cosinekitty/astronomy/astronomy.kt
```

La implementación Kotlin/JVM de esa versión está concentrada en un único archivo `astronomy.kt`. El encabezado declara explícitamente `Astronomy Engine for Kotlin / JVM`, usa `@file:JvmName("Astronomy")` y el paquete `io.github.cosinekitty.astronomy`. La librería upstream documenta licencia MIT y objetivos de precisión de aproximadamente ±1 arcminute, basándose en VSOP87/NOVAS y tablas de coeficientes embebidas en código fuente.

### APIs usadas por ATHENA

La implementación Android actual de ATHENA solo necesita estas entradas públicas de Astronomy Engine:

| API | Uso en ATHENA | Salida usada |
|---|---|---|
| `Time(...)` | Construir instante UTC astronómico desde `BirthDateTimeUtc` | `ut`, `tt` y cachés internos usados por cálculos posteriores |
| `sunPosition(time)` | Calcular posición geocéntrica eclíptica del Sol | `Ecliptic.elon` |
| `eclipticGeoMoon(time)` | Calcular posición geocéntrica eclíptica de la Luna | `Spherical.lon` |
| `siderealTime(time)` | Calcular GAST para tiempo sidéreo local | `Double` en horas sidéreas |

La fórmula del Ascendente de ATHENA no depende de más APIs de Astronomy Engine: toma `siderealTime(time)`, longitud/latitud geográfica, oblicuidad fija local y trigonometría propia.

### Árbol interno de dependencias observado

#### `Time`

Dependencias mínimas:

- `universalTimeDays(year, month, day, hour, minute, second)`: conversión calendario UTC → días UT desde J2000.
- `terrestrialTime(ut)` y `universalTime(tt)`: conversión UT/TT mediante modelo delta-T.
- `dayValueToDateTime(ut)` solo si se necesita serializar o depurar; no es necesario para calcular Sol/Luna/sidéreo en ATHENA.
- Constantes de tiempo: `MILLISECONDS_PER_DAY`, `DAYS_PER_MILLENNIUM` y constantes auxiliares de calendario.
- Cachés mutables internos en `Time`: `psi`, `eps`, `st` para nutación/oblicuidad y sidéreo.

Portabilidad: el núcleo es matemático. El constructor desde fecha UTC usa enteros, `Double` y funciones de `kotlin.math`. El único elemento JVM-específico visible es `@JvmStatic`/`@file:JvmName` y APIs de formato/string que no son necesarias para el port mínimo.

#### `siderealTime(time)`

Dependencias mínimas:

- `time.ut` y `time.julianCenturies()`.
- `earthRotationAngle(time)`.
- `earthTilt(time)`.
- `iau2000b(time)` para calcular/cachar nutación (`psi`) y corrección de oblicuidad (`eps`).
- `meanObliquity(time)`.
- Helpers trigonométricos: `dcos`, grados↔radianes, normalización modular.
- Constantes: `ASEC360`, `ASEC2RAD` y factores grados/horas.

Tablas/coefs necesarios: no usa tablas grandes; solo la serie truncada IAU2000B de 5 términos embebida en la función `iau2000b` y el polinomio de oblicuidad media.

Portabilidad: parece matemática pura y portable a `commonMain`. No requiere JVM, I/O, red, archivos ni recursos externos.

#### `sunPosition(time)`

Dependencias mínimas:

- `time.addDays(-1.0 / C_AUDAY)` para corrección de tiempo-luz.
- `helioEarthPos(adjustedTime)`.
- `calcVsop(vsopModel(Body.Earth), time)`.
- Modelo VSOP de la Tierra (`vsopModelEarth`) con series `lon`, `lat`, `rad`.
- `vsopFormulaCalc`, `vsopSphereToRect`, `vsopRotate`.
- Tipos mínimos: `Body.Earth`, `VsopModel`, `VsopFormula`, términos VSOP, `TerseVector`, `Vector`, `Ecliptic`.
- Transformaciones de orientación: `gyration(..., PrecessDirection.From2000)`, precesión/nutación asociada, `earthTilt(adjustedTime)`, `rotateEquatorialToEcliptic(...)`.
- Constantes: `C_AUDAY`, `DAYS_PER_MILLENNIUM`, `DEG2RAD`/`RAD2DEG`, constantes de precesión/nutación ya usadas por `earthTilt`.

Tablas/coefs necesarios: solo el subconjunto VSOP87 de la Tierra. No se necesitan los modelos VSOP de Mercurio, Venus, Marte, Júpiter, Saturno, Urano ni Neptuno para la longitud solar usada por ATHENA.

Portabilidad: el cálculo es puro, con tablas embebidas y `kotlin.math`. El riesgo principal no es compatibilidad con Kotlin/Native sino recortar correctamente el grafo sin romper precesión/nutación ni el sistema de coordenadas “true equinox of date”.

#### `eclipticGeoMoon(time)`

Dependencias mínimas:

- `MoonContext(time).calcMoon()`.
- `MoonContext` completo para el algoritmo lunar: variables internas, `PascalArray2`, `longPeriodic`, `solarN`, `planetary`, `addSolarTerms(this)`, `addSol`, `addn`, `term`, `addTheta`.
- `addSolarTerms(...)`: bloque generado/embebido de términos periódicos lunares solares.
- Tipos mínimos: `Spherical`, `Vector`, `Ecliptic`.
- Conversiones: `Spherical.toVector(time)` si se reutiliza parte del flujo; para `eclipticGeoMoon` concreto se construye el vector manualmente.
- Transformaciones de coordenadas: `earthTilt(time)`, `eclipticToEquatorial(...)`, `nutation(...)`, `rotateEquatorialToEcliptic(...)`, `PrecessDirection.From2000`.
- Constantes: `PI2`, `RAD2DEG`, `EARTH_EQUATORIAL_RADIUS_AU`, `KM_PER_AU`, y las constantes de nutación/oblicuidad compartidas.

Tablas/coefs necesarios: no depende de VSOP completo para la Luna; usa términos lunares embebidos en `MoonContext`/`addSolarTerms` y algunas correcciones planetarias pequeñas hardcodeadas en `planetary()`.

Portabilidad: parece matemática pura y portable a `commonMain`. No requiere recursos externos. El principal riesgo es precisión/regresión si se omiten términos del algoritmo lunar o si se simplifica la conversión de coordenadas true-of-date.

### Partes de Astronomy Engine que NO serían necesarias

Para el alcance Sol/Luna/sidéreo/Ascendente básico no haría falta portar:

- APIs públicas de planetas distintos de Tierra como target indirecto del Sol: Mercurio, Venus, Marte, Júpiter, Saturno, Urano, Neptuno y Plutón.
- Simulador gravitatorio de Plutón y baricentro del sistema solar.
- Cálculo de velocidades (`StateVector`) salvo que alguna helper compartida lo exija y se decida no separarla.
- Rise/set, culminación, refraction, observers topocéntricos, horizontal coordinates y `terra`/`inverseTerra`.
- Eclipses, fases lunares, apsides, elongaciones, equinoccios/solsticios como búsquedas, tránsitos y eventos.
- Magnitudes, elongación visual, libración lunar, orientación de ejes y lunas de Júpiter.
- Estrellas definidas por usuario y constelaciones.
- Formateo ISO/string, parsing, helpers Java/JVM (`@JvmStatic`, `@file:JvmName`) y compatibilidad Java.
- Modelos VSOP de planetas no necesarios para `helioEarthPos`.

### Evaluación de portabilidad KMP

Las rutas inspeccionadas para las cuatro APIs requeridas son mayoritariamente matemáticas puras: `Double`, arrays, enums/data classes simples y `kotlin.math`. No se observaron dependencias de ficheros, red, recursos empaquetados, locale, reflexión JVM ni APIs Android.

Elementos a adaptar para `commonMain`:

- Quitar anotaciones JVM (`@file:JvmName`, `@JvmStatic`) o aislarlas en Android/JVM si se mantiene una API Java.
- Evitar formatos `String.format` en el subconjunto mínimo o moverlos fuera del core común.
- Revisar excepciones públicas (`Exception`, `IllegalArgumentException`) y visibilidad para mantener una API interna estable.
- Mantener comportamiento de `%` con negativos y normalización angular con tests, porque pequeñas diferencias aquí afectan longitudes.
- Mantener las tablas/coeficientes como código Kotlin común, no como recursos externos.

Conclusión: un port mínimo parece razonable técnicamente para Kotlin/Native porque el grafo usado no depende de JVM en su cálculo astronómico central.

### Estimación de complejidad del port mínimo

Estimación aproximada si se copia/adapta código de Astronomy Engine en vez de reescribir fórmulas desde cero:

- 1 archivo de atribución/licencia bajo `docs/licenses/` o equivalente, más encabezados de atribución en cada archivo derivado.
- 3 a 6 archivos Kotlin comunes si se separa por responsabilidad:
  - tiempo/delta-T;
  - tipos matemáticos mínimos (`Vector`, `Spherical`, `Ecliptic`);
  - nutación/oblicuidad/sidéreo;
  - VSOP Tierra/posición solar;
  - algoritmo lunar;
  - adaptador interno para `BasicNatalChartCalculator` futuro.
- Decenas de helpers internos pequeños, pero pocos tipos públicos nuevos si se mantiene encapsulado en `shared/data`.
- Una tabla VSOP de Tierra y el bloque `addSolarTerms` lunar como principales masas de código/coefs.

Riesgos:

- **Precisión**: medio. El objetivo actual de ATHENA valida con tolerancia `0.25°`, mucho más laxa que ±1 arcminute, pero el Ascendente es sensible al tiempo sidéreo y ubicación. Se deben migrar los casos Madrid/New York/Beijing a `commonTest` y añadir comparaciones Android-vs-common durante la transición.
- **Recorte accidental**: medio/alto. `sunPosition` y `eclipticGeoMoon` comparten transformaciones de precesión/nutación; recortar “solo lon” sin conservar el sistema true-of-date puede introducir diferencias difíciles de detectar.
- **Mantenimiento**: medio. Copiar un subconjunto congela la versión y obliga a rastrear fixes upstream manualmente.
- **Licencia**: bajo si se conserva MIT correctamente. La licencia permite copiar/modificar/sublicenciar, pero exige mantener copyright y texto de permiso en copias o porciones sustanciales.
- **Tamaño de código**: bajo/medio. Evitar planetas/eventos reduce mucho el alcance; las tablas de Tierra y Luna siguen siendo el mayor volumen.

### Atribución y licencia si se copia código

Si se copia o deriva código de Astronomy Engine, el siguiente cambio funcional debería:

1. Añadir el texto MIT completo de Astronomy Engine en una ruta versionada del repo, por ejemplo `docs/licenses/astronomy-engine-MIT.md`.
2. Mantener en los archivos derivados un encabezado breve indicando que contienen código adaptado de `cosinekitty/astronomy` tag `v2.1.17`, con copyright original de Don Cross y licencia MIT.
3. Documentar en el propio archivo o en `docs/technical/natal-engine-capabilities.md` qué partes se portaron y cuáles se omitieron.
4. Evitar mezclar código derivado con lógica de negocio para que sea auditable y reemplazable.

### Port manual vs otra librería

Recomendación para el alcance actual: **port manual mínimo y encapsulado**, no carta completa ni dependencia nueva inmediata.

Motivos:

- Las cuatro APIs necesarias forman un subconjunto acotado y matemático.
- La dependencia actual ya está validada en Android con tolerancia funcional de ATHENA.
- Un adaptador común mantiene igual el resultado público (`NatalChartResult`) y reduce divergencia Android/iOS.
- Incorporar otra librería KMP requeriría volver a auditar precisión, licencia, tamaño, targets iOS reales y diferencias de coordenadas.

Condición antes de implementar: comprobar una vez más si existe artefacto KMP/iOS real de Astronomy Engine. Si no existe, portar solo el subconjunto anterior a `shared/data/src/commonMain`, con tests comunes y sin tocar UI, economía, Esencia Natal ni ranking/CSV de ciudades.

### Siguiente paso recomendado

Continuar en esta rama con un spike de implementación acotado para crear un motor común interno limitado a:

1. `Time` mínimo UTC/TT.
2. `siderealTime` con nutación/oblicuidad necesarias.
3. `sunLongitude` basado en VSOP Tierra y transformaciones true-of-date.
4. `moonLongitude` basado en `MoonContext`/`addSolarTerms` y transformaciones true-of-date.
5. Reutilización exacta de la fórmula de Ascendente ya validada en Android.
6. Tests `commonTest` contra los fixtures existentes de Madrid, New York, Beijing y caso sin ubicación.

No se recomienda ampliar este port a planetas, casas, aspectos, MC/IC o carta completa en el mismo paso.
