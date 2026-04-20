package com.agc.bwitch.localization

import androidx.compose.runtime.Composable

/**
 * Boundary para cambios de idioma en runtime.
 *
 * Importante: no debe recrear el árbol completo de composición, porque eso
 * puede reinicializar estado local en niveles altos (p.ej. AppRoot) y disparar
 * efectos colaterales de navegación.
 */
@Composable
fun AppLocaleEnvironment(
    languageCode: String,
    content: @Composable () -> Unit,
) {
    // `languageCode` se conserva en la firma para mantener el contrato actual.
    // Los textos se actualizan por recomposición vía LocalAppStrings.
    content()
}
