package com.agc.bwitch.data.userprofile

import com.agc.bwitch.data.storage.storageFileFromUri
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.userprofile.AvatarRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

class FirebaseAvatarRepository(
    private val authRepository: AuthRepository
) : AvatarRepository {

    private val storage = Firebase.storage

    override suspend fun uploadAvatar(
        fileUri: String,
        mimeType: String?,
        previousUrl: String?
    ): String {
        val uid = currentUidOrNull() ?: error("No hay sesión")

        val ext = when (mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        val version = Clock.System.now().toEpochMilliseconds()
        val newPath = "users/$uid/profile/avatar_$version.$ext"
        val ref = storage.reference.child(newPath)

        val file = storageFileFromUri(fileUri)
        ref.putFile(file)

        val newUrl = ref.getDownloadUrl()

        // Cleanup best-effort del avatar anterior (si es nuestro)
        runCatching { deletePreviousAvatarIfSafe(uid, previousUrl) }

        return newUrl
    }

    private suspend fun deletePreviousAvatarIfSafe(uid: String, previousUrl: String?) {
        val path = previousUrl?.let(::extractStoragePathFromDownloadUrl) ?: return

        // Seguridad: solo borrar si cuadra exactamente con nuestro patrón de avatar
        val allowedPrefix = "users/$uid/profile/avatar_"
        if (!path.startsWith(allowedPrefix)) return

        // Opcional: evita borrar el mismo path si por lo que sea coincide
        // (normalmente no coincidirá porque ahora versionamos)
        storage.reference.child(path).delete()
    }

    /**
     * Extrae el path "users/<uid>/profile/...." desde una downloadUrl típica:
     * .../o/<PATH_ENCODED>?alt=media&token=...
     */
    private fun extractStoragePathFromDownloadUrl(url: String): String? {
        val marker = "/o/"
        val start = url.indexOf(marker)
        if (start < 0) return null
        val from = start + marker.length
        val to = url.indexOf('?', from).let { if (it < 0) url.length else it }
        val encoded = url.substring(from, to)
        return percentDecode(encoded)
    }

    // Percent-decode simple (KMP-safe)
    private fun percentDecode(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hex = s.substring(i + 1, i + 3)
                val v = hex.toIntOrNull(16)
                if (v != null) {
                    out.append(v.toChar())
                    i += 3
                    continue
                }
            }
            // Firebase usa %2F para '/', esto lo cubre arriba.
            // Si aparece '+' no lo convertimos a espacio (en URL path no debería).
            out.append(c)
            i++
        }
        return out.toString()
    }

    private suspend fun currentUidOrNull(): String? =
        withTimeoutOrNull(2_000) {
            authRepository.authState
                .filterNotNull()
                .first()
                .uid
        }
}