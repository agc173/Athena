# iOS Readiness — BWitch

Fecha de auditoría: 2026-06-02  
Rama: `feature/ios-launch-preparation`  
Contexto: auditoría desde Linux/CI sin Mac ni Xcode; no se implementan cambios grandes.

## Resumen ejecutivo

BWitch ya tiene una base KMP/iOS funcional a nivel de estructura: target iOS en Gradle, proyecto `iosApp`, entrypoint Swift que carga el `UIViewController` de Compose, DI de plataforma con Ktor Darwin y varios `actual` iOS. El estado estimado de preparación iOS es **aprox. 45%** para un primer TestFlight técnico y **aprox. 30%** para lanzamiento App Store completo.

La app puede compilar conceptualmente para iOS porque el código común usa GitLive Firebase y hay `actual` iOS para los puntos KMP principales. Sin embargo, no se pudo validar compilación real desde este entorno porque Gradle no pudo resolver el plugin Android/AGP remoto o cacheado. La validación iOS definitiva sigue dependiendo de Mac/Xcode.

Los bloqueantes principales para iOS son nativos/configuracionales: falta `GoogleService-Info.plist`, signing/team/capabilities, configuración Firebase iOS, Google Sign-In iOS, StoreKit/receipt validation si Premium/compras son requisito de negocio, APNs/FCM si push entra en el primer release, privacidad/App Store metadata y QA en dispositivos.

## Archivos y áreas auditadas

| Área | Archivos/rutas auditadas | Hallazgos |
|---|---|---|
| KMP/Gradle | `composeApp/build.gradle.kts`, `shared/*/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts` | Targets `iosArm64` e `iosSimulatorArm64`; no `iosX64`; frameworks estáticos; sin CocoaPods plugin. |
| iOS host | `iosApp/iosApp.xcodeproj`, `iosApp/iosApp/ContentView.swift`, `iosApp/iosApp/iOSApp.swift`, `iosApp/iosApp/Info.plist`, `iosApp/Configuration/Config.xcconfig` | Proyecto Xcode existe; bundle/team son placeholders; Info.plist mínimo; launch generado; app ignora safe area globalmente. |
| iOS actuals | `composeApp/src/iosMain/**`, `shared/data/src/iosMain/**` | Hay actuals para plataforma, DI, share text, avatar picker, haptics, settings/storage, language y Firebase App Check bootstrap no-op. |
| Firebase | `shared/data/src/commonMain/**`, `shared/data/src/iosMain/**`, `composeApp/src/iosMain/**` | Auth/Firestore/Functions/Storage usan GitLive en commonMain; App Check iOS es no-op; Analytics iOS no-op; falta configuración nativa. |
| Login | `shared/data/src/commonMain/kotlin/.../FirebaseAuthRepository.kt`, `composeApp/src/androidMain/.../GoogleIdTokenProviderAndroid.kt`, `composeApp/src/iosMain/.../PlatformModule.ios.kt` | Email/password debería funcionar con GitLive si Firebase iOS está configurado; Google Sign-In solo tiene proveedor Android; Apple Sign-In no aparece implementado. |
| Premium/compras | `shared/data/src/commonMain/.../billing`, `shared/data/src/androidMain/.../googleplay`, `composeApp/src/iosMain/...PurchaseLauncher.ios.kt`, `functions/src/**` | Android Play Billing existe; iOS está unsupported/no-op; backend actual valida Google Play, no App Store. |
| Push | `composeApp/src/androidMain/...notifications`, `composeApp/src/iosMain/...SettingsPush*.ios.kt`, `shared/data/src/commonMain/.../FunctionsPushRegistrationRepository.kt`, `functions/src/notifications/**` | Backend y contrato admiten plataforma `ios`; no hay APNs/FCM iOS ni permiso/token lifecycle nativo. |
| Share/recursos | `composeApp/src/commonMain/composeResources/**`, `composeApp/src/iosMain/...ShareLauncher.ios.kt`, `iosApp/iosApp/Assets.xcassets/**` | Share text iOS existe; visual share iOS pendiente; muchos WebP en recursos Compose; app icon 1024 existe. |
| Políticas/permisos | `composeApp/src/commonMain/kotlin/.../SettingsLinks.kt`, `iosApp/iosApp/Info.plist`, `iosApp/iosApp.xcodeproj/project.pbxproj` | Privacy/Terms tienen TODO de URL real; Info.plist carece de descripciones de permisos; no hay privacy manifest. |

