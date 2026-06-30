# Birthplace catalog audit

CSV: `composeApp\src\commonMain\composeResources\files\birthplaces.csv`

## 1. Size
- CSV bytes: 2,693,773 bytes (2630.6 KiB)
- Approx. gzip size: 912,439 bytes (891.1 KiB)
- Total rows: 31,555
- Rows with searchNames: 14,309
- Total searchNames aliases: 21,405
- Broad-catalogue row expectation: OK (expected at least 10,000 rows when regenerated from full cities15000 without compact limits)

## 2. Coverage by countryCode
- Distinct country codes: 244
- Broad-catalogue country expectation: OK (expected at least 100 country codes; compact 39-country catalogues are no longer the target)
- Top 30 countries by entries:
  - IN: 3,737
  - US: 3,121
  - BR: 2,248
  - CN: 2,083
  - JP: 1,215
  - RU: 986
  - DE: 960
  - GB: 802
  - FR: 657
  - MX: 625
  - IT: 596
  - ES: 544
  - PH: 520
  - ID: 443
  - IR: 418
  - TR: 415
  - PK: 363
  - MY: 350
  - PL: 344
  - CO: 317
  - CA: 296
  - ZA: 295
  - DZ: 293
  - TH: 293
  - VN: 290
  - UA: 277
  - NG: 269
  - AR: 247
  - ET: 233
  - EG: 232

## 3. Duplicates / homonyms
- City names repeated in multiple countries: 606
- Top 30 cityName values by repetition:
  - Victoria: 9 entries across AR, CA, CL, HK, HN, PH, SC, US
  - La Unión: 8 entries across AR, CL, CO, ES, PE, SV
  - Richmond: 8 entries across CA, GB, NZ, US, ZA
  - Springfield: 8 entries across US
  - San Pedro: 7 entries across AR, BZ, CR, MX, PH
  - Santa Rosa: 7 entries across AR, BR, CO, EC, PE, PH, US
  - Santa Cruz: 7 entries across AW, BR, CL, PH, US
  - Santa Bárbara: 7 entries across BR, GT, HN, PH, US, VE
  - San Fernando: 7 entries across ES, MX, PE, PH, TT
  - La Paz: 6 entries across AR, BO, HN, MX, PH, UY
  - San Juan: 6 entries across AR, CR, PE, PH, PR, US
  - San Miguel: 6 entries across AR, CR, PE, PH, SV
  - Aurora: 6 entries across BR, CA, PH, US
  - Candelária: 6 entries across BR, CO, ES, PH, PR
  - Burlington: 6 entries across CA, US
  - San Antonio: 6 entries across CL, PE, PH, PY, US
  - San Felipe: 6 entries across CL, CR, GT, MX, VE
  - San Marcos: 6 entries across CO, GT, NI, SV, US
  - San José: 6 entries across CR, PH, US
  - Santa Ana: 6 entries across PE, PH, SV, US, VE
  - Middletown: 6 entries across US
  - Lincoln: 5 entries across AR, GB, US
  - Mercedes: 5 entries across AR, PH, US, UY
  - San Francisco: 5 entries across AR, PH, SV, US
  - San Isidro: 5 entries across AR, CR, ES, MX, PE
  - San Lorenzo: 5 entries across AR, CO, HN, PY, US
  - San Luis: 5 entries across AR, CU, HN, PH, US
  - San Martín: 5 entries across AR, CO, PE, SV
  - San Rafael: 5 entries across AR, CR, MX, US, VE
  - San Vicente: 5 entries across AR, CO, CR, PH, SV

## 4. Suspicious entries
- Mirrors the generator-only urban subdivision exclusion policy; this audit remains read-only and changes no runtime data.
- Suspicious rows matched: 0

