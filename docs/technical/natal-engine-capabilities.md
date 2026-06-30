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