## Estado por área

| Área | Preparación aprox. | Estado | Riesgo | Siguiente acción recomendada |
|---|---:|---|---|---|
| Estructura KMP iOS | 65% | Hay targets iOS y proyecto Xcode; faltan validaciones reales. | Importante antes de TestFlight | Validar en Mac `iosSimulatorArm64` y dispositivo. |
| Firebase Auth email/password | 60% | Cubierto por GitLive en común, pendiente config nativa. | Importante antes de TestFlight | Añadir app iOS en Firebase y `GoogleService-Info.plist`. |
| Firestore | 60% | Cubierto por GitLive en común; reglas/schema ya documentados. | Importante antes de TestFlight | Validar lecturas/escrituras reales iOS. |
| Functions | 60% | Cubierto por GitLive callable client común. | Importante antes de TestFlight | Validar región `europe-west1`, auth y App Check. |
| Storage/avatar | 55% | GitLive Storage común + picker iOS a JPEG temporal. | Importante antes de TestFlight | Añadir permiso de Photo Library y probar subida. |
| Analytics | 20% | Android reflection adapter; iOS registra `NoOpAnalyticsTracker`. | Puede esperar si no es KPI de launch | Implementar Firebase Analytics nativo iOS o aceptar no-op inicial. |
| App Check | 25% | Android tiene SDK nativo; iOS `installAppCheckDebugProvider` es no-op. | Importante si Functions exige App Check | Configurar DeviceCheck/App Attest o debug provider iOS. |
| Remote Config | 0% / no detectado | No se detecta dependencia ni uso. | Solo documentación | No aplica salvo que se añada feature. |
| Crashlytics | 0% / no detectado | No se detecta dependencia ni uso. | Puede esperar post-launch técnico | Añadir si se quiere observabilidad TestFlight. |
| Login Google iOS | 20% | Firebase credential común existe, pero no proveedor iOS de idToken. | Bloqueante si Google login es requisito | Configurar client IDs/URL scheme y proveedor iOS. |
| Apple Sign-In | 0% | No detectado. | Importante si el login social de terceros queda disponible en iOS | Evaluar norma App Store: si Google login se ofrece en iOS, añadir Apple Sign-In. |
| Premium suscripciones iOS | 15% | Contratos comunes existen; iOS launchers devuelven unsupported. | Bloqueante si Premium de pago sale en iOS | Implementar StoreKit + backend App Store receipt/server validation. |
| Moon packs iOS | 15% | Android INAPP existe; iOS unsupported. | Bloqueante si moon packs son core revenue | Definir productos IAP y validación App Store. |
| Push iOS | 20% | Backend reusable con `ios`; UI iOS fuerza permiso false/token null. | Puede esperar si push no es core | Implementar APNs/FCM y capacidades en Xcode. |
| Share text | 70% | `UIActivityViewController` iOS implementado. | Bajo | Probar presenter/safe areas en simulador. |
| Visual share | 20% | iOS devuelve NotImplementedError localizado. | Puede esperar si fallback texto es aceptable | Implementar captura/export iOS cuando haya Mac. |
| Recursos Compose/WebP | 55% | Recursos WebP comunes presentes; requiere prueba iOS real. | Importante antes de TestFlight | Validar carga con Kamel/Compose Resources en iOS. |
| App icon / launch | 50% | AppIcon 1024 presente y launch screen generado por Xcode. | Importante antes de TestFlight | Completar asset catalog y revisar branding. |
| Políticas/privacidad | 25% | Links placeholder/TODO; no privacy manifest. | Bloqueante App Store | Publicar URLs reales, `PrivacyInfo.xcprivacy`, App Privacy. |

## 1. Estado general iOS/KMP

### Cubierto actualmente

- `composeApp` declara `iosArm64()` e `iosSimulatorArm64()` y genera framework estático `ComposeApp`.
- Los módulos compartidos `shared:data`, `shared:domain`, `shared:presentation` y `shared:di` también declaran `iosArm64()` e `iosSimulatorArm64()` con frameworks estáticos.
- El host Swift existe en `iosApp`: `ContentView` integra `MainViewControllerKt.MainViewController()` desde el framework Compose.
- `MainViewController` inicializa Firebase bootstrap y luego monta `App()` en `ComposeUIViewController`.
- DI iOS registra `Darwin.create()` como `HttpClientEngine`, dispatcher, haptics, sound player no-op, analytics no-op y rewarded ads no-op.
- Hay `actual` iOS para plataforma, versión, debug flag, platform context, share text, avatar picker, push bridges no-op/controlados y purchase launchers unsupported.

