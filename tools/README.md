# Tooling

## Offline birthplace preset generation

`generate_birthplace_presets.py` generates the offline `birthplaces.csv` resource from local GeoNames data by default. Kotlin generation is explicit and limited to small fallback/legacy catalogues. The script does not download files, and the app does not add network calls or runtime API dependencies for birthplace lookup.

Manual GeoNames download steps:

1. Open `https://download.geonames.org/export/dump/` in a browser.
2. Download `cities15000.zip`.
3. Unzip it locally under `tools/geonames/` (ignored by git) or another local-only folder.
4. Optional: download `countryInfo.txt` from the same directory if generated presets should contain country names instead of only ISO country codes.
5. Optional, recommended for localized search: download `alternateNamesV2.txt` from the same directory to populate CSV `searchNames`.
6. Run the generator from the repository root.

### Reference birthplace catalogue philosophy

ATHENA now recommends a broad city-level catalogue generated directly from GeoNames `cities15000.txt`. The CSV resource is small enough that we should optimize for coverage and user recognition rather than for Kotlin source size or a hand-balanced compact list.

The recommended default mode:

- uses every policy-eligible `cities15000` row;
- excludes only clearly unwanted urban subdivisions;
- does **not** apply country tiers by default;
- does **not** apply `--global-limit` by default;
- does **not** apply per-country limits by default;
- keeps allowlist/exclude files as manual overrides for exceptional cases, not as the base coverage strategy.

This catalogue is still not a statement that omitted municipalities are invalid birthplaces: GeoNames `cities15000` itself is the coverage boundary, and users can choose the nearest city-level reference when their exact locality is below that source threshold.

### Default selection policy

Selection order for the recommended broad mode:

1. Parse all local GeoNames cities.
2. Apply the generator-only urban subdivision eligibility policy before allowlist selection. By default, rows with GeoNames `featureCode=PPLX` (section of populated place) are omitted, as are names that clearly describe neighbourhood/district-level entities: `administrative district`, `arrondissement`, `barrio`, `borough`, `district`, `quarter`, or `ward`. The word `centro` is narrower: it is excluded only for audited big-city-centre patterns currently covered by `Centro Habana` / `Centro Havana` and `Madrid Centro`, so real cities named Centro are not dropped by accident.
3. Apply `--min-population` only when explicitly provided. In the recommended command it is omitted, so all policy-eligible `cities15000` entries remain candidates.
4. Include policy-eligible allowlist entries if an allowlist is explicitly provided. Allowlist entries cannot re-add `PPLX` or barrio/district-like rows.
5. If legacy compact tier/limit flags are omitted, include all eligible candidates.
6. Apply an exclude file only when explicitly provided. Allowlisted cities are not excluded unless the exclude entry is prefixed with `!`.
7. Deduplicate by GeoName ID.
8. Sort the final CSV output by country name, city name, and GeoName ID for deterministic diffs.
9. Write `birthplaces.csv` by default, including a final `searchNames` column (empty unless `--alternate-names` is provided). Kotlin is written only when `--kotlin-output` (or deprecated `--output`) is passed, and only if the selected city count stays under `--kotlin-max-presets` (default `200`).

Legacy tier and global-limit flags remain available for intentionally compact catalogues, but they are no longer the recommended ATHENA regeneration strategy.

### Allowlist and exclude list

`--allowlist-file` is an optional manual override for must-have, policy-eligible real cities. It does not override the urban subdivision policy: `PPLX`, barrio/district-like, arrondissement, ward, quarter, and audited `centro` subdivision rows remain excluded even if listed. Each non-empty line can be a GeoName ID or `City|CountryCode`. The recommended broad command does not need an allowlist as the base coverage mechanism.

`--exclude-file` is an optional manual override for false positives after candidate selection. Each non-empty line can be a GeoName ID or `City|CountryCode`. Prefix an entry with `!` to force exclusion even when it also appears in the allowlist. The recommended broad command relies first on the built-in urban-subdivision policy; use explicit excludes only for audited exceptions.

### Recommended ATHENA command

```bash
python tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --alternate-names tools/geonames/alternateNamesV2.txt
```

When `--alternate-names` is provided, the generator fills `searchNames` from supported-language GeoNames aliases (`es,en,fr,it,pt,ru,de` by default via `--search-name-language`) after filtering to selected GeoName IDs and deduplicating normalized aliases. The generator prints a summary with total selected presets, allowlist requested/matched/missing counts, exclude requested/matched counts, top country counts, and CSV file size. With no tier, per-country, population, or global-limit flags, the default mode selects all eligible `cities15000` rows after urban-subdivision filtering. The recommended flow regenerates only `birthplaces.csv`; `BirthplacePresets.generated.kt` remains a small fallback/legacy source.

