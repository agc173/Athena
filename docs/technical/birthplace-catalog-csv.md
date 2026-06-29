# Birthplace catalog CSV

The offline birthplace CSV is generated from a local GeoNames `cities15000.txt` export with the same selection rules used for the Kotlin-generated presets.

Regenerate both outputs with:

```bash
python3 tools/generate_birthplace_presets.py /path/to/cities15000.txt \
  --country-info /path/to/countryInfo.txt \
  --output composeApp/src/commonMain/kotlin/com/agc/bwitch/ui/astrology/birthplace/BirthplacePresets.generated.kt \
  --csv-output composeApp/src/commonMain/composeResources/files/birthplaces.csv
```

The CSV schema is stable:

```csv
geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode
```
