# Birthplace catalog audit

CSV: `composeApp\src\commonMain\composeResources\files\birthplaces.csv`

## 1. Size
- CSV bytes: 170,221 bytes (166.2 KiB)
- Approx. gzip size: 58,851 bytes (57.5 KiB)
- Total rows: 2,190

## 2. Coverage by countryCode
- Distinct country codes: 39
- Top 30 countries by entries:
  - US: 82
  - AR: 80
  - AU: 80
  - BE: 80
  - BR: 80
  - CA: 80
  - CL: 80
  - CO: 80
  - CU: 80
  - FR: 80
  - DE: 80
  - GT: 80
  - IT: 80
  - MX: 80
  - PE: 80
  - PT: 80
  - ZA: 80
  - GB: 80
  - VE: 80
  - CH: 73
  - EC: 71
  - ES: 71
  - NZ: 47
  - DO: 44
  - CN: 40
  - IE: 40
  - JP: 40
  - RU: 40
  - BO: 39
  - CR: 36

## 3. Duplicates / homonyms
- City names repeated in multiple countries: 55
- Top 30 cityName values by repetition:
  - San Felipe: 4 entries across CL, CR, GT, VE
  - Colón: 3 entries across AR, CU, PA
  - San Juan: 3 entries across AR, CR, PE
  - San Miguel: 3 entries across AR, CR, PE
  - Trinidad: 3 entries across BO, CU, UY
  - Cambridge: 3 entries across CA, GB, NZ
  - Concepción: 3 entries across CL, PY, VE
  - San Antonio: 3 entries across CL, PY, US
  - San José: 3 entries across CR, US
  - San Cristobal: 3 entries across CU, DO, VE
  - Valencia: 3 entries across EC, ES, VE
  - Washington: 3 entries across US
  - Córdoba: 2 entries across AR, ES
  - Mercedes: 2 entries across AR, UY
  - Morón: 2 entries across AR, CU
  - Pilar: 2 entries across AR, PY
  - San Francisco: 2 entries across AR, US
  - San Luis: 2 entries across AR, CU
  - San Martín: 2 entries across AR, PE
  - San Rafael: 2 entries across AR, CR
  - Santa Rosa: 2 entries across AR, EC
  - Villa Mercedes: 2 entries across AR, CL
  - Liverpool: 2 entries across AU, GB
  - Newcastle: 2 entries across AU, ZA
  - Sydney: 2 entries across AU, CA
  - Baden: 2 entries across AT, CH
  - La Paz: 2 entries across BO, UY
  - Sucre: 2 entries across BO, EC
  - Gloucester: 2 entries across CA, GB
  - Hamilton: 2 entries across CA, NZ

## 4. Suspicious entries
- Mirrors the generator-only urban subdivision exclusion policy; this audit remains read-only and changes no runtime data.
- Suspicious rows matched: 0

## 5. Key cities
- OK Madrid/ES: Madrid, ES (Spain; pop 3,255,944; Europe/Madrid; id 3117735)
- OK Barcelona/ES: Barcelona, ES (Spain; pop 1,686,208; Europe/Madrid; id 3128760)
- OK Valencia/ES: Valencia, ES (Spain; pop 824,340; Europe/Madrid; id 2509954)
- OK Seville/ES or Sevilla/ES: Sevilla, ES (Spain; pop 686,741; Europe/Madrid; id 2510911)
- MISSING New York/US
- OK Los Angeles/US: Los Angeles, US (United States; pop 3,820,914; America/Los_Angeles; id 5368361)
- OK Mexico City/MX: Mexico City, MX (Mexico; pop 12,294,193; America/Mexico_City; id 3530597)
- OK Buenos Aires/AR: Buenos Aires, AR (Argentina; pop 2,891,082; America/Argentina/Buenos_Aires; id 3435910)
- OK Bogotá/CO: Bogotá, CO (Colombia; pop 7,674,366; America/Bogota; id 3688689)
- OK São Paulo/BR: São Paulo, BR (Brazil; pop 12,400,232; America/Sao_Paulo; id 3448439)
- OK Rio de Janeiro/BR: Rio de Janeiro, BR (Brazil; pop 6,747,815; America/Sao_Paulo; id 3451190)
- OK London/GB: London, GB (United Kingdom; pop 8,961,989; Europe/London; id 2643743)
- OK Paris/FR: Paris, FR (France; pop 2,138,551; Europe/Paris; id 2988507)
- OK Berlin/DE: Berlin, DE (Germany; pop 3,426,354; Europe/Berlin; id 2950159)
- OK Rome/IT: Rome, IT (Italy; pop 2,318,895; Europe/Rome; id 3169070)
- OK Moscow/RU: Moscow, RU (Russia; pop 10,381,222; Europe/Moscow; id 524901)
- OK Tokyo/JP: Tokyo, JP (Japan; pop 9,733,276; Asia/Tokyo; id 1850147)
- OK Beijing/CN: Beijing, CN (China; pop 18,960,744; Asia/Shanghai; id 1816670)
- OK Shanghai/CN: Shanghai, CN (China; pop 24,874,500; Asia/Shanghai; id 1796236)
- OK Sydney/AU: Sydney, AU (Australia; pop 5,638,830; Australia/Sydney; id 2147714)

## 6. Test queries
Python approximation of the runtime normalization/ranking; report-only.
- `madrid` → 3 shown
  - Madrid, ES (Spain; pop 3,255,944; Europe/Madrid; id 3117735)
  - Madrid, CO (Colombia; pop 135,000; America/Bogota; id 3675707)
  - Las Rozas de Madrid, ES (Spain; pop 95,550; Europe/Madrid; id 3118848)
- `moscow` → 1 shown
  - Moscow, RU (Russia; pop 10,381,222; Europe/Moscow; id 524901)
- `tokyo` → 1 shown
  - Tokyo, JP (Japan; pop 9,733,276; Asia/Tokyo; id 1850147)
- `buenos aires` → 1 shown
  - Buenos Aires, AR (Argentina; pop 2,891,082; America/Argentina/Buenos_Aires; id 3435910)
- `bogota` → 1 shown
  - Bogotá, CO (Colombia; pop 7,674,366; America/Bogota; id 3688689)
- `new york` → 1 shown
  - New York City, US (United States; pop 8,804,190; America/New_York; id 5128581)
- `roma` → 1 shown
  - La Romana, DO (Dominican Republic; pop 208,437; America/Santo_Domingo; id 3500957)
- `moscu` → 0 shown
- `pekin` → 0 shown
- `londres` → 0 shown

## Notes
- This script does not modify the catalogue or runtime code.
- Suspicious-entry detection is intentionally broad and diagnostic only.
