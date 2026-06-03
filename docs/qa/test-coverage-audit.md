# Test Coverage Audit — BWitch / ATHENA

Fecha: 2026-06-03

Rama auditada: `feature/test-coverage-audit` solicitada; el checkout local reportó `## work` al iniciar la auditoría.

Scope: auditoría documental previa a Google Play Closed Testing. No se añadieron tests, frameworks, lógica productiva, deploys, merges ni cambios de configuración.

---

## Actualización — 2026-06-03 — `feature/functions-selftests-fix`

Se corrigieron los 3 selftests rojos de Functions detectados durante esta auditoría sin tocar lógica productiva de economía:

- `functions/src/economy/economyResolvers.selftest.ts`: la expectativa de límite diario de `TAROT_1` estaba obsoleta; el catálogo vigente permite `moonExtraDailyMax: 3`, por lo que el caso rojo usaba `tarot1MoonUsed: 2` y aún debía resolver `MOON`. El test ahora valida rechazo con `tarot1MoonUsed: 3`.
- `functions/src/economy/runtimeConfig.selftest.ts`: las expectativas no incluían los flags ya existentes `synastryEconomyV2Enabled` y `pendulumEconomyV2Enabled`, ambos con fallback seguro `false`.
- `functions/package.json`: se añadieron scripts estables `test:selftests` y `coverage:selftests` para ejecutar los selftests y coverage diagnóstico de Functions.

Validación posterior:

- `npm run lint`: pass.
- `npm run build`: pass.
- `npm run test:selftests`: pass, 72/72 tests.

La nota histórica de la auditoría original se conserva debajo para trazabilidad del hallazgo inicial.

---

## 1. Resumen ejecutivo

- **No hay coverage real y fiable del proyecto completo hoy**: no existe Kover/Jacoco configurado en Gradle, no hay pipeline CI versionado y las tareas Gradle no llegan a configurarse en este entorno porque no se resuelve `com.android.application:8.7.3` desde los repositorios configurados.
- **Sí existe un coverage diagnóstico parcial para Functions** usando `node --test` vía `tsx --test --experimental-test-coverage`: reportó **62.82% line coverage / 82.23% branch / 70.17% funcs** sobre los ficheros TypeScript cargados por los selftests, pero en la auditoría original **no debía considerarse coverage oficial** porque el comando no estaba versionado como script npm y terminó con **3 tests fallidos**; la actualización `feature/functions-selftests-fix` añadió scripts y dejó los selftests en verde.
- **Estimación cualitativa de coverage total repo**: baja/media, aproximadamente **20–30% efectivo** si se pondera todo el código Kotlin + Functions por riesgo y superficie. La cobertura es buena en lógica pura de `shared/presentation` para Tarot/Horoscope/Economy/Premium analytics y en algunos servicios Functions, pero es casi nula en UI Compose, navegación real, Android/iOS platform code, Firebase Rules, Storage, emuladores y flujos end-to-end.
- **Módulos con tests Kotlin**: `composeApp` (solo test placeholder), `shared/domain`, `shared/data`, `shared/presentation`.
- **Módulos sin tests Kotlin efectivos**: `shared/di`, `androidUnitTest`, `androidInstrumentedTest`, `iosTest`; no se encontraron tests UI ni E2E versionados.
- **Functions tiene selftests significativos**, pero están repartidos en `*.selftest.ts`, `*SelfTest.ts` y herramientas manuales; `lint`, `build`, `test:selftests` y `coverage:selftests` están como scripts npm estándar tras la actualización `feature/functions-selftests-fix`.
- **Riesgo más importante antes de Closed Testing**: no es la ausencia de un porcentaje perfecto de coverage, sino la falta de ejecución confiable/automatizada de tests Gradle y mantener versionada la ejecución de selftests de Functions y evitar regresiones en economía/runtime config.

### Recomendación clara para Closed Testing

Antes de ampliar testers cerrados, hacer como mínimo:

