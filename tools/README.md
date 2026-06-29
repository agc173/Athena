# Tooling

## Offline birthplace preset generation

`generate_birthplace_presets.py` generates the offline `birthplaces.csv` resource from local GeoNames data by default. Kotlin generation is explicit and limited to small fallback/legacy catalogues. The script does not download files, and the app does not add network calls or runtime API dependencies for birthplace lookup.

Manual GeoNames download steps:

1. Open `https://download.geonames.org/export/dump/` in a browser.
2. Download `cities15000.zip`.
3. Unzip it locally under `tools/geonames/` (ignored by git) or another local-only folder.
4. Optional: download `countryInfo.txt` from the same directory if generated presets should contain country names instead of only ISO country codes.
5. Run the generator from the repository root.

### Reference birthplace catalogue philosophy

ATHENA keeps a compact reference catalogue rather than every municipality. The catalogue should provide meaningful birthplaces that are recognizable reference points for natal chart users while keeping the runtime catalogue in CSV instead of generated Kotlin.

This means the generator intentionally favors:

- supported-language markets and countries with likely ATHENA users;
- globally important countries and large metropolitan anchors;
- provincial/state capitals and clear identity or tourism exceptions;
- deterministic, curated output that can be reviewed in code.

The catalogue is not a statement that omitted municipalities are invalid birthplaces. For compactness, satellite towns and less useful municipalities can be excluded from the selectable reference list, and users can manually choose a nearby major reference city.

### Tier strategy

The recommended generator mode uses configurable country tiers:

- **Tier A**: supported-language markets and priority countries. These receive the broadest per-country coverage.
- **Tier B**: globally important or large countries that should receive stronger coverage than the default baseline.
- **Tier C**: all remaining countries. These receive a small baseline so the catalogue remains global without becoming a full municipality database.

Selection order:

1. Parse all local GeoNames cities.
2. Apply `--min-population` to normal candidates when provided. Allowlisted cities are still eligible below the minimum.
3. Include allowlist entries first.
4. Add up to `--tier-a-limit` largest cities for each Tier A country.
5. Add up to `--tier-b-limit` largest cities for each Tier B country.
6. Add up to `--tier-c-limit` largest cities for each remaining country.
7. Fill remaining slots with the largest global cities until `--global-limit` is reached.
8. Apply the exclude file after candidate selection. Allowlisted cities are not excluded unless the exclude entry is prefixed with `!`.
9. Deduplicate by GeoName ID.
10. Sort the final CSV output by country name, city name, and GeoName ID for deterministic diffs.
11. Write `birthplaces.csv` by default. Kotlin is written only when `--kotlin-output` (or deprecated `--output`) is passed, and only if the selected city count stays under `--kotlin-max-presets` (default `200`).

### Allowlist and exclude list

`--allowlist-file` keeps must-have cities in the candidate set. Each non-empty line can be a GeoName ID or `City|CountryCode`. `tools/geonames/birthplace_allowlist.txt` contains globally important birthplaces that should not be missed, such as Madrid, Tokyo, New York, Sydney, Beijing, Shanghai, São Paulo, Buenos Aires, Bogotá, and others.

`--exclude-file` removes compact-catalogue false positives after candidate selection. Each non-empty line can be a GeoName ID or `City|CountryCode`. Prefix an entry with `!` to force exclusion even when it also appears in the allowlist. `tools/geonames/birthplace_exclude.txt` starts with Spanish satellite municipalities such as Getafe, Leganés, Barakaldo, and nearby examples that should not appear as independent reference birthplaces in the compact catalogue.

### Recommended ATHENA command

```bash
python tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --tier-a-country ES,MX,AR,CO,CL,PE,UY,PY,BO,EC,VE,GT,CR,PA,DO,CU,US,CA,GB,IE,AU,NZ,ZA,PT,BR,FR,BE,CH,LU,DE,AT,IT --tier-b-country RU,CN,JP,KR,IN,ID,TR,UA,PL,RO,NL,SE,NO,FI,DK,GR,CZ,HU,IL,AE,SA,EG,MA,NG,KE --tier-a-limit 80 --tier-b-limit 40 --tier-c-limit 5 --global-limit 2200 --allowlist-file tools/geonames/birthplace_allowlist.txt --exclude-file tools/geonames/birthplace_exclude.txt
```

The generator prints a summary with total selected presets, allowlist requested/matched/missing counts, exclude requested/matched counts, top country counts, and CSV file size. The recommended flow regenerates only `birthplaces.csv`; `BirthplacePresets.generated.kt` remains a small fallback/legacy source.

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

- raw CSV bytes, approximate gzip size, and total rows;
- `countryCode` coverage and the top 30 countries by entry count;
- repeated city names / homonyms, including the top 30 repeated `cityName` values;
- suspicious entries that look like neighbourhoods, districts, boroughs, wards, barrios, quarters, arrondissements, municipios, comunas, or other urban subdivisions, using broad report-only heuristics;
- presence of the minimum key-city checklist;
- top 10 approximate search results for diagnostic queries using Python logic aligned with the current birthplace normalizer/ranking rules.

Optionally write the same report as Markdown:

```bash
python tools/audit_birthplace_catalog.py --markdown-output docs/reports/birthplace_catalog_audit.md
```

The suspicious-entry section is intentionally diagnostic only; it must not be interpreted as an automatic exclusion policy.

### Legacy options

Legacy weighted flags remain available for compatibility:

```bash
python3 tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --min-population 50000 --global-limit 5000
```

`--limit` remains available as a deprecated alias for `--global-limit` when `--global-limit` is omitted. `--priority-country`, `--per-priority-country-limit`, and `--per-country-limit` map to the Tier A and Tier C options when the new tier flags are omitted.
