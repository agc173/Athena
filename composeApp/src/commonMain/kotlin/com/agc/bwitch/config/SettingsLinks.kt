package com.agc.bwitch.config

/**
 * Destinos externos para acciones de Ajustes (soporte/legal).
 *
 * Reemplazar en producto real:
 * - [supportEmail]
 * - [privacyPolicyUrl]
 * - [termsAndConditionsUrl]
 */
object SettingsLinks {
    const val supportEmail: String = "support@bwitch.app"

    // TODO(product): Sustituir por la URL real publicada de Privacy Policy.
    const val privacyPolicyUrl: String = "https://bwitch.app/privacy"

    // TODO(product): Sustituir por la URL real publicada de Terms & Conditions.
    const val termsAndConditionsUrl: String = "https://bwitch.app/terms"

    fun contactSupportMailto(): String = "mailto:$supportEmail"

    fun reportIssueMailto(): String =
        "mailto:$supportEmail?subject=BWitch%20Issue%20Report&body=Please%20describe%20the%20issue%20you%20found."
}