1. **Desbloquear ejecución local/CI de Gradle tests** en un entorno donde AGP 8.7.3 resuelva correctamente.
2. **Mantener verdes** los selftests de Functions ya corregidos antes de usar economía premium/synastry/pendulum con testers reales.
3. **Ejecutar `npm run test:selftests`** para correr los selftests existentes de Functions de forma reproducible.
4. **Validar manualmente en dispositivo** login, compra/restauración, consumo de lunas, avatar Storage, push y navegación principal.
5. **No bloquear Closed Testing por no tener Kover/Jacoco aún**, siempre que el build real, selftests críticos y QA manual pasen; sí bloquear producción pública si no hay coverage/CI mínimo.

---

## 2. Inventario de tests

### 2.1 Resumen por módulo

| Módulo / área | Tests encontrados | Tipo dominante | Ejecución local/CI | Dependencias externas | Observación |
|---|---:|---|---|---|---|
| `composeApp` | 1 fichero / 1 caso | Unit placeholder commonTest | Gradle, hoy bloqueado por AGP | No | No cubre UI real ni navegación. |
| `shared/domain` | 9 ficheros | Unit puro commonTest | Gradle, hoy bloqueado por AGP | No | Cubre lógica de dominio seleccionada. |
| `shared/data` | 9 ficheros | Unit + integración ligera con fakes/settings | Gradle, hoy bloqueado por AGP | No emulador; usa fakes/settings in-memory | Buenas pruebas de repositorios concretos, sin Firebase real. |
| `shared/presentation` | 10 ficheros | Unit de ViewModels con fakes | Gradle, hoy bloqueado por AGP | No | Cobertura fuerte en ViewModels críticos, sin UI Compose. |
| `shared/di` | 0 | N/A | N/A | N/A | Sin tests de composición Koin. |
| `functions` | 16 ficheros detectados | selftests unitarios + herramienta local integración | `npm run lint`, `npm run build`, `npm run test:selftests`; coverage diagnóstico con `npm run coverage:selftests` | La herramienta `oracleLocalTest.ts` requiere emuladores Auth/Firestore/Functions | Selftests útiles; 72/72 pasan tras `feature/functions-selftests-fix`. |
| Firebase Rules | 0 tests | N/A | N/A | Requerirían Emulator Suite | No hay tests automatizados de `firestore.rules` ni `storage.rules`. |
| Android instrumented/UI | 0 | N/A | N/A | Emulador/dispositivo | Riesgo alto para login, billing, push, picker avatar. |
| iOS tests | 0 | N/A | N/A | Mac/Xcode | Riesgo fuera del alcance Google Play, pero relevante para TestFlight. |
| E2E | 0 | N/A | N/A | Emuladores/dispositivos/Firebase | No hay journeys automatizados. |

### 2.2 Detalle de tests Kotlin