### Pendiente o riesgoso

- No hay target `iosX64`; esto está bien para Apple Silicon, pero limita simuladores Intel antiguos.
- `BuildFlags.ios` y `BuildInfo.ios` devuelven `true` siempre; en release iOS reportarán debug salvo que se implemente lectura desde build settings o Info.plist.
- `ContentView` usa `.ignoresSafeArea()` globalmente; puede provocar UI bajo notch/home indicator. Debe validarse pantalla por pantalla.
- `Info.plist` es mínimo; faltan permisos como Photo Library si el avatar picker se mantiene.
- `PRODUCT_BUNDLE_IDENTIFIER` usa `com.agc.bwitch.BWitch$(TEAM_ID)`, que no parece un bundle id final válido para App Store.
- `TEAM_ID` está vacío en `Config.xcconfig`.
- El proyecto Xcode invoca `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, pero esto solo se valida en Mac/Xcode.

### Validable sin Mac

- Inventario de `expect/actual` y no-ops.
- Compilación metadata/common si las dependencias Gradle están disponibles.
- Revisión de imports Android-only mediante `rg`.
- Revisión documental de Firebase, push, billing y políticas.

## 2. Firebase iOS

### Cubierto por GitLive/KMP

- **Auth**: `FirebaseAuthRepository` usa `Firebase.auth`, email/password y `GoogleAuthProvider.credential(idToken, null)` en commonMain.
- **Firestore**: repositorios comunes usan `Firebase.firestore` y Koin registra `FirebaseFirestore` común.
- **Functions**: `GitLiveFunctionsClient` usa `Firebase.functions(region)` con región por defecto `europe-west1`.
- **Storage**: `FirebaseAvatarRepository` usa `Firebase.storage`; el file wrapper iOS convierte URI a `NSURL`.

### Requiere configuración nativa iOS

- Crear app iOS en Firebase Console con el bundle id final.
- Añadir `GoogleService-Info.plist` al target `iosApp` en Xcode.
- Inicialización nativa de Firebase si GitLive no lo hace automáticamente con el plist en esta versión; verificar en Mac. El código común llama `FirebaseBootstrapper.init()`, pero el `actual` iOS visible solo deja App Check como no-op.
- Activar proveedores de Auth en Firebase Console: Email/Password, Google si se usará, Apple si se implementa.
- Firestore rules/indexes ya deben estar desplegados en el proyecto Firebase correcto.
- Functions callable deben estar desplegadas en `europe-west1` y admitir clientes iOS.
- Storage rules/bucket deben admitir subida de avatar para el uid autenticado.

### Por producto Firebase

| Producto | Estado iOS | Pendiente Firebase Console / nativo |
|---|---|---|
| Auth email/password | Cubierto por GitLive común | Habilitar proveedor Email/Password; plist; probar sesión persistente en iOS. |
| Auth Google | Parcial | Habilitar Google provider; configurar iOS OAuth client, reversed client ID y URL scheme; implementar proveedor iOS de idToken. |
| Firestore | Cubierto por GitLive común | Validar reglas, índices, offline/persistencia y paths reales con usuario iOS. |
| Functions | Cubierto por GitLive común | Validar región, auth, errores serializados y App Check si se exige. |
| Storage | Cubierto por GitLive común | Validar `NSURL` temporal, permisos de foto y reglas del bucket. |
| Analytics | iOS no-op | Añadir Firebase Analytics iOS o aceptar sin analytics inicial. |
| App Check | iOS no-op | Configurar DeviceCheck/App Attest o debug provider; registrar debug token si emulador/TestFlight. |
| Remote Config | No detectado | No aplica salvo nueva feature. |
| Crashlytics | No detectado | Añadir SDK/configuración si se quiere crash reporting TestFlight/App Store. |
| Messaging/FCM | No detectado en iOS | Requiere APNs key/cert, capability Push Notifications y SDK/bridge iOS. |

### Archivos faltantes o a crear

- `iosApp/iosApp/GoogleService-Info.plist` o ubicación equivalente añadida al target Xcode.
- Posible `iosApp/iosApp/PrivacyInfo.xcprivacy`.
- Configuración de URL schemes en `Info.plist` para Google Sign-In.
- Entitlements/capabilities para Push Notifications, Background Modes si aplica, Sign in with Apple si aplica, App Groups si se añadieran.

## 3. Login iOS

### Qué funcionará sin cambios grandes

- Email/password debería funcionar en iOS con GitLive una vez Firebase iOS esté correctamente configurado y el provider esté habilitado en Console.
- Sign-out común y limpieza local tienen bridge iOS seguro.

### Qué falta para Google Sign-In iOS

- No existe `GoogleIdTokenProvider` iOS equivalente al Android Credential Manager.
- Añadir SDK nativo de Google Sign-In vía SPM o CocoaPods en Xcode.
- Configurar `GoogleService-Info.plist` con `CLIENT_ID` y `REVERSED_CLIENT_ID`.
- Añadir URL scheme con `REVERSED_CLIENT_ID` al Info.plist.
- Implementar bridge iOS que presente el flujo Google y entregue `idToken` al `FirebaseAuthRepository.signInWithGoogleIdToken` común.
- Verificar en Firebase Console que Google provider y OAuth consent estén configurados para iOS.

### Apple Sign-In

- No se detecta implementación actual.
- Si la app iOS ofrece Google Sign-In u otro login social de terceros, **Sign in with Apple normalmente debe ofrecerse también** para revisión App Store. Si el primer TestFlight iOS solo permite email/password, Apple Sign-In puede posponerse, pero conviene planificarlo antes de App Store si Google entra en iOS.

### Puntos Android-only actuales

- `GoogleIdTokenProviderAndroid` usa Credential Manager y Google ID library Android.
- Android platform module registra billing, push, ads y analytics nativos que iOS no registra o registra como no-op/unsupported.

## 4. Premium / compras iOS

### Estado actual

- Existe interfaz multiplataforma `SubscriptionBillingDataSource` con fallback `UnsupportedSubscriptionBillingDataSource`.
- `dataKoinModule` registra el fallback unsupported por defecto y Android lo sobrescribe con `GooglePlaySubscriptionBillingDataSource`.
- iOS `SubscriptionPurchaseLauncher` y `SubscriptionManagementLauncher` devuelven `Unsupported`.
- Existe `MoonPackBillingDataSource` común con fallback unsupported y Android `GooglePlayMoonPackBillingDataSource` para INAPP.
- iOS `MoonPackPurchaseLauncher` devuelve `Unsupported` y `consume=false`.
- Backend actual de compras usa Google Play Android Publisher para validar purchase tokens de moon packs. Premium/entitlements también envían `packageName`, `productId`, `purchaseToken` y `basePlanId` orientados a Google Play.

### Qué hay que implementar para Premium iOS

1. Definir catálogo IAP iOS en App Store Connect:
   - Suscripción mensual Premium.
   - Suscripción anual Premium si se mantiene en Android/negocio.
   - Consumibles moon packs equivalentes a Android si se venderán en iOS.
2. Implementar StoreKit 2 en iOS detrás de los contratos existentes:
   - Query catálogo.
   - Launch purchase.
   - Restore purchases.
   - Transaction updates.
   - Finish transaction cuando backend confirme.
3. Backend:
   - Añadir validación App Store Server API o receipt/transaction validation.
   - Modelar plataforma `ios` en requests de compras/premium.
   - Guardar transactionId/originalTransactionId/webOrderLineItemId según tipo.
   - Soportar renovaciones/cancelaciones con App Store Server Notifications v2 si Premium depende de estado autoritativo.
4. UI:
   - Mostrar precios StoreKit en paywall.
   - Mensaje de restore claro.
   - Enlace de gestión de suscripción iOS.

### ¿Puede salir iOS sin compras?

- **Técnicamente sí**, si el producto acepta un lanzamiento iOS sin Premium pagado/moon packs y se ocultan o degradan los CTAs de compra unsupported.
- **Riesgo de negocio alto** si Premium/moon packs son parte central del lanzamiento.
- **Riesgo App Review** si se intenta vender contenido digital fuera de IAP o se enlaza a compra externa no permitida.

### Restore purchases iOS

- Debe existir antes de App Store si hay suscripciones/IAP iOS.
- Restore debe consultar transacciones StoreKit, validar backend y actualizar `userEntitlements/{uid}` o el modelo autoritativo equivalente.

## 5. Push notifications iOS

### Estado actual

- Android tiene `BwitchFirebaseMessagingService`, manager, channels, synchronizer y bridges de permisos.
- iOS `SettingsPushPermissionBridge` reporta `permissionGranted=false` y `token=null`.
- iOS lifecycle bridge indica push fuera de alcance.
- Backend/data común ya puede registrar/unregister token con `PushPlatform.IOS -> "ios"`.

### Qué falta exactamente

- Capability Push Notifications en Xcode.
- APNs Auth Key/cert en Apple Developer.
- Subir APNs key a Firebase Console si se usa FCM.
- SDK/bridge iOS para solicitar permiso con `UNUserNotificationCenter`.
- Registro APNs y/o FCM token.
- Sincronización de token con `registerPushToken` usando platform `ios`.
- Manejo de refresh del token.
- Unregister en sign-out/borrado de cuenta.
- Routing de notificaciones iOS equivalente a `PushIntentRouter` Android.
- Pruebas de foreground/background/tap.

### ¿Puede quedar fuera del primer lanzamiento iOS?

Sí, si push no es promesa central del producto. Para TestFlight inicial se puede mantener desactivado, pero la UI debe comunicarlo de forma honesta o esconder controles de push en iOS si genera confusión.

## 6. Share / imágenes / recursos iOS

### Cubierto

- `ShareLauncher.ios` usa `UIActivityViewController` para texto.
- Avatar picker iOS abre Photo Library, convierte a JPEG temporal y devuelve URI/mime type.
- App icon asset catalog existe con imagen 1024.
- Compose Resources contiene muchos `.webp` compartidos para tarot/zodiaco/rituales.

### Pendiente

- Visual share de Birth Essence iOS está explícitamente no implementado y devuelve error localizado.
- Validar compatibilidad real de WebP en Compose Multiplatform iOS/Kamel para todos los recursos usados.
- Añadir `NSPhotoLibraryUsageDescription` al Info.plist por el picker de avatar.
- Revisar safe areas porque el host Swift ignora safe area globalmente.
- Revisar launch screen generated y branding final.
- Completar app icons requeridos si Xcode/App Store lo marca; ahora hay al menos 1024, pero debe validarse el asset catalog completo.

## 7. Links, políticas y permisos

### Hallazgos

- `SettingsLinks` mantiene TODO para sustituir Privacy Policy y Terms por URLs reales publicadas.
- `Info.plist` no declara permisos específicos; el avatar picker requiere al menos `NSPhotoLibraryUsageDescription`.
- No se detecta `PrivacyInfo.xcprivacy`.
- Android incluye rewarded ads/AdMob; iOS registra rewarded ads no-op, por lo que ATT puede no aplicar inicialmente si iOS no muestra ads ni tracking cross-app.
- Analytics iOS es no-op, por lo que la declaración de datos en App Store dependerá de Firebase/Auth/Firestore/Storage/Functions y datos de usuario realmente recolectados.

### Pendiente App Store

- Publicar Privacy Policy y Terms.
- Completar App Privacy en App Store Connect según datos: cuenta/auth uid, email si se usa, perfil/avatar, contenido generado/consultas, compras si se implementan, tokens push si se implementan, analytics/crash si se añaden.
- Crear `PrivacyInfo.xcprivacy` si las SDKs o Apple lo requieren.
- Decidir ATT: no pedir si no hay tracking/ads iOS. Si se añade AdMob/ads personalizados o tracking, revisar ATT.

## 8. QA iOS pendiente con Mac/Xcode

### Checklist con Mac/Xcode

1. Abrir `iosApp/iosApp.xcodeproj` en Xcode.
2. Configurar Team/signing en target `iosApp`.
3. Reemplazar `PRODUCT_BUNDLE_IDENTIFIER` por bundle id final, por ejemplo `com.agc.bwitch` o el acordado.
4. Añadir `GoogleService-Info.plist` al target.
5. Verificar si Firebase necesita llamada nativa `FirebaseApp.configure()` en `iOSApp.swift` o si GitLive bootstrap actual basta.
6. Instalar dependencias nativas si se añaden Google Sign-In/Analytics/Messaging/StoreKit helpers por SPM/CocoaPods.
7. Build simulator iPhone SE.
8. Build simulator iPhone Pro.
9. Build simulator iPad.
10. Build device físico.
11. Validar arranque y navegación principal.
12. Validar login email/password: signup, signin, signout, password reset si está expuesto.
13. Validar Google Sign-In iOS cuando esté implementado.
14. Validar Apple Sign-In si se implementa.
15. Validar Firestore: perfil, rituales, horóscopo, tarot/decks, birth chart.
16. Validar Functions: economía, LLM/oráculo, horóscopo unlock, premium entitlement, account deletion.
17. Validar Storage/avatar: picker, upload, download/render.
18. Validar App Check: debug/simulator y dispositivo/TestFlight.
19. Validar Premium StoreKit: catálogo, compra mensual, compra anual, cancelación, expiración, restore.
20. Validar moon packs StoreKit: compra consumible, grant, idempotencia, finish/consume equivalente.
21. Validar restore purchases iOS.
22. Validar push si entra en scope: permiso, token, register/unregister, foreground/background, tap route.
23. Validar visual share o fallback text-only.
24. Validar safe areas: iPhone SE, notch, Dynamic Island, landscape si se soporta, iPad.
25. Validar dark mode si aplica.
26. Validar localizaciones principales.
27. Validar recursos WebP/imágenes en todas las pantallas con decks/zodiaco.
28. Validar audio/haptics tarot; audio iOS actual es no-op.
29. Validar App Store screenshots y metadata.
30. Subir build a TestFlight.
31. Ejecutar smoke test TestFlight externo/interno.

## 9. Riesgos de lanzamiento

### Bloqueante para iOS/App Store

- Falta configuración Firebase iOS y `GoogleService-Info.plist`.
- Bundle id/team/signing no finalizados.
- Privacy Policy/Terms reales y App Privacy incompletos.
- Si se ofrece Google Sign-In en iOS, falta Google Sign-In iOS y probablemente Apple Sign-In para cumplir revisión.
- Si Premium/moon packs son parte del lanzamiento, falta StoreKit y validación App Store backend.
- Permiso Photo Library falta en Info.plist si avatar picker se conserva.

### Importante antes de TestFlight

- Validar compilación iOS en Mac.
- Validar safe areas por `.ignoresSafeArea()` global.
- Validar App Check iOS si backend requiere tokens.
- Validar Firestore/Functions/Storage reales.
- Validar WebP/Kamel/Compose Resources iOS.
- Definir si ocultar push/compras/visual share unsupported en iOS inicial.

### Puede esperar post-launch

- Push iOS si no es core.
- Firebase Analytics iOS si el lanzamiento inicial no requiere KPIs completos.
- Crashlytics iOS, aunque recomendable para TestFlight.
- Audio tarot iOS real si haptics/fallback son aceptables.
- Visual share iOS si share text cumple MVP.

### Solo documentación/operación

- Mantener este checklist actualizado.
- Registrar decisiones de scope: sin compras, sin push o sin Google Sign-In en primer TestFlight si se decide.
- Documentar productos IAP y mapping de producto Android/iOS cuando se definan.

## 10. Checklist sin Mac

- [x] Crear rama `feature/ios-launch-preparation`.
- [x] Auditar estructura `iosMain` y `iosApp`.
- [x] Auditar `expect/actual`, no-ops y unsupported iOS.
- [x] Auditar Firebase GitLive/commonMain vs configuración nativa pendiente.
- [x] Auditar login email/password, Google y Apple.
- [x] Auditar billing Android, contratos multiplataforma e iOS unsupported.
- [x] Auditar push Android/backend reusable e iOS no-op.
- [x] Auditar share text, visual share, recursos y app icon.
- [x] Auditar policies/permisos/privacy manifest.
- [x] Intentar validación Gradle sin Mac.
- [ ] Reintentar Gradle cuando dependencias estén disponibles en cache o red.
- [ ] Añadir documentación de decisión de scope si se decide lanzar TestFlight sin compras/push/Google.

## 11. Comandos ejecutables sin Mac

> Nota: en esta auditoría, el intento Gradle falló antes de ejecutar tareas por resolución de plugin AGP (`com.android.application:8.7.3`) no disponible en repos/cache del entorno.

```bash
git checkout -B feature/ios-launch-preparation
find composeApp/src/iosMain -type f -print | sort
find shared -path '*/iosMain/*' -type f -print | sort
find iosApp -type f -maxdepth 4 -print | sort
rg -n "expect fun|expect class|expect object|actual fun|actual class|actual object|NoOp|Unsupported|TODO" composeApp/src shared -g '*.kt' -g '!**/build/**'
rg -n "Firebase|GoogleAuthProvider|firestore|functions|storage|AppCheck|Analytics|RemoteConfig|Crashlytics|Messaging" shared composeApp iosApp gradle -g '*.*' -g '!**/build/**'
rg -n "Billing|StoreKit|Purchase|SubscriptionBillingDataSource|MoonPackBillingDataSource|GooglePlay|receipt|purchaseToken" shared composeApp functions docs -g '*.*' -g '!**/build/**'
rg -n "Push|Notification|APNs|FCM|registerPushToken|unregisterPushToken|PushPlatform" shared composeApp functions docs -g '*.*' -g '!**/build/**'
./gradlew :composeApp:compileKotlinIosSimulatorArm64 :shared:data:compileKotlinIosSimulatorArm64 :shared:presentation:compileKotlinIosSimulatorArm64 :shared:domain:compileKotlinIosSimulatorArm64 :shared:di:compileKotlinIosSimulatorArm64 --dry-run
./gradlew :shared:data:compileCommonMainKotlinMetadata :shared:presentation:compileCommonMainKotlinMetadata :shared:domain:compileCommonMainKotlinMetadata :shared:di:compileCommonMainKotlinMetadata
```

## 12. Recomendaciones de orden de trabajo

1. **Mac bootstrap mínimo**: signing, bundle id, plist, build simulator/device.
2. **Firebase iOS base**: Auth email/password, Firestore, Functions, Storage/avatar, App Check debug/device.
3. **Scope de lanzamiento**: decidir explícitamente si primer TestFlight va sin compras, sin push, sin Google Sign-In y sin visual share.
4. **Privacidad/App Store**: Privacy Policy, Terms, Info.plist permissions, privacy manifest/App Privacy.
5. **Login social**: implementar Google Sign-In iOS y Apple Sign-In si se quiere social login en App Store.
6. **StoreKit + backend**: suscripciones, moon packs, restore y App Store Server validation.
7. **Push iOS**: APNs/FCM, token lifecycle, routing y preferencias.
8. **QA visual**: safe areas, iPad, dark mode, recursos WebP, screenshots.
9. **Observabilidad**: Crashlytics/Analytics iOS si se necesita antes de TestFlight amplio.

## 13. Prompts futuros sugeridos

### StoreKit / Premium iOS

```text
Estamos en la rama de iOS. Implementa StoreKit 2 para iOS detrás de SubscriptionBillingDataSource y MoonPackBillingDataSource sin cambiar economía. Mantén Android intacto. Primero audita productos actuales Android y contratos; después añade actual/bridge iOS mínimo, restore purchases y documentación. No cambies backend salvo crear un plan detallado de App Store Server API si no cabe en la pasada.
```

### Backend App Store validation

```text
Audita el backend de compras actual Google Play y diseña/implementa soporte iOS con App Store Server API para suscripciones y consumibles. No cambies economía. Añade validación idempotente por transactionId/originalTransactionId, plataforma ios, y actualiza docs de Firestore/API si cambian contratos.
```

### Push iOS

```text
Implementa push iOS en BWitch sin tocar Android: permisos UNUserNotificationCenter, APNs/FCM token lifecycle, register/unregister con PushPlatform.IOS, routing básico y documentación de capabilities/APNs. Mantén fallback seguro si Firebase Messaging iOS no está configurado.
```

### Google Sign-In + Apple Sign-In iOS

```text
Implementa login social iOS: Google Sign-In mediante SDK nativo y bridge de idToken hacia FirebaseAuthRepository, y evalúa/añade Sign in with Apple si el login Google queda disponible para App Store. Actualiza Info.plist URL schemes, docs de Firebase Console y checklist de QA.
```

### Privacy/App Store readiness

```text
Prepara App Store readiness iOS: Info.plist permissions, PrivacyInfo.xcprivacy, privacy/data collection checklist, Terms/Privacy links reales y screenshots checklist. No cambies lógica de negocio.
```
