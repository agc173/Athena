# Tooling

## Offline birthplace preset generation

`generate_birthplace_presets.py` generates `BirthplacePresets.generated.kt` from local GeoNames data. The script does not download files, and the app does not add network calls or runtime API dependencies for birthplace lookup.

Manual GeoNames download steps:

1. Open `https://download.geonames.org/export/dump/` in a browser.
2. Download `cities15000.zip`.
3. Unzip it locally under `tools/geonames/` (ignored by git) or another local-only folder.
4. Optional: download `countryInfo.txt` from the same directory if generated presets should contain country names instead of only ISO country codes.
5. Run the generator from the repository root:

```bash
python3 tools/generate_birthplace_presets.py /path/to/cities15000.txt --country-info /path/to/countryInfo.txt
```

If `--country-info` is omitted, the generated `countryName` falls back to the ISO country code from `cities15000.txt`. The committed hand-written list remains the runtime fallback whenever the generated list is empty.

ATHENA recommended command for weighted coverage of supported languages and key markets:

```bash
python tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --priority-country ES,PT,BR,IT,FR,DE,AT,CH,GB,IE,US,CA,AU,MX,AR,CL,CO,PE --per-priority-country-limit 120 --per-country-limit 10 --global-limit 3000 --allowlist-file tools/geonames/birthplace_allowlist.txt
```

Weighted selection parses every city that passes `--min-population` (when provided), includes the largest cities per priority country, includes a smaller per-country baseline for other countries, then fills any remaining slots with the largest global cities until `--global-limit` is reached. Cities are deduplicated by GeoName ID and the Kotlin output is finally sorted by country name, city name, and GeoName ID for deterministic diffs.

`--allowlist-file` keeps important manual entries in the candidate set. Each non-empty line can be either a GeoName ID or `City|CountryCode`; `tools/geonames/birthplace_allowlist.txt` includes Málaga (`Málaga|ES`) as the initial example.

Legacy global-only cap example:

```bash
python3 tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --min-population 50000 --global-limit 5000
```

`--min-population` filters out GeoNames rows below the provided population value. `--limit` remains available as a deprecated alias for `--global-limit` when `--global-limit` is omitted.