| Ruta | Módulo | Tipo | Qué cubre | CI/local | Emulador/Firebase | Clasificación |
|---|---|---|---|---|---|---|
| `composeApp/src/commonTest/kotlin/com/agc/bwitch/ComposeAppCommonTest.kt` | `composeApp` | Unit placeholder | Test de ejemplo básico; no valida Compose UI ni navegación | Gradle common tests; hoy bloqueado por AGP | No | Unit puro, valor bajo |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/security/InputPolicyTest.kt` | `shared/domain` | Unit | Política de input/seguridad de texto | Gradle; hoy bloqueado | No | Unit puro |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/account/RestorePendingAccountDeletionUseCaseTest.kt` | `shared/domain` | Unit | Restauración/cancelación de borrado pendiente | Gradle; hoy bloqueado | No | Unit puro |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/astrology/synastry/SynastryReadingGeneratorTest.kt` | `shared/domain` | Unit | Generación/validaciones de lectura Synastry | Gradle; hoy bloqueado | No | Unit puro |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/astrology/horoscope/GetDailyHoroscopeUseCaseTest.kt` | `shared/domain` | Unit | Use case de horóscopo diario | Gradle; hoy bloqueado | No | Unit puro |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/astrology/horoscope/RewardDailyConstellationProgressUseCaseTest.kt` | `shared/domain` | Unit | Recompensa/progreso diario de constelación | Gradle; hoy bloqueado | No | Unit puro |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/astrology/birthchart/GenerateBirthEssenceUseCaseTest.kt` | `shared/domain` | Unit | Generación de Birth Essence | Gradle; hoy bloqueado | No | Unit puro |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/userprofile/ProfileCompletionTest.kt` | `shared/domain` | Unit | Completitud de perfil/onboarding | Gradle; hoy bloqueado | No | Unit puro |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/analytics/AnalyticsEventTest.kt` | `shared/domain` | Unit | Eventos/payloads analytics | Gradle; hoy bloqueado | No | Unit puro |
| `shared/domain/src/commonTest/kotlin/com/agc/bwitch/domain/moons/MoonUseCasesTest.kt` | `shared/domain` | Unit | Casos de uso de lunas/economía local | Gradle; hoy bloqueado | No | Unit puro |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/account/AccountDeletionCallableDataSourceTest.kt` | `shared/data` | Unit con fake callable | DataSource de borrado/restauración de cuenta | Gradle; hoy bloqueado | No | Integración ligera |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/astrology/horoscope/HoroscopeDailyRemoteDtoTest.kt` | `shared/data` | Unit | Mapping DTO horóscopo diario e idioma/cache key | Gradle; hoy bloqueado | No | Unit puro |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/astrology/horoscope/SettingsHoroscopeDailyRepositoryTest.kt` | `shared/data` | Unit con settings fake | Caché local de horóscopo por idioma y fallback legacy | Gradle; hoy bloqueado | No | Integración ligera local |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/astrology/horoscope/BackendFirstConstellationProgressRepositoryTest.kt` | `shared/data` | Unit con fakes | Repositorio backend-first de progreso constelación y fallback local | Gradle; hoy bloqueado | No | Integración ligera |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/astrology/birthchart/BirthEssenceRemoteDtoTest.kt` | `shared/data` | Unit | Mapping DTO Birth Essence e idioma legacy/regional | Gradle; hoy bloqueado | No | Unit puro |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/oracle/OracleRepositoryImplTest.kt` | `shared/data` | Unit con fake callable | Normalización de idioma al llamar backend Oracle | Gradle; hoy bloqueado | No | Integración ligera |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/settings/BillingBackedSubscriptionRepositoryTest.kt` | `shared/data` | Unit con fakes billing/backend | Entitlements Premium, restore y catálogo mensual | Gradle; hoy bloqueado | No Google Play real | Integración ligera crítica |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/userprofile/SettingsUserProfileRepositoryTest.kt` | `shared/data` | Unit con settings fake | Persistencia local de perfil/description | Gradle; hoy bloqueado | No | Integración ligera local |
| `shared/data/src/commonTest/kotlin/com/agc/bwitch/data/moons/BackendFirstMoonRepositoryTest.kt` | `shared/data` | Unit con fakes | Balance de lunas backend-first y fallback local | Gradle; hoy bloqueado | No | Integración ligera crítica |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/tarot/TarotViewModelTest.kt` | `shared/presentation` | ViewModel unit | Idioma, retry, gasto de lunas, errores legacy, deck selection | Gradle; hoy bloqueado | No | Unit ViewModel crítico |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/astrology/synastry/SynastryViewModelTest.kt` | `shared/presentation` | ViewModel unit | Idioma y validación de signos obligatorios | Gradle; hoy bloqueado | No | Unit ViewModel |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/astrology/horoscope/HoroscopeViewModelTest.kt` | `shared/presentation` | ViewModel unit | Horóscopo diario/semanal/mensual, unlocks, premium, overlays, rewards | Gradle; hoy bloqueado | No | Unit ViewModel crítico |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/economy/EconomyViewModelTest.kt` | `shared/presentation` | ViewModel unit | Login diario, rewarded ads, analytics, paywall origins/impressions | Gradle; hoy bloqueado | No | Unit ViewModel crítico |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/auth/SessionViewModelAccountDeletionTest.kt` | `shared/presentation` | ViewModel unit | Restauración/borrado pendiente, expiración, sign-out async | Gradle; hoy bloqueado | No | Unit ViewModel crítico auth |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/oracle/OracleAskViewModelTest.kt` | `shared/presentation` | ViewModel unit | Idioma, refresh economía, errores economía/ad legacy, retry, límites input | Gradle; hoy bloqueado | No | Unit ViewModel crítico |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/userprofile/SettingsViewModelAnalyticsTest.kt` | `shared/presentation` | ViewModel unit | Premium CTA, restore, entitlement active/inactive, catálogo mensual, analytics | Gradle; hoy bloqueado | No Google Play real | Unit ViewModel crítico monetización |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/userprofile/OnboardingProfileViewModelTest.kt` | `shared/presentation` | ViewModel unit | Evita guardar perfil si username falta al subir avatar | Gradle; hoy bloqueado | No | Unit ViewModel |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/moons/MoonStoreViewModelTest.kt` | `shared/presentation` | ViewModel unit | Compra de packs de lunas, productos no disponibles, consume/feedback | Gradle; hoy bloqueado | No Google Play real | Unit ViewModel monetización |
| `shared/presentation/src/commonTest/kotlin/com/agc/bwitch/presentation/analytics/FakeAnalyticsTracker.kt` | `shared/presentation` | Test helper | Fake compartido para analytics | Gradle; hoy bloqueado | No | Helper, no caso de test |

### 2.3 Detalle de Functions selftests/scripts

| Ruta | Tipo | Qué cubre | Ejecución local/CI | Emulador/Firebase | Clasificación |
|---|---|---|---|---|---|
| `functions/src/account/service.selftest.ts` | `node:test` selftest | Patches de borrado/restauración de cuenta | Manual con `npx tsx --test`; no script npm | No | Unit puro |
| `functions/src/birthessence/promptHardening.selftest.ts` | `node:test` selftest | Delimitación de prompt y reglas anti prompt injection | Manual | No | Unit puro |
| `functions/src/economy/deckProgress.selftest.ts` | `node:test` selftest | Progreso de decks por gasto de lunas e idempotencia | Manual | No | Unit puro crítico economía |
| `functions/src/economy/economyResolvers.selftest.ts` | `node:test` selftest | Resolución FREE/PREMIUM/MOON/REJECT para Tarot/Oracle/Birth/Synastry/Horoscope | `npm run test:selftests`; verde tras actualizar expectativa obsoleta de `TAROT_1` | No | Unit puro crítico economía |
| `functions/src/economy/runtimeConfig.selftest.ts` | `node:test` selftest | Path/config de flags runtime de economía | `npm run test:selftests`; verde tras incluir flags Synastry/Pendulum | No | Unit puro crítico config |
| `functions/src/firestore/zodiacSigns.selftest.ts` | selftest ejecutable | Normalización/signos zodiacales | Manual | No | Unit puro |
| `functions/src/llm/providers/DeepSeekProvider.selftest.ts` | selftest ejecutable async | Parsing JSON/respuesta proveedor DeepSeek con fake fetch | Manual | No red real | Unit puro LLM |
| `functions/src/oracle/callables/payloadBuilders.selftest.ts` | `node:test` selftest | Limpieza de `undefined` y payloads economy | Manual | No | Unit puro |
| `functions/src/oracle/oracle/oracleSelfTest.ts` | selftest ejecutable | Prompt/schema Oracle básico | Manual | No | Unit puro |
| `functions/src/oracle/oracle/promptInputRisk.selftest.ts` | `node:test` selftest | Detección de prompt injection y delimitación de pregunta | Manual | No | Unit puro seguridad LLM |
| `functions/src/oracle/tarot/promptHardening.selftest.ts` | `node:test` selftest | Delimitación prompt Tarot y secrecy rules | Manual | No | Unit puro seguridad LLM |
| `functions/src/oracle/tarot/tarotSelfTest.ts` | selftest ejecutable | Draw determinista, posiciones Tarot 3, idioma/nombres cartas | Manual; ejecutado individualmente OK | No | Unit puro |
| `functions/src/premium/service.selftest.ts` | `node:test` selftest | Validación Google Play purchase/restore/refresh con fakes | Manual | No Google Play real | Unit puro crítico monetización |
| `functions/src/userprofile/callables/saveUserProfile.selftest.ts` | `node:test` selftest | Normalización/validación/sanitización de perfil | Manual | No | Unit puro |
| `functions/src/utils/inputNormalization.selftest.ts` | `node:test` selftest | Normalización de inputs y límites | Manual | No | Unit puro seguridad/input |
| `functions/tools/oracleLocalTest.ts` | Herramienta integración local | Auth anónimo + callable `oracleGetStatus`, `tarotDraw`, `oracleAsk` contra emuladores | Manual; no ejecutada porque requiere emuladores arrancados | Sí: Auth 9099, Firestore 8080, Functions 5001 | Integración local/E2E parcial |

---

## 3. Coverage tooling

### 3.1 Estado actual

| Tooling | Estado | Evidencia/impacto |
|---|---|---|
| Jacoco | No encontrado | No hay plugin/config Gradle Jacoco ni tareas de report versionadas. |
| Kover | No encontrado | No hay plugin `org.jetbrains.kotlinx.kover`; sin cobertura KMP oficial. |
| Gradle coverage reports | No encontrado | `./gradlew tasks` ni siquiera configura por AGP; no hay tareas `kover*`/`jacoco*`. |
| npm coverage | No encontrado como script | `functions/package.json` tiene `lint`, `build`, deploy/admin scripts; no `test` ni `coverage`. |
| `node:test` coverage | Posible de forma ad-hoc | `npx --prefix functions tsx --test --experimental-test-coverage ...` genera reporte diagnóstico, versionado como `coverage:selftests`; la auditoría original fallaba por tests rojos ya corregidos. |
| Firebase Rules tests | No encontrados | No hay `@firebase/rules-unit-testing`, Jest/Vitest ni tests de Emulator Suite para `firestore.rules`/`storage.rules`. |
| CI config | No encontrado | No hay `.github/workflows`, `.gitlab-ci.yml`, `azure-pipelines.yml` o equivalente versionado. |

### 3.2 ¿Se puede obtener coverage real hoy?

- **Proyecto KMP completo**: **no**. Falta Kover/Jacoco y la configuración Gradle falla antes de ejecutar tests en este entorno por resolución de AGP.
- **Functions**: **parcial/diagnóstico sí**, no oficial. El comando ad-hoc reportó **62.82% line coverage** pero con **3 tests fallidos**. Tras `feature/functions-selftests-fix`, los selftests están estabilizados y el script quedó versionado; sigue siendo métrica diagnóstica hasta definir coverage oficial.

### 3.3 Comandos exactos para generar coverage cuando se habilite tooling

Hoy no existen estos comandos como tareas versionadas, pero la ruta recomendada es:

```bash
# KMP/Kotlin: añadir Kover en build.gradle.kts raíz o subproyectos y luego ejecutar
./gradlew koverXmlReport koverHtmlReport

