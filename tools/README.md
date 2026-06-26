# Tooling

## Offline birthplace preset generation

`generate_birthplace_presets.py` generates `BirthplacePresets.generated.kt` from local GeoNames data. The script does not download files, and the app does not add network calls or runtime API dependencies for birthplace lookup.

Manual GeoNames download steps:

1. Open `https://download.geonames.org/export/dump/` in a browser.
2. Download `cities15000.zip`.
3. Unzip it locally under `tools/geonames/` (ignored by git) or another local-only folder.
4. Optional: download `countryInfo.txt` from the same directory if generated presets should contain country names instead of only ISO country codes.
5. Run the generator from the repository root.

### Reference birthplace catalogue philosophy

ATHENA keeps a compact reference catalogue rather than every municipality. The catalogue should provide meaningful birthplaces that are recognizable reference points for natal chart users while keeping generated Kotlin small enough to compile comfortably.

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
10. Sort the final Kotlin output by country name, city name, and GeoName ID for deterministic diffs.

### Allowlist and exclude list

`--allowlist-file` keeps must-have cities in the candidate set. Each non-empty line can be a GeoName ID or `City|CountryCode`. `tools/geonames/birthplace_allowlist.txt` contains globally important birthplaces that should not be missed, such as Madrid, Tokyo, New York, Sydney, Beijing, Shanghai, São Paulo, Buenos Aires, Bogotá, and others.

`--exclude-file` removes compact-catalogue false positives after candidate selection. Each non-empty line can be a GeoName ID or `City|CountryCode`. Prefix an entry with `!` to force exclusion even when it also appears in the allowlist. `tools/geonames/birthplace_exclude.txt` starts with Spanish satellite municipalities such as Getafe, Leganés, Barakaldo, and nearby examples that should not appear as independent reference birthplaces in the compact catalogue.

### Recommended ATHENA command

```bash
python tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --tier-a-country ES,MX,AR,CO,CL,PE,UY,PY,BO,EC,VE,GT,CR,PA,DO,CU,US,CA,GB,IE,AU,NZ,ZA,PT,BR,FR,BE,CH,LU,DE,AT,IT --tier-b-country RU,CN,JP,KR,IN,ID,TR,UA,PL,RO,NL,SE,NO,FI,DK,GR,CZ,HU,IL,AE,SA,EG,MA,NG,KE --tier-a-limit 80 --tier-b-limit 40 --tier-c-limit 5 --global-limit 2200 --allowlist-file tools/geonames/birthplace_allowlist.txt --exclude-file tools/geonames/birthplace_exclude.txt
```

The generator prints a summary with total generated presets, allowlist requested/matched/missing counts, exclude requested/matched counts, top country counts, and generated file size when available.

### Legacy options

Legacy weighted flags remain available for compatibility:

```bash
python3 tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --min-population 50000 --global-limit 5000
```

`--limit` remains available as a deprecated alias for `--global-limit` when `--global-limit` is omitted. `--priority-country`, `--per-priority-country-limit`, and `--per-country-limit` map to the Tier A and Tier C options when the new tier flags are omitted.
