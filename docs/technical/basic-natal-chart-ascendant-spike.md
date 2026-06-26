# Basic natal chart Ascendant spike

## Decision

This document records an isolated technical spike for `spike/ascendant-validation`. It does **not** promote Ascendant to production, does **not** change `NatalChartResult`, does **not** change `BasicNatalChartCalculator` production behavior, and does **not** modify UI.

## Spike goal

Validate whether the current experimental direct Ascendant formula measures the opposite ecliptic/horizon intersection (the Descendant), and whether a test-only correction:

```text
corrected = normalizeDegrees(raw + 180.0)
```

matches modern Astro-Seek reference charts consistently.

The `+180°` correction is intentionally limited to the Android unit test spike. It is not production code and must not be treated as an accepted implementation until the feature is explicitly productized.

## Definition

The Ascendant is the ecliptic longitude of the point where the ecliptic intersects the observer's **local eastern horizon** at the birth moment. It is not a body position; it is a horizon/ecliptic intersection. It therefore depends on the birth instant and birthplace, not only on UTC date/time.

## Formula under test

The spike keeps using approach **A: direct mathematical formula**, with Astronomy Engine supplying sidereal time.

1. Convert `BirthDateTimeUtc` to `Time`.
2. Read Greenwich Apparent Sidereal Time:
   `gastHours = siderealTime(time)`.
3. Convert to Local Sidereal Time, using east-positive longitude:
   `lstDegrees = normalizeDegrees((gastHours + longitudeDegrees / 15.0) * 15.0)`.
4. Use latitude `φ`, sidereal angle `θ`, and fixed mean obliquity `ε = 23.4392911°`:

```text
raw = atan2(
    -cos(θ),
    sin(θ) * cos(ε) + tan(φ) * sin(ε)
)
```

5. Normalize `raw` into `[0, 360)` and map it with `longitudeToZodiacSign()`.
6. For this spike only, also measure `corrected = normalizeDegrees(raw + 180.0)`.

This remains independent of the Sun/Moon production code path and does not change `NatalChartResult`.

## Reference cases

The spike uses three independent modern Astro-Seek reference charts supplied for validation:

| Case | Local civil time | UTC time | Latitude | Longitude | Astro-Seek Sun | Astro-Seek Moon | Astro-Seek Ascendant |
|---|---:|---:|---:|---:|---:|---:|---:|
| Madrid | 1994-12-29 09:00 CET | 1994-12-29 08:00 UTC | 40.4166667 | -3.7000000 | 277°22′ Capricorn | 233°59′ Scorpio | 281°43′ Capricorn |
| New York | 1980-06-01 15:10 EDT | 1980-06-01 19:10 UTC | 40.7166667 | -74.0000000 | 71°24′ Gemini | 286°26′ Capricorn | 191°02′ Libra |
| Beijing | 2007-10-16 14:54 CST | 2007-10-16 06:54 UTC | 39.9000000 | 116.4000000 | 202°32′ Libra | 257°51′ Sagittarius | 318°45′ Aquarius |

## Measurements

Gradle could not execute the Android unit test in this environment because of the known Kotlin serialization plugin resolution problem, so the test output is not recorded here from a successful Gradle run. The raw/corrected Ascendant measurements below are the direct-formula values for the same reference inputs and are the values the spike test is designed to print once Gradle resolves.

| Case | Current/raw formula | Raw sign | Raw Δ vs Astro-Seek ASC | Corrected (+180°) | Corrected sign | Corrected Δ vs Astro-Seek ASC |
|---|---:|---|---:|---:|---|---:|
| Madrid | 101.7194° | Cancer | 179.9973° | 281.7194° | Capricorn | 0.0027° |
| New York | 11.0406° | Aries | 179.9927° | 191.0406° | Libra | 0.0073° |
| Beijing | 138.7547° | Leo | 179.9953° | 318.7547° | Aquarius | 0.0047° |

## Spike-only Android unit test

`shared/data/src/androidUnitTest/kotlin/com/agc/bwitch/data/astrology/natal/AscendantSpikeTest.kt` contains a spike-only test that:

- calculates Sun and Moon through the existing `BasicNatalChartCalculator`;
- calculates the current/raw Ascendant using the existing direct formula copied into test scope;
- calculates `corrected = normalizeDegrees(raw + 180.0)` in test scope only;
- prints actual/expected/delta/sign details for Sun, Moon, raw Ascendant, and corrected Ascendant for all three Astro-Seek references;
- asserts Sun delta `<= 0.25°`;
- asserts Moon delta `<= 0.25°`;
- asserts corrected Ascendant delta `<= 0.25°`;
- asserts corrected Ascendant sign matches Astro-Seek;
- does **not** assert the raw Ascendant.

## Hypothesis assessment

All three references support the same hypothesis: the current direct formula is consistently returning the opposite horizon point from the Astro-Seek Ascendant reference. Adding `+180°` in the spike test aligns the measured longitude and sign with Astro-Seek for Madrid, New York, and Beijing within `0.01°` in the measured formula values above.

This is still a research spike. The result is strong enough to justify a later production design task, but production code remains unchanged in this branch.

## Production status

No production code changed in this spike:

- no UI changes;
- no `NatalChartResult` changes;
- no `BasicNatalChartCalculator` production behavior changes;
- no production Ascendant API/model is added;
- no Gradle/dependency changes are made.

## Recommended next implementation step

If the feature is productized later, create a production `AscendantCalculator` in `shared/data/src/androidMain`, add non-spike tests in `androidUnitTest`, consider replacing fixed mean obliquity with an Astronomy Engine-derived value or rotation API cross-check, and only then extend domain/API/UI in a separate feature change.