# O por módulo, si se decide granularidad modular
./gradlew :shared:domain:koverXmlReport :shared:data:koverXmlReport :shared:presentation:koverXmlReport

# Functions: añadir script npm estable, por ejemplo test:selftests y coverage:selftests
npm --prefix functions run test:selftests
npm --prefix functions run coverage:selftests
```

### 3.4 Qué habría que añadir para coverage fiable

1. **Kover para Kotlin Multiplatform** en Gradle, con exclusiones explícitas para `composeApp` generated/resources si procede.
2. **Un script npm oficial** para selftests de Functions, por ejemplo:
   - `test:selftests`: `tsx --test "src/**/*.selftest.ts"` más wrappers `*SelfTest.ts` si se mantienen.
   - `coverage:selftests`: mismo comando con `--experimental-test-coverage` o migración a `c8`/`vitest` si se quiere LCOV estable.
3. **CI mínimo** que ejecute:
   - Gradle common tests.
   - Android unit tests.
   - Functions lint/build/selftests.
   - Firebase rules tests cuando existan.
4. **Rules unit tests** con Firebase Emulator Suite para `firestore.rules` y `storage.rules`.
5. **Opcional después de Closed Testing**: uno o dos instrumented tests Android smoke para navegación/login mockeado; no meter frameworks pesados antes si retrasa el cierre.

---

## 4. Comandos ejecutados y resultados

| Comando | Resultado | Observaciones |
|---|---|---|
| `pwd && rg --files -g 'AGENTS.md' -g '!**/.gradle/**' -g '!**/build/**' && git status --short --branch` | Pass | Repo en `/workspace/BWitch`; solo `AGENTS.md`; branch local reportada `## work`. |
| `find . ... -type d \( -name '*Test' -o -name 'test' -o -name 'tests' -o -name 'selftests' \) -print` | Pass | Solo directorios `commonTest` Kotlin detectados; no `androidUnitTest`, `androidInstrumentedTest` ni `iosTest`. |
| `rg --files ... | rg '(^|/)(commonTest|androidUnitTest|androidInstrumentedTest|iosTest|test|tests|selftests|__tests__)/...'` | Pass | Inventario inicial de Kotlin tests y selftests/scripts Functions. |
| `rg -n "kover|jacoco|coverage|nyc|c8|lcov|vitest|jest|node:test|firebase emulators|rules-unit-testing|test" ...` | Pass | No se detectó Jacoco/Kover/coverage Gradle/npm; sí deps `firebase-functions-test` y referencias `node:test`. |
| `./gradlew --version` | Pass | Gradle 8.9 disponible; JVM launcher 25, daemon Java 21. |
| `./gradlew tasks --all ...` | Fail por entorno/dependencias | Falla configurando Gradle: plugin `com.android.application` 8.7.3 no encontrado en Google/Maven/Gradle Plugin Portal. |
| `./gradlew :shared:domain:allTests :shared:data:allTests :shared:presentation:allTests :composeApp:allTests --continue` | Fail por entorno/dependencias | Mismo bloqueo AGP; no llega a ejecutar tests Kotlin. |
| `npm --prefix functions run lint` | Pass | ESLint OK; solo warning npm `Unknown env config "http-proxy"`. |
| `npm --prefix functions run build` | Pass | TypeScript `tsc` OK; solo warning npm `Unknown env config "http-proxy"`. |
| `npx --prefix functions tsx functions/src/oracle/tarot/tarotSelfTest.ts` | Pass | Selftest Tarot individual OK. |
| `npx --prefix functions tsx functions/src/oracle/oracle/oracleSelfTest.ts` | Pass | Selftest Oracle individual OK. |
| `npx --prefix functions tsx --test $(rg --files functions/src -g '!functions/lib/**' | rg '\.selftest\.ts$')` | Fail histórico por tests rojos | 72 tests: 69 pass, 3 fail. Fallaban `economyResolvers.selftest.ts` y `runtimeConfig.selftest.ts`; corregido después con `npm run test:selftests` en `feature/functions-selftests-fix`. |
| `NODE_V8_COVERAGE=functions/.coverage/tmp npx --prefix functions tsx --test --experimental-test-coverage $(rg --files functions/src -g '!functions/lib/**' | rg '\.selftest\.ts$')` | Fail histórico por tests rojos, generó métrica diagnóstica | 72 tests: 69 pass, 3 fail. Reporte diagnóstico: all files **62.82% line / 82.23% branch / 70.17% funcs**. Se borró `functions/.coverage`; repetir con `npm run coverage:selftests` si se necesita nueva métrica diagnóstica. |