### Optional Kotlin fallback output

Use `--kotlin-output <path>` only for a small fallback/legacy catalogue, normally with a small `--global-limit` such as `100`:

```bash
python tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --tier-a-country ES,MX,AR,CO,CL,PE,UY,PY,BO,EC,VE,GT,CR,PA,DO,CU,US,CA,GB,IE,AU,NZ,ZA,PT,BR,FR,BE,CH,LU,DE,AT,IT --tier-b-country RU,CN,JP,KR,IN,ID,TR,UA,PL,RO,NL,SE,NO,FI,DK,GR,CZ,HU,IL,AE,SA,EG,MA,NG,KE --tier-a-limit 20 --tier-b-limit 10 --tier-c-limit 1 --global-limit 100 --allowlist-file tools/geonames/birthplace_allowlist.txt --exclude-file tools/geonames/birthplace_exclude.txt --kotlin-output composeApp/src/commonMain/kotlin/com/agc/bwitch/ui/astrology/birthplace/BirthplacePresets.generated.kt
```

Kotlin output has a guardrail: if the selected city count exceeds `--kotlin-max-presets` (default `200`), generation fails and explains that large catalogues must live in CSV. `--output` remains available as a deprecated alias for `--kotlin-output`, but it is disabled unless explicitly passed.


## Birthplace catalogue audit

`audit_birthplace_catalog.py` is a read-only diagnostic tool for the runtime CSV catalogue. It does not modify `birthplaces.csv`, app runtime code, ranking, loaders, repositories, or astronomical calculation.

Run it from the repository root with standard Python:

```bash
python tools/audit_birthplace_catalog.py
```

The console report measures:

- raw CSV bytes, approximate gzip size, total rows, rows with `searchNames`, and total `searchNames` aliases;
- `countryCode` coverage and the top 30 countries by entry count;
- repeated city names / homonyms, including the top 30 repeated `cityName` values;
- entries that still match the same generator-only urban subdivision policy (`PPLX`, district/barrio/borough/ward/quarter/arrondissement name terms, and the narrow audited `centro` patterns);
- presence of the minimum key-city checklist;
- top 10 approximate search results for diagnostic queries using Python logic aligned with the current birthplace normalizer/ranking rules.

Optionally write the same report as Markdown:

```bash
python tools/audit_birthplace_catalog.py --markdown-output docs/reports/birthplace_catalog_audit.md
```

The suspicious-entry section is read-only: it reports rows that the current generator policy would exclude on the next regeneration, but it does not modify the existing CSV or any runtime code.


## Birthplace alternate names audit

`audit_birthplace_alternate_names.py` is a read-only diagnostic tool for evaluating whether GeoNames `alternateNamesV2.txt` would be useful for a future `searchNames` column or sidecar CSV. It does not modify `birthplaces.csv`, runtime lookup, ranking, loaders, repositories, UI, or astronomical calculation.

The audit is intentionally limited to the languages currently supported by ATHENA (`es`, `en`, `fr`, `it`, `pt`, `ru`, `de`). Users in unsupported app languages can still search with one of those localized names.

Download `alternateNamesV2.txt` manually from GeoNames and place it under `tools/geonames/` (ignored by git), then run:

```bash
python tools/audit_birthplace_alternate_names.py --alternate-names tools/geonames/alternateNamesV2.txt
```

Optional paths:

```bash
python tools/audit_birthplace_alternate_names.py --birthplaces-csv composeApp/src/commonMain/composeResources/files/birthplaces.csv --alternate-names tools/geonames/alternateNamesV2.txt
```

The report includes:

- catalogue cities with at least one useful supported-language alternate name;
- alias totals per supported language;
- mean and approximate p50/p75/p90/p95/p99 aliases per city;
- top cities by alias count;
- estimated UTF-8 and gzip size for a future `searchNames` CSV column or separate `geonameId,searchNames` CSV;
- diagnostic query matches for major cities such as Moscow, London, Rome, New York City, Beijing, Munich, Cologne, Vienna, Lisbon, Brussels, and Amsterdam.

### Legacy options

Legacy compact flags remain available for compatibility when a deliberately small catalogue is needed:

```bash
python3 tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --min-population 50000 --global-limit 5000
```

`--limit` remains available as a deprecated alias for `--global-limit` when `--global-limit` is omitted. `--priority-country`, `--per-priority-country-limit`, and `--per-country-limit` map to the Tier A and Tier C options when the new tier flags are omitted.