## 5. Key cities
- OK Madrid/ES: Madrid, ES (Spain; pop 3,255,944; Europe/Madrid; id 3117735)
- OK Barcelona/ES: Barcelona, ES (Spain; pop 1,686,208; Europe/Madrid; id 3128760)
- OK Valencia/ES: Valencia, ES (Spain; pop 824,340; Europe/Madrid; id 2509954)
- OK Seville/ES or Sevilla/ES: Sevilla, ES (Spain; pop 686,741; Europe/Madrid; id 2510911)
- OK New York/US or New York City/US: New York City, US (United States; pop 8,804,190; America/New_York; id 5128581)
- OK Los Angeles/US: Los Angeles, US (United States; pop 3,820,914; America/Los_Angeles; id 5368361)
- OK Mexico City/MX: Mexico City, MX (Mexico; pop 12,294,193; America/Mexico_City; id 3530597)
- OK Buenos Aires/AR: Buenos Aires, AR (Argentina; pop 2,891,082; America/Argentina/Buenos_Aires; id 3435910)
- OK Bogotá/CO: Bogotá, CO (Colombia; pop 7,674,366; America/Bogota; id 3688689)
- OK São Paulo/BR: São Paulo, BR (Brazil; pop 12,400,232; America/Sao_Paulo; id 3448439)
- OK Rio de Janeiro/BR: Rio de Janeiro, BR (Brazil; pop 6,747,815; America/Sao_Paulo; id 3451190)
- OK London/GB: London, GB (United Kingdom; pop 8,961,989; Europe/London; id 2643743)
- OK Paris/FR: Paris, FR (France; pop 2,138,551; Europe/Paris; id 2988507)
- OK Berlin/DE: Berlin, DE (Germany; pop 3,426,354; Europe/Berlin; id 2950159)
- OK Amsterdam/NL: Amsterdam, NL (The Netherlands; pop 741,636; Europe/Amsterdam; id 2759794)
- OK Rome/IT: Rome, IT (Italy; pop 2,318,895; Europe/Rome; id 3169070)
- OK Moscow/RU: Moscow, RU (Russia; pop 10,381,222; Europe/Moscow; id 524901)
- OK Tokyo/JP: Tokyo, JP (Japan; pop 9,733,276; Asia/Tokyo; id 1850147)
- OK Beijing/CN: Beijing, CN (China; pop 18,960,744; Asia/Shanghai; id 1816670)
- OK Shanghai/CN: Shanghai, CN (China; pop 24,874,500; Asia/Shanghai; id 1796236)
- OK Sydney/AU: Sydney, AU (Australia; pop 5,638,830; Australia/Sydney; id 2147714)

## 6. Test queries
Python approximation of the runtime normalization/ranking; report-only.
- `madrid` → 5 shown
  - Madrid, ES (Spain; pop 3,255,944; Europe/Madrid; id 3117735)
  - Madrid, CO (Colombia; pop 135,000; America/Bogota; id 3675707)
  - Humanes de Madrid, ES (Spain; pop 18,098; Europe/Madrid; id 3120501)
  - Rivas-Vaciamadrid, ES (Spain; pop 68,405; Europe/Madrid; id 3107112)
  - Las Rozas de Madrid, ES (Spain; pop 95,550; Europe/Madrid; id 3118848)
- `moscow` → 2 shown
  - Moscow, RU (Russia; pop 10,381,222; Europe/Moscow; id 524901)
  - Moscow, US (United States; pop 25,060; America/Los_Angeles; id 5601538)
- `tokyo` → 2 shown
  - Tokyo, JP (Japan; pop 9,733,276; Asia/Tokyo; id 1850147)
  - Nishi-Tokyo-shi, JP (Japan; pop 207,388; Asia/Tokyo; id 1850692)
- `buenos aires` → 1 shown
  - Buenos Aires, AR (Argentina; pop 2,891,082; America/Argentina/Buenos_Aires; id 3435910)
- `bogota` → 1 shown
  - Bogotá, CO (Colombia; pop 7,674,366; America/Bogota; id 3688689)