---

## 5. Clasificación de cobertura por área

| Área | Cobertura actual | Evidencia | Riesgo |
|---|---|---|---|
| Auth | Media en ViewModel/domain; baja en Firebase real | `SessionViewModelAccountDeletionTest`, `RestorePendingAccountDeletionUseCaseTest`, account service selftests | Alto: login/borrado/restauración pueden bloquear usuario; falta emulador/Auth real. |
| Profile/onboarding | Media en validación/cache; baja en UI/avatar real | `ProfileCompletionTest`, `SettingsUserProfileRepositoryTest`, `OnboardingProfileViewModelTest`, `saveUserProfile.selftest.ts` | Alto en avatar/Storage; medio en perfil textual. |
| Horoscope | Media/alta en ViewModel/data/domain; baja en backend real | Tests de `HoroscopeViewModel`, DTOs, repos cache/progress, use cases | Medio/alto: unlocks y premium deben validarse manualmente. |
| Tarot | Media/alta en ViewModel + selftests prompt/draw; baja en callable/emulador real | `TarotViewModelTest`, `tarotSelfTest.ts`, prompt hardening | Alto si consume lunas/LLM; faltan pruebas callable con emulador. |
| Oracle | Media en ViewModel/data/prompt/input; baja en callable/emulador real | `OracleAskViewModelTest`, `OracleRepositoryImplTest`, oracle prompt/input selftests | Alto por LLM/economía/retries; falta E2E local reproducible. |
| Birth Essence | Media en domain/data/prompt; baja en UI/backend real | `GenerateBirthEssenceUseCaseTest`, `BirthEssenceRemoteDtoTest`, prompt hardening | Medio/alto por coste LLM/economía. |
| Synastry | Media en domain/presentation/economy resolver; selftests economía verdes tras corrección | `SynastryViewModelTest`, `SynastryReadingGeneratorTest`, economy selftests | Alto: área monetizada; mantener selftests de economía verdes. |
| Pendulum | Baja | Solo aparece en resolvers/runtime config; sin ViewModel/data/UI tests dedicados detectados | Medio/alto si monetizado; falta smoke manual. |
| Economy | Media en ViewModels/repos/resolvers; selftests Functions verdes tras corrección | `EconomyViewModelTest`, `BackendFirstMoonRepositoryTest`, `economyResolvers.selftest.ts` | Muy alto: bugs cuestan dinero/lunas y pueden romper límites. |
| Premium billing | Media en fakes; baja contra Google Play real | `BillingBackedSubscriptionRepositoryTest`, `SettingsViewModelAnalyticsTest`, `premium/service.selftest.ts` | Muy alto: compras/restores bloquean monetización. |
| Push notifications | Baja/nula | No se detectan tests para `AndroidPush*`/FCM | Medio para Closed Testing; alto antes de producción pública si push es core. |
| Storage/avatar | Baja | Sin rules tests ni instrumented picker/upload; solo ViewModel evita save sin username | Alto por privacidad y UX; validar manualmente y añadir rules tests. |
| Firestore rules | Nula automatizada | `firestore.rules` existe, pero sin tests de rules | Muy alto para seguridad de datos. |
| Storage rules | Nula automatizada | `storage.rules` existe, pero sin tests de rules | Alto para avatar cross-user/MIME/size. |
| LLM retries/refunds/watchdog | Baja/media parcial | `DeepSeekProvider.selftest.ts`, prompt hardening, economía resolvers; sin integración watchdog/refund | Muy alto si doble cargo o llamadas colgadas. |
| i18n | Media en lógica idioma; baja en UI strings completa | Tests de idioma en Tarot/Oracle/Horoscope/Synastry y DTOs | Medio; QA manual puede cubrir visualmente en Closed Testing. |
| Navigation/AppRoot | Nula | No hay UI tests ni navigation smoke | Alto para bloqueo de journeys, aunque QA manual puede cubrir antes de Closed Testing. |

