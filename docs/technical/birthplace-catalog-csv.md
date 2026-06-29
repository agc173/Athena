# Birthplace catalog CSV

The offline birthplace CSV is generated from a local GeoNames `cities15000.txt` export. The runtime catalogue is the CSV resource; `BirthplacePresets.generated.kt` is only a small fallback/legacy source and must not be regenerated as a large catalogue.

## Recommended regeneration flow

1. Download `cities15000.zip` from `https://download.geonames.org/export/dump/`.
2. Unzip it to `tools/geonames/cities15000.txt` (the local dump is ignored by git).
3. Download `countryInfo.txt` from the same GeoNames dump directory to `tools/geonames/countryInfo.txt`.
4. From the repository root, regenerate the CSV only:

```bash
python tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --tier-a-country ES,MX,AR,CO,CL,PE,UY,PY,BO,EC,VE,GT,CR,PA,DO,CU,US,CA,GB,IE,AU,NZ,ZA,PT,BR,FR,BE,CH,LU,DE,AT,IT --tier-b-country RU,CN,JP,KR,IN,ID,TR,UA,PL,RO,NL,SE,NO,FI,DK,GR,CZ,HU,IL,AE,SA,EG,MA,NG,KE --tier-a-limit 80 --tier-b-limit 40 --tier-c-limit 5 --global-limit 2200 --allowlist-file tools/geonames/birthplace_allowlist.txt --exclude-file tools/geonames/birthplace_exclude.txt
```

By default the generator writes `composeApp/src/commonMain/composeResources/files/birthplaces.csv` and does **not** write `BirthplacePresets.generated.kt`. This keeps the real runtime catalogue in a resource file and avoids creating a huge Kotlin source that can break compilation.

The CSV schema is stable:

```csv
geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode
```

## Optional Kotlin fallback generation

Generate Kotlin only when a small manual/legacy fallback is intentionally needed:

```bash
python tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --tier-a-country ES,MX,AR,CO,CL,PE,UY,PY,BO,EC,VE,GT,CR,PA,DO,CU,US,CA,GB,IE,AU,NZ,ZA,PT,BR,FR,BE,CH,LU,DE,AT,IT --tier-b-country RU,CN,JP,KR,IN,ID,TR,UA,PL,RO,NL,SE,NO,FI,DK,GR,CZ,HU,IL,AE,SA,EG,MA,NG,KE --tier-a-limit 20 --tier-b-limit 10 --tier-c-limit 1 --global-limit 100 --allowlist-file tools/geonames/birthplace_allowlist.txt --exclude-file tools/geonames/birthplace_exclude.txt --kotlin-output composeApp/src/commonMain/kotlin/com/agc/bwitch/ui/astrology/birthplace/BirthplacePresets.generated.kt
```

The generator refuses to write Kotlin when the selected city count exceeds the safe guardrail (`--kotlin-max-presets`, default `200`). Large catalogues must stay in CSV.