- `new york` → 4 shown
  - New York City, US (United States; pop 8,804,190; America/New_York; id 5128581)
  - West New York, US (United States; pop 53,366; America/New_York; id 5106292)
  - Jakarta, ID (Indonesia; pop 8,540,121; Asia/Jakarta; id 1642911)
  - Amsterdam, US (United States; pop 18,008; America/New_York; id 5107152)
- `roma` → 10 shown
  - Rome, IT (Italy; pop 2,318,895; Europe/Rome; id 3169070)
  - Rome, US (United States; pop 36,323; America/New_York; id 4219762)
  - Rome, US (United States; pop 32,573; America/New_York; id 5134295)
  - Roman, RO (Romania; pop 67,819; Europe/Bucharest; id 668732)
  - Romainville, FR (France; pop 24,772; Europe/Paris; id 2983026)
  - Romano Banco, IT (Italy; pop 26,163; Europe/Rome; id 8948705)
  - Romans-sur-Isère, FR (France; pop 35,002; Europe/Paris; id 2983011)
  - Romano di Lombardia, IT (Italy; pop 17,549; Europe/Rome; id 3169056)
  - Romeoville, US (United States; pop 39,719; America/Chicago; id 4908068)
  - Nedroma, DZ (Algeria; pop 27,742; Africa/Algiers; id 2486284)
- `moscu` → 2 shown
  - Moscow, RU (Russia; pop 10,381,222; Europe/Moscow; id 524901)
  - Moscow, US (United States; pop 25,060; America/Los_Angeles; id 5601538)
- `moscú` → 2 shown
  - Moscow, RU (Russia; pop 10,381,222; Europe/Moscow; id 524901)
  - Moscow, US (United States; pop 25,060; America/Los_Angeles; id 5601538)
- `pekin` → 3 shown
  - Pekin, US (United States; pop 33,223; America/Chicago; id 4905599)
  - Beijing, CN (China; pop 18,960,744; Asia/Shanghai; id 1816670)
  - Peqin, AL (Albania; pop 16,580; Europe/Tirane; id 3184497)
- `pekín` → 3 shown
  - Pekin, US (United States; pop 33,223; America/Chicago; id 4905599)
  - Beijing, CN (China; pop 18,960,744; Asia/Shanghai; id 1816670)
  - Peqin, AL (Albania; pop 16,580; Europe/Tirane; id 3184497)
- `londres` → 10 shown
  - London, GB (United Kingdom; pop 8,961,989; Europe/London; id 2643743)
  - London, CA (Canada; pop 422,324; America/Toronto; id 6058560)
  - New London, US (United States; pop 27,179; America/New_York; id 4839416)
  - East London, ZA (South Africa; pop 478,676; Africa/Johannesburg; id 1006984)
  - Derry, GB (United Kingdom; pop 83,652; Europe/London; id 2643736)
  - Barnet, GB (United Kingdom; pop 30,000; Europe/London; id 2656295)
  - Bexley, GB (United Kingdom; pop 228,000; Europe/London; id 2655775)
  - Sutton, GB (United Kingdom; pop 187,600; Europe/London; id 2636503)
  - Croydon, GB (United Kingdom; pop 173,314; Europe/London; id 2651817)
  - Hounslow, GB (United Kingdom; pop 66,292; Europe/London; id 2646517)
- `nueva york` → 4 shown
  - New York City, US (United States; pop 8,804,190; America/New_York; id 5128581)
  - West New York, US (United States; pop 53,366; America/New_York; id 5106292)
  - Jakarta, ID (Indonesia; pop 8,540,121; Asia/Jakarta; id 1642911)
  - Amsterdam, US (United States; pop 18,008; America/New_York; id 5107152)

## Notes
- This script does not modify the catalogue or runtime code.
- Suspicious-entry detection is intentionally broad and diagnostic only.