---

## 6. Riesgos antes de Closed Testing

### 6.1 Áreas críticas sin tests suficientes

- **Android login real / Google Sign-In / Firebase Auth**: unit tests no sustituyen el flujo real de Credential Manager + Firebase.
- **Google Play Billing real**: hay fakes de repositorios, pero no instrumented/smoke con Play Billing test products.
- **Firebase Rules**: cero tests automatizados para Firestore/Storage; riesgo de acceso cross-user o bloqueo accidental.
- **Storage/avatar end-to-end**: picker, crop/upload, MIME/size/rules solo se puede validar manualmente hoy.
- **Navigation/AppRoot**: sin smoke tests; una regresión puede bloquear acceso a features aunque ViewModels pasen.
- **Functions economía/runtime config**: los selftests existentes fallaban en la auditoría original; quedaron verdes en `feature/functions-selftests-fix` y deben mantenerse en CI/local antes de Closed Testing.

### 6.2 Áreas donde un bug puede costar dinero o saldo

- Consumo de lunas en Tarot/Oracle/Horoscope/Synastry/Pendulum/Birth Essence.
- Rewarded ads y límites diarios.
- Premium entitlement active/inactive/restore.
- LLM retries/refunds/watchdog si una llamada falla después de reservar coste.
- Moon pack purchases y consumo/confirmación de compra.

