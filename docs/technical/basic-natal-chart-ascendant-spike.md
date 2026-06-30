# Basic natal chart Ascendant spike

## Decision

ATHENA can calculate a **local experimental Ascendant** without Swiss Ephemeris, backend APIs, Firestore, Functions, or LLM calls. The recommended next step is to validate a direct formula against independent astrology calculators before promoting the field into `NatalChartResult`.

## Definition

The Ascendant is the ecliptic longitude of the point where the ecliptic intersects the observer's **local eastern horizon** at the birth moment. It is not a body position; it is a horizon/ecliptic intersection. It therefore depends on the birth instant and birthplace, not only on UTC date/time.

## Required inputs

- UTC birth date/time, represented in the current MVP by `BirthDateTimeUtc`.
- Geographic latitude in decimal degrees, positive north.
- Geographic longitude in decimal degrees, positive east. Western longitudes are negative.
- An obliquity value for the ecliptic/equator angle.

## Astronomy Engine APIs actually available/relevant

The installed project dependency is declared only for Android data code as `com.github.cosinekitty:astronomy:v2.1.17` in `shared/data`.

The currently used Android production calculator already imports and compiles against these Kotlin/JVM names:

- `io.github.cosinekitty.astronomy.Time`
- `io.github.cosinekitty.astronomy.sunPosition`
- `io.github.cosinekitty.astronomy.eclipticGeoMoon`

The upstream Kotlin documentation for the same package exposes the local-coordinate APIs needed for an implementation path:

- `siderealTime(time: Time): Double`, returning Greenwich Apparent Sidereal Time in sidereal hours.
- `Observer(latitude, longitude, height)`.
- `rotationEqdHor(time, observer)` and `rotationHorEqd(time, observer)`.
- `rotationEqdEct(time)` / `rotationEctEqd(time)` for equator-of-date and true-ecliptic-of-date conversions.
- `rotationHorEcl(time, observer)` / `rotationEclHor(time, observer)` for horizontal and J2000 ecliptic conversions.
- `Vector`, `Spherical`, `vectorFromSphere`, `sphereFromVector`, and `rotateVector` for coordinate-system experiments.

Gradle could not resolve the repository's Kotlin serialization plugin version in this environment, so local class inspection through Gradle dependency resolution was blocked. The function names above are taken from the project's existing source imports plus the Astronomy Engine Kotlin package documentation, not invented names.

## Chosen approach for the spike

Use approach **A: direct mathematical formula**, with Astronomy Engine supplying sidereal time.

1. Convert `BirthDateTimeUtc` to `Time`.
2. Read Greenwich Apparent Sidereal Time:
   `gastHours = siderealTime(time)`.
3. Convert to Local Sidereal Time, using east-positive longitude:
   `lstDegrees = normalizeDegrees((gastHours + longitudeDegrees / 15.0) * 15.0)`.
4. Use latitude `φ`, sidereal angle `θ`, and obliquity `ε`:

```text
ascendantLongitude = atan2(
    -cos(θ),
    sin(θ) * cos(ε) + tan(φ) * sin(ε)
)
```

5. Normalize the result into `[0, 360)` and map it with `longitudeToZodiacSign()`.

This is intentionally independent of the Sun/Moon code path and does not change `NatalChartResult`.

## Why not use rotation APIs first?

A coordinate-system implementation is attractive because Astronomy Engine can rotate between horizon, equatorial, and ecliptic frames. However, the direct formula is smaller, easier to audit, and uses only one Astronomy Engine value (`siderealTime`) plus explicit geometry. Rotation APIs are still useful as a later cross-check:

- Build a unit horizontal vector for due east on the horizon.
- Rotate HOR → EQD → ECT.
- Convert the resulting vector to spherical ecliptic longitude.
- Compare against the formula within a small tolerance.

That cross-check should happen before production promotion if we want stronger confidence.

## Known limitations

- The spike uses a fixed mean obliquity constant (`23.4392911°`) instead of deriving true obliquity/nutation from Astronomy Engine. This should be replaced or cross-checked with the rotation API path for production.
- Ascendant is very sensitive to birth time and longitude. A few minutes of clock or timezone error can change the degree noticeably.
- Near polar latitudes, the ecliptic/horizon geometry can be degenerate or unintuitive; validation should include guardrails for extreme latitudes.
- Online astrology calculators may use house systems, refraction assumptions, topocentric details, or ephemerides that are not identical to this minimal astronomical formula.
- This spike validates signs/degrees only as a local regression harness; it is not yet a full acceptance suite.

## Reference/regression cases

These cases are defined for deterministic regression of the formula. They still need manual comparison against 2-3 independent trusted astrology calculators before productizing.

| Case | UTC birth date/time | Latitude | Longitude | Spike expected longitude | Expected sign |
|---|---:|---:|---:|---:|---|
| Greenwich J2000 noon | 2000-01-01 12:00:00 UTC | 51.4769 | 0.0000 | ~204.3° | Libra |
| New York sample | 1990-06-15 18:30:00 UTC | 40.7128 | -74.0060 | ~13.7° | Aries |
| Sydney equinox sample | 2024-03-20 03:06:00 UTC | -33.8688 | 151.2093 | ~269.0° | Sagittarius |

Manual validation to do before production:

1. Enter each UTC time directly in an online calculator that supports UTC/timezone override.
2. Use the exact decimal coordinates above, not city defaults, if the tool allows it.
3. Record the calculator name, URL, date accessed, house system/settings, and returned Ascendant degree/sign.
4. Accept production only if independent references agree within an MVP tolerance to be defined, likely ≤1° for ordinary latitudes.

## ATHENA v1 acceptability

This is acceptable as an **ATHENA v1 candidate** if validation confirms agreement within the chosen tolerance for ordinary latitudes and modern dates. It is not yet acceptable to expose in UI or persist because:

- independent external validation has not been recorded;
- the fixed obliquity should be replaced or cross-checked;
- no guardrails exist yet for invalid/extreme birthplace coordinates.

## Recommended next implementation step

Promote only after validation by adding a production `AscendantCalculator` in `shared/data/src/androidMain`, with tests in `androidUnitTest`, then extend the domain model in a separate feature commit if the API and UI copy are ready.
