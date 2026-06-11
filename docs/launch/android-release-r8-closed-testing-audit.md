# Android release R8 audit — Closed Testing

Fecha: 2026-06-11

App: ATHENA / `com.agc.bwitch`

Scope: preparar una build Android `release` lo más cercana posible a Producción, activando R8/obfuscación sin cambios funcionales.

## 1. Resultado ejecutivo

- **Antes de esta auditoría, `release` no ejercitaba R8**: `isMinifyEnabled = false`, sin `shrinkResources` y sin archivo ProGuard/R8 propio.
- **Cambio mínimo aplicado**: `release` activa `isMinifyEnabled`, `isShrinkResources` y usa `proguard-android-optimize.txt` más `composeApp/proguard-rules.pro`.
- **Reglas añadidas**: solo preservación de atributos de metadata/annotations/signatures. No se añadieron `-keep` globales ni reglas que desactiven obfuscación para paquetes completos.
- **Mapping files**: con `isMinifyEnabled = true`, AGP/R8 generará el mapping de release en `composeApp/build/outputs/mapping/release/mapping.txt` al completar `:composeApp:assembleRelease`.
- **Validación bloqueada en este entorno**: `./gradlew :composeApp:assembleRelease` no llegó a configurar el proyecto porque no pudo resolver el plugin `com.android.application:8.7.3` desde los repositorios configurados. Debe repetirse en el entorno de build real antes de Closed Testing.

## 2. Configuración release auditada

| Punto | Estado previo | Estado tras cambio | Riesgo / nota |
|---|---:|---:|---|
| `minifyEnabled` / `isMinifyEnabled` | `false` | `true` | Ahora Closed Testing sí ejecuta R8/obfuscación. |
| `shrinkResources` / `isShrinkResources` | No configurado | `true` | Reduce recursos no usados; requiere revisar visualmente iconos/raw/resources en release. |
| `proguardFiles` | No configurado | `getDefaultProguardFile("proguard-android-optimize.txt")` + `proguard-rules.pro` | Usa el preset optimizado estándar de Android y reglas app mínimas. |
| `proguard-android-optimize.txt` | No usado | Usado | Más cercano a Producción que `proguard-android.txt`; puede exponer bugs de optimización antes del rollout. |
| Mapping file | No se generaba mapping útil al no minificar | Se generará al ensamblar release | Guardar el mapping por cada build subida a Play Console. |

## 3. Reglas ProGuard/R8 existentes

No se encontraron archivos ProGuard/R8 previos en el repo (`*.pro`, `*proguard*`, `consumer-rules.pro`) antes de esta auditoría. Se añadió `composeApp/proguard-rules.pro` como punto único para reglas app-specific.

## 4. Compatibilidad R8 por SDK/librería

| Área | Uso en ATHENA | Compatibilidad esperada | Riesgo residual |
|---|---|---|---|
| Firebase Auth / Firestore / Functions / Storage vía GitLive | Dependencias KMP en `shared:data`; uso con serializers explícitos y SDK GitLive. | Riesgo bajo-medio. GitLive y Firebase Android SDK suelen aportar reglas consumer; el riesgo real está en validar login/callables/Firestore/Storage en release minificada. | Validar sesión existente, login, callable auth, Firestore reads/writes y Storage avatar en release. |
| Firebase Messaging (FCM) | SDK Android nativo y `BwitchFirebaseMessagingService` declarado en manifest. | Riesgo bajo. El service está en manifest y extiende `FirebaseMessagingService`; R8/AGP debe conservar entry points Android. | Validar `onNewToken`, foreground notification y tap/deep route en release. |
| Google Sign-In / Credential Manager / Google ID | Credential Manager + GoogleIdTokenCredential. | Riesgo bajo-medio. No hay reflection propia; depende de Play Services/Credential Manager consumer rules. | Validar cuenta nueva y cuenta ya autorizada en release; revisar `default_web_client_id`. |
| Google Play Billing | `billing-ktx:8.0.0` en `shared:data` Android; data sources Android para subs y consumibles. | Riesgo bajo. Uso tipado de BillingClient/ProductDetails/Purchase sin reflection propia. | Validar catálogo, flujo sandbox, pending/cancel, acknowledge/consume y restore en release. |
| AdMob | `play-services-ads:24.3.0`, `MobileAds.initialize`, rewarded ads. | Riesgo bajo si App ID y ad unit release son correctos. SDK aporta reglas consumer. | El manifest aún debe inyectar App ID real en release; validar rewarded load/show en release. |
| Koin | DSL con `single`/`factory`; inyección por tipo y un uso `KoinJavaComponent.inject`. | Riesgo bajo-medio. Koin normalmente no requiere conservar nombres de clases para el DSL usado, pero una definición faltante solo aparece en runtime. | Ejecutar navegación smoke release y flujos que resuelven platform services. |
| Ktor / OkHttp | Dependencia Ktor OkHttp; engine Android inyectado desde platform module. | Riesgo bajo. No se detectó networking Ktor activo por `HttpClient`/`ContentNegotiation` en el código actual; sí hay dependencia. | Si se reactiva `HttpClient`, validar requests release y añadir reglas solo ante fallo concreto. |
| Kotlin Serialization | DTOs `@Serializable`, llamadas `.serializer()` explícitas, `Json { ignoreUnknownKeys = true }`. | Riesgo bajo-medio. El patrón de serializers explícitos es favorable para R8; se preservan atributos de annotations/signatures. | Validar callables/Firestore decode/local Settings decode en release; evitar reglas globales salvo error concreto. |
| Compose Multiplatform | UI compartida en `composeApp`; release R8 con resource shrinking. | Riesgo bajo-medio. Compose suele ser compatible con R8, pero resource shrink puede exponer recursos referenciados indirectamente. | Smoke visual de pantallas principales, navegación, themes, launcher icon y raw sound. |