### 6.3 Áreas donde un bug puede bloquear login/compra

- Auth bootstrap y restore de cuenta pendiente.
- Google Sign-In Android.
- Premium restore/validate contra backend.
- Billing product catalog y estado `available/unavailable`.
- Redirección post-login/onboarding/navigation.

### 6.4 Áreas donde basta QA manual para Closed Testing

- Revisión visual de pantallas Compose y tema.
- i18n visual de idiomas principales.
- Navegación bottom/tab/back stack básica.
- Push notification display/routing si no es requisito central para testers.
- Avatar crop visual, siempre que reglas Storage ya estén revisadas manualmente.

### 6.5 Áreas que deberían tener tests antes de producción pública

- Firebase Rules automatizadas.
- CI obligatorio con Gradle + Functions.
- Android instrumented smoke para login/premium critical path.
- Coverage Kover por módulos shared.
- Selftests Functions verdes y versionados como `npm test`/`npm run test:selftests`.
- E2E local con Emulator Suite para Oracle/Tarot/economy happy path + idempotencia.

---

## 7. Plan recomendado por fases

### Fase 0 — Antes de ampliar Closed Testing (mínimo)

1. Resolver entorno AGP/Gradle y ejecutar:
   - `./gradlew :shared:domain:allTests :shared:data:allTests :shared:presentation:allTests :composeApp:allTests --continue`
   - si existen tareas Android unit: `./gradlew testDebugUnitTest`
