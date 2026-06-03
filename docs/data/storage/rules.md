# Firebase Storage Rules

Este documento versiona el contrato de seguridad de Firebase Storage para BWitch. Las reglas productivas viven en `storage.rules` y se referencian desde `firebase.json`.

## Estado actual

- Modelo de seguridad: **deny-by-default** para cualquier ruta no declarada.
- Scope versionado: avatares de perfil de usuario.
- No se despliega automáticamente desde el repo; el despliegue debe ejecutarse manualmente y validarse antes de closed testing.

## Ruta permitida para avatares

```text
users/{uid}/profile/avatar_<epoch>.<ext>
```

Donde:

- `{uid}` debe coincidir exactamente con `request.auth.uid`.
- `<epoch>` debe ser numérico (`[0-9]+`) y corresponde al timestamp en milisegundos que genera `FirebaseAvatarRepository` con `Clock.System.now().toEpochMilliseconds()`.
- `<ext>` puede ser `jpg`, `jpeg`, `png` o `webp`.

Ejemplos válidos de nombre:

- `users/A/profile/avatar_123.jpg`
- `users/A/profile/avatar_1717350000000.jpeg`
- `users/A/profile/avatar_1717350000000.png`
- `users/A/profile/avatar_1717350000000.webp`

Ejemplos rechazados:

- `users/A/profile/random.jpg`
- `users/A/profile/avatar_abc.jpg`
- `users/A/profile/avatar_123.txt`
- `users/A/profile/avatar_123.gif`
- `users/A/private/avatar_123.jpg`

## Permisos

| Operación | Condición |
| --- | --- |
| `read` | Usuario autenticado, owner del `{uid}` y nombre de archivo de avatar válido. |
| `create` / `update` | Usuario autenticado, owner del `{uid}`, nombre válido, `contentType` permitido y tamaño máximo de 5 MB. |
| `delete` | Usuario autenticado, owner del `{uid}` y nombre de archivo de avatar válido. |
| Cualquier otra ruta/operación | Denegada. |

## Validaciones de escritura

Las escrituras de avatar (`create` y `update`) solo se permiten cuando `request.resource` cumple:

- `request.resource.contentType` ∈ `image/jpeg`, `image/png`, `image/webp`.
- `request.resource.size <= 5 MB` (`5 * 1024 * 1024` bytes).
- `fileName.matches('^avatar_[0-9]+\\.(jpg|jpeg|png|webp)$')`.

No se acepta `image/jpg`. El MIME canónico para JPEG es `image/jpeg`; Android obtiene el MIME desde `ContentResolver.getType(uri)` y normalmente devuelve `image/jpeg` para JPEG, mientras que iOS convierte la imagen temporal a JPEG y reporta explícitamente `image/jpeg`. Si un proveedor Android concreto devuelve `image/jpg`, la subida será rechazada por reglas y deberá normalizarse en cliente o tratarse como bug antes de ampliar reglas.

## Compatibilidad con el flujo actual de avatar

El flujo actual es compatible con estas reglas sin cambiar rutas:

- `FirebaseAvatarRepository` obtiene el usuario autenticado y sube a `users/{uid}/profile/avatar_<timestamp>.<ext>`.
- La extensión se calcula desde el MIME: `image/png` → `png`, `image/webp` → `webp`, resto → `jpg`.
- Android abre `ActivityResultContracts.GetContent()` con `image/*` y pasa el MIME de `ContentResolver.getType(uri)`.
- iOS toma una imagen de Photo Library, la convierte a JPEG temporal y pasa `image/jpeg`.
- El borrado best-effort del avatar anterior solo intenta borrar paths con prefijo `users/{uid}/profile/avatar_`; las reglas añaden owner-check y validación de nombre.

Riesgo operativo conocido: la subida usa `putFile(file)` sin metadata explícita en el repositorio. Las reglas dependen de que Firebase Storage reciba un `contentType` permitido. Validar en dispositivo/emulador que los objetos subidos conservan `image/jpeg`, `image/png` o `image/webp`.

## Pruebas manuales recomendadas

Validar en Firebase Rules Playground o Emulator Suite:

1. UID `A` puede crear `users/A/profile/avatar_123.jpg` con `contentType=image/jpeg` y tamaño `< 5 MB`.
2. UID `A` no puede crear `users/B/profile/avatar_123.jpg`.
3. UID `A` no puede subir `users/A/profile/avatar_123.txt` aunque el MIME sea de imagen.
4. UID `A` no puede subir `users/A/profile/avatar_123.jpg` con `contentType=image/jpeg` y tamaño `> 5 MB`.
5. UID `A` no puede escribir `users/A/profile/random.jpg`.
6. UID `A` puede leer y borrar `users/A/profile/avatar_123.jpg`.
7. UID `B` no puede leer ni borrar `users/A/profile/avatar_123.jpg`.
8. Usuario no autenticado no puede leer, crear, actualizar ni borrar ningún avatar.

## Validación local

Comandos sugeridos desde la raíz del repo:

```bash
# Si tu versión de Firebase CLI soporta dry-run:
firebase deploy --only storage --dry-run

# Validación local con emuladores:
firebase emulators:start --only storage,auth
```

Si la CLI no está instalada globalmente, usar una instalación local/temporal de Firebase CLI y repetir los comandos anteriores. El emulador de Storage está configurado en `firebase.json` en el puerto `9199`.

## Deploy manual

1. Revisar diff de `storage.rules`, `firebase.json` y esta documentación.
2. Ejecutar dry-run si tu versión de Firebase CLI lo soporta:
   ```bash
   firebase deploy --only storage --dry-run
   ```
3. Si la CLI no soporta `--dry-run`, validar las reglas con Emulator Suite y/o Rules Playground antes de desplegar.
4. Validar las pruebas manuales en Rules Playground o Emulator Suite.
5. Desplegar manualmente:
   ```bash
   firebase deploy --only storage
   ```
6. Confirmar en Firebase Console que el bucket activo usa las reglas versionadas.
7. Probar subida, lectura de avatar y borrado de avatar previo con una cuenta real de testing.

## Relación con Firestore

Estas reglas solo protegen objetos de Firebase Storage. La URL pública/descargable del avatar se persiste en Firestore dentro del perfil de usuario y sigue regida por `firestore.rules` y la documentación de `docs/data/firestore/rules.md`.