## 5. Puntos de riesgo detectados

### 5.1 Reflection

- `AndroidFirebaseAnalyticsTracker` usa `Class.forName("com.google.firebase.analytics.FirebaseAnalytics")` y métodos por reflection. La dependencia `firebase-analytics` no está declarada en `composeApp`, así que hoy degrada a no-op. Si se añade analytics más adelante, validar release; si R8 rompe esta integración, añadir regla específica para `com.google.firebase.analytics.FirebaseAnalytics`, no reglas globales.
- `MainActivity` usa `KoinJavaComponent.inject(...::class.java)` para tres dependencias Android/platform; no depende de nombres de clase, pero sí de que los módulos Koin se inicialicen correctamente en release.

### 5.2 Serialización

- Hay numerosos DTOs `@Serializable` en `shared:data` y algunos modelos de dominio serializables. El uso principal es con serializers explícitos (`Foo.serializer()`), lo cual reduce el riesgo frente a lookup reflectivo.
- No se detectaron serializers polimórficos dinámicos ni companions serializables con nombre custom que justifiquen `-keep` de clases completas.
- Se añadieron únicamente `-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod` para no retirar metadata necesaria por serialización/librerías JVM.

### 5.3 Clases cargadas dinámicamente / entry points Android

- Entry points Android declarados en manifest: `BWitchApplication`, `MainActivity`, `BwitchFirebaseMessagingService` y `FileProvider`. No se añadieron reglas manuales para ellos porque el manifiesto/AGP/R8 ya debe tratarlos como entry points; validar instalación/arranque release confirma este punto.
- FCM recibe intents por action `com.google.firebase.MESSAGING_EVENT`; validación manual de push en release es obligatoria.

### 5.4 APIs sensibles a obfuscación

- Billing y Google Sign-In dependen de SDKs Google/Play Services y callbacks; el riesgo real es runtime/configuración de Play Console, no reglas app-specific.
- AdMob depende de App ID/Ad Unit ID release. El manifest usa `${ADMOB_APP_ID}` y `release` lo resuelve desde Gradle property/env var, con fallback al App ID de test si no está definido. Para Closed Testing cercano a Producción, definir `ADMOB_APP_ID` real en el entorno de build y no depender del fallback.
- App Check release instala Play Integrity provider. Antes de testers externos, la app release debe estar registrada en Firebase App Check/Play Integrity con el signing certificate correcto.

## 6. Cambios aplicados

1. `composeApp/build.gradle.kts`
   - `release.isMinifyEnabled = true`
   - `release.isShrinkResources = true`
   - `release.proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`

2. `composeApp/src/androidMain/AndroidManifest.xml`
   - El App ID de AdMob deja de estar hardcodeado y pasa a resolverse con `${ADMOB_APP_ID}`, usando los placeholders ya configurados para debug/release.

3. `composeApp/proguard-rules.pro`
   - Añadido archivo mínimo para reglas app-specific.
   - Añadida solo preservación de atributos de signatures/annotations/inner-enclosing metadata.

## 7. Checklist obligatorio antes de subir a Closed Testing

### Build / artefacto

- Ejecutar `./gradlew :composeApp:clean :composeApp:assembleRelease` en el entorno Android real.
- Confirmar que existe `composeApp/build/outputs/mapping/release/mapping.txt` y archivarlo junto al APK/AAB subido.
- Revisar warnings R8: no ignorar `Missing class`, `whyareyoukeeping`, reglas sugeridas o warnings de serializers.
- Generar el artefacto que se subirá a Play (`bundleRelease` si el canal usa AAB) y validar que usa el mismo signing/config que Closed Testing.

### Config externa release

- Definir `ADMOB_APP_ID` real en Gradle property/env var del build de release.
- Confirmar `google-services.json` de release y `default_web_client_id` correcto.
- Confirmar App Check Play Integrity configurado para el certificado de firma usado en Closed Testing.
- Confirmar productos/subs en Play Console y tester license accounts.

### Smoke manual release minificada

- Instalación limpia de release y arranque sin crash.
- Login Google + Firebase Auth.
- Sesión existente tras matar/reabrir app.
- Firestore reads/writes principales: perfil, horóscopos, tarot/oracle si aplica.
- Cloud Functions callable: economía/status/claim y flujos premium/restore.
- Storage avatar: seleccionar, recortar, subir, ver y persistir.
- FCM: permiso, token, registro backend, push foreground/background, tap navigation.
- Billing: catálogo premium, compra sandbox, pending/cancel, acknowledge, restore; moon packs si están habilitados para testers.
- AdMob rewarded: load/show/reward/cancel/failure con IDs esperados.
- Navegación principal Compose y pantallas críticas en tema release.
- Recurso raw `tarot_card_flip.wav`, iconos launcher y FileProvider/share.

## 8. Criterio Go/No-Go

**Go para Closed Testing** solo si:

- `assembleRelease` o `bundleRelease` pasa con R8 activo.
- Mapping file se genera y queda archivado.
- No hay warnings R8 sin explicar.
- Smoke manual release minificada pasa en al menos un dispositivo real con Play Services.
- Login, App Check, Billing, FCM y AdMob se validan con configuración real/sandbox de Closed Testing.

**No-Go** si:

- El build requiere `-dontwarn` amplios sin entender la causa.
- Algún SDK crítico solo funciona en debug/no-minify.
- Falta mapping file del artefacto subido.
- Release usa por accidente App ID de AdMob de test cuando el objetivo es una prueba cercana a Producción.