2. Mantener verdes los selftests rojos de Functions ya corregidos y documentar cualquier expectativa que cambie por diseño.
3. Usar el script npm liviano `test:selftests` para selftests existentes, sin framework pesado.
4. QA manual obligatoria en Android:
   - Login/logout.
   - Onboarding/profile/avatar.
   - Premium subscribe/restore con cuenta test.
   - Lunas: daily login, rewarded ad, moon pack si aplica.
   - Tarot/Oracle/Horoscope/Synastry/Pendulum/Birth Essence happy path y error path básico.
5. Validar Firestore/Storage rules manualmente en Emulator Suite o Rules Playground antes de exponer datos reales a testers.

### Fase 1 — Antes de producción pública

1. Añadir Kover para `shared/domain`, `shared/data`, `shared/presentation` y decidir si `composeApp` se mide o se excluye parcialmente por UI/generated.
2. Añadir Firebase Rules tests con Emulator Suite:
   - owner can read/write allowed profile/avatar paths.
   - other user denied.
   - unauthenticated denied.
   - MIME/size invalid denied para Storage.
3. Añadir CI versionado con Gradle + Functions + rules tests.
4. Añadir Android instrumented smoke mínimo:
   - App arranca.
   - Login mock/fake o emulator auth.
   - Navegación a store/settings y pantalla principal.
5. Añadir E2E local Functions con emuladores para economía + idempotencia + refunds/retries.

### Fase 2 — Post-launch

1. Coverage thresholds progresivos, no agresivos al inicio:
   - `shared/domain`: 60–70%.
   - `shared/presentation`: 50–60%.
   - `shared/data`: 40–50%.
   - `functions`: 60% y subiendo por áreas críticas.
2. Snapshot/golden tests selectivos de UI si Compose Multiplatform tooling lo permite sin coste alto.
3. Tests iOS en Mac/Xcode para TestFlight: smoke de arranque, Firebase config, sign-in y compras si se lanzan en iOS.
4. Monitoreo post-launch: alertas Cloud Functions para errores de economía, purchases y LLM provider.

### Qué no merece la pena testear ahora

- Pixel-perfect de toda la UI Compose.
- Every-screen UI automation antes de tener smoke y CI estable.
- Tests iOS desde Linux en esta rama.
- Frameworks pesados de E2E si retrasan Closed Testing; priorizar selftests verdes, rules tests y QA manual.

---

## 8. Conclusión

BWitch/ATHENA tiene una base de tests unitarios útil en dominio, data y sobre todo presentation, además de selftests importantes en Functions. Sin embargo, para Closed Testing el proyecto aún depende demasiado de validación manual en las rutas que más importan a usuarios reales: login, compras, economía, Firebase Rules, avatar Storage y navegación.

**Go/No-Go recomendado para Closed Testing**:

- **Go condicionado** si Gradle tests se ejecutan en un entorno correcto, Functions lint/build pasan, los selftests críticos se mantienen verdes, y QA manual de journeys críticos pasa.
- **No-Go** para producción pública hasta tener CI mínimo, rules tests automatizados y coverage KMP/Functions oficial.
