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

Example with a local GeoNames folder, population floor, and generated-list cap:

```bash
python3 tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --min-population 50000 --limit 5000
```

`--min-population` filters out GeoNames rows below the provided population value. `--limit` keeps the top N remaining cities by population before the final deterministic country/name/id sort.
