package com.agc.bwitch.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

/**
 * Boundary mínimo y seguro para la fase actual:
 * - Recompone el árbol cuando cambia el idioma seleccionado por BWitch.
 * - No muta locale global de plataforma desde composición.
 *
 * Nota: esto por sí solo no fuerza que `stringResource(...)` ignore el locale del sistema.
 * Mantiene una base honesta hasta definir una estrategia robusta de runtime locale en Compose MPP.
 */
@Composable
fun AppLocaleEnvironment(
    languageCode: String,
    content: @Composable () -> Unit,
) {
    key(languageCode) {
        content()
    }
}
