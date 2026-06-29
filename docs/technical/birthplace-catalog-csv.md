# Birthplace catalog CSV

The offline birthplace CSV is generated from a local GeoNames `cities15000.txt` export with the exact same selection rules used for the Kotlin-generated presets. The generator builds one in-memory city list and writes both outputs from that list, so the runtime CSV catalogue and `GeneratedBirthplacePresets` stay aligned.

## Recommended regeneration flow

1. Download `cities15000.zip` from `https://download.geonames.org/export/dump/`.
2. Unzip it to `tools/geonames/cities15000.txt` (the local dump is ignored by git).
3. Download `countryInfo.txt` from the same GeoNames dump directory to `tools/geonames/countryInfo.txt`.
4. From the repository root, regenerate both files together:

```bash
python tools/generate_birthplace_presets.py tools/geonames/cities15000.txt --country-info tools/geonames/countryInfo.txt --tier-a-country ES,MX,AR,CO,CL,PE,UY,PY,BO,EC,VE,GT,CR,PA,DO,CU,US,CA,GB,IE,AU,NZ,ZA,PT,BR,FR,BE,CH,LU,DE,AT,IT --tier-b-country RU,CN,JP,KR,IN,ID,TR,UA,PL,RO,NL,SE,NO,FI,DK,GR,CZ,HU,IL,AE,SA,EG,MA,NG,KE --tier-a-limit 80 --tier-b-limit 40 --tier-c-limit 5 --global-limit 2200 --allowlist-file tools/geonames/birthplace_allowlist.txt --exclude-file tools/geonames/birthplace_exclude.txt --output composeApp/src/commonMain/kotlin/com/agc/bwitch/ui/astrology/birthplace/BirthplacePresets.generated.kt --csv-output composeApp/src/commonMain/composeResources/files/birthplaces.csv
```

Do not regenerate only one of the two outputs. The generator fails if the CSV data row count does not match the number of Kotlin presets selected for `GeneratedBirthplacePresets`.

The CSV schema is stable:

```csv
geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode
```
