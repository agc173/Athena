package com.agc.bwitch.localization

import com.agc.bwitch.domain.localization.AppLanguage

data class AppStrings(
    val auth: AuthStrings,
    val onboarding: OnboardingStrings,
    val settings: SettingsStrings,
    val navigation: NavigationStrings,
    val common: CommonStrings,
)

data class AuthStrings(
    val subtitle: String,
    val continueWithGoogle: String,
    val emailLabel: String,
    val passwordLabel: String,
    val signIn: String,
    val signUp: String,
    val connecting: String,
    val googleSignInErrorFallback: String,
)

data class OnboardingStrings(
    val profileTitle: String,
    val profileSubtitle: String,
    val profileAvatarContentDescription: String,
    val usernameLabel: String,
    val usernameError: String,
    val birthDateLabel: String,
    val birthDatePlaceholder: String,
    val birthDateFormatError: String,
    val continueButton: String,
)

data class SettingsStrings(
    val subtitle: String,
    val refreshProfile: String,
    val signOut: String,
)

data class NavigationStrings(
    val onboardingProfile: String,
    val astrology: String,
    val birthChart: String,
    val synastry: String,
    val profile: String,
    val settings: String,
    val oracle: String,
    val oracleDebug: String,
    val guide: String,
    val rituals: String,
    val ritual: String,
    val dailyRitual: String,
    val habits: String,
    val tarot: String,
    val pendulum: String,
    val horoscopeDaily: String,
)

data class CommonStrings(
    val languageSectionTitle: String,
    val languageSectionSubtitle: String,
    val languageSelectedPrefix: String,
)

fun resolveAppStrings(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.Spanish -> fallbackAppStrings
    AppLanguage.English -> englishAppStrings
    AppLanguage.Portuguese -> portugueseAppStrings
    AppLanguage.Russian -> russianAppStrings
    AppLanguage.French -> frenchAppStrings
    AppLanguage.Italian -> italianAppStrings
    AppLanguage.German -> germanAppStrings
}

val fallbackAppStrings: AppStrings = AppStrings(
    auth = AuthStrings(
        subtitle = "Accede para continuar tu camino mágico",
        continueWithGoogle = "Continuar con Google",
        emailLabel = "Correo electrónico",
        passwordLabel = "Contraseña",
        signIn = "Iniciar sesión",
        signUp = "Crear cuenta",
        connecting = "Conectando…",
        googleSignInErrorFallback = "No se pudo iniciar sesión con Google",
    ),
    onboarding = OnboardingStrings(
        profileTitle = "Completa tu perfil",
        profileSubtitle = "Necesitamos algunos datos para personalizar tu experiencia.",
        profileAvatarContentDescription = "Avatar del perfil",
        usernameLabel = "Nombre de usuario",
        usernameError = "Usa entre 3 y 20 caracteres: letras, números, punto o guion bajo.",
        birthDateLabel = "Fecha de nacimiento",
        birthDatePlaceholder = "AAAA-MM-DD",
        birthDateFormatError = "Formato inválido. Usa AAAA-MM-DD.",
        continueButton = "Continuar",
    ),
    settings = SettingsStrings(
        subtitle = "Gestiona tu perfil y preferencias",
        refreshProfile = "Actualizar perfil",
        signOut = "Cerrar sesión",
    ),
    navigation = NavigationStrings(
        onboardingProfile = "Completa tu perfil",
        astrology = "Astrología",
        birthChart = "Esencia natal",
        synastry = "Sinastría",
        profile = "Perfil",
        settings = "Ajustes",
        oracle = "Oráculo",
        oracleDebug = "Oracle debug",
        guide = "Guía",
        rituals = "Rituales",
        ritual = "Ritual",
        dailyRitual = "Ritual del día",
        habits = "Hábitos",
        tarot = "Tarot",
        pendulum = "El Péndulo",
        horoscopeDaily = "Horóscopo diario",
    ),
    common = CommonStrings(
        languageSectionTitle = "Idioma de la app",
        languageSectionSubtitle = "Este cambio se aplicará en próximas pantallas migradas.",
        languageSelectedPrefix = "✓ ",
    ),
)

private val englishAppStrings = fallbackAppStrings.copy(
    auth = fallbackAppStrings.auth.copy(
        subtitle = "Sign in to continue your magical path",
        continueWithGoogle = "Continue with Google",
        emailLabel = "Email",
        passwordLabel = "Password",
        signIn = "Sign in",
        signUp = "Create account",
        connecting = "Connecting…",
        googleSignInErrorFallback = "Google sign-in failed",
    ),
    onboarding = fallbackAppStrings.onboarding.copy(
        profileTitle = "Complete your profile",
        profileSubtitle = "We need a few details to personalize your experience.",
        profileAvatarContentDescription = "Profile avatar",
        usernameLabel = "Username",
        usernameError = "Use 3 to 20 characters: letters, numbers, dot or underscore.",
        birthDateLabel = "Birth date",
        birthDatePlaceholder = "YYYY-MM-DD",
        birthDateFormatError = "Invalid format. Use YYYY-MM-DD.",
        continueButton = "Continue",
    ),
    settings = fallbackAppStrings.settings.copy(
        subtitle = "Manage your profile and preferences",
        refreshProfile = "Refresh profile",
        signOut = "Sign out",
    ),
    navigation = fallbackAppStrings.navigation.copy(
        onboardingProfile = "Complete your profile",
        astrology = "Astrology",
        birthChart = "Birth essence",
        synastry = "Synastry",
        profile = "Profile",
        settings = "Settings",
        oracle = "Oracle",
        guide = "Guide",
        rituals = "Rituals",
        ritual = "Ritual",
        dailyRitual = "Daily ritual",
        habits = "Habits",
        pendulum = "Pendulum",
        horoscopeDaily = "Daily horoscope",
    ),
    common = fallbackAppStrings.common.copy(
        languageSectionTitle = "App language",
        languageSectionSubtitle = "This change will apply to migrated screens.",
    ),
)

private val portugueseAppStrings = fallbackAppStrings.copy(
    auth = fallbackAppStrings.auth.copy(
        subtitle = "Entre para continuar seu caminho mágico",
        continueWithGoogle = "Continuar com Google",
        emailLabel = "E-mail",
        passwordLabel = "Senha",
        signIn = "Entrar",
        signUp = "Criar conta",
        connecting = "Conectando…",
        googleSignInErrorFallback = "Não foi possível entrar com o Google",
    ),
    onboarding = fallbackAppStrings.onboarding.copy(
        profileTitle = "Complete seu perfil",
        profileSubtitle = "Precisamos de alguns dados para personalizar sua experiência.",
        profileAvatarContentDescription = "Avatar do perfil",
        usernameLabel = "Nome de usuário",
        usernameError = "Use de 3 a 20 caracteres: letras, números, ponto ou sublinhado.",
        birthDateLabel = "Data de nascimento",
        birthDatePlaceholder = "AAAA-MM-DD",
        birthDateFormatError = "Formato inválido. Use AAAA-MM-DD.",
        continueButton = "Continuar",
    ),
    settings = fallbackAppStrings.settings.copy(
        subtitle = "Gerencie seu perfil e preferências",
        refreshProfile = "Atualizar perfil",
        signOut = "Sair",
    ),
    navigation = fallbackAppStrings.navigation.copy(
        onboardingProfile = "Complete seu perfil",
        astrology = "Astrologia",
        birthChart = "Essência natal",
        synastry = "Sinastria",
        profile = "Perfil",
        settings = "Configurações",
        oracle = "Oráculo",
        guide = "Guia",
        rituals = "Rituais",
        dailyRitual = "Ritual do dia",
        habits = "Hábitos",
        pendulum = "Pêndulo",
        horoscopeDaily = "Horóscopo diário",
    ),
    common = fallbackAppStrings.common.copy(
        languageSectionTitle = "Idioma do app",
        languageSectionSubtitle = "Esta mudança será aplicada nas telas migradas.",
    ),
)

private val russianAppStrings = fallbackAppStrings.copy(
    auth = fallbackAppStrings.auth.copy(
        subtitle = "Войдите, чтобы продолжить ваш магический путь",
        continueWithGoogle = "Продолжить с Google",
        emailLabel = "Эл. почта",
        passwordLabel = "Пароль",
        signIn = "Войти",
        signUp = "Создать аккаунт",
        connecting = "Подключение…",
        googleSignInErrorFallback = "Не удалось войти через Google",
    ),
    onboarding = fallbackAppStrings.onboarding.copy(
        profileTitle = "Заполните профиль",
        profileSubtitle = "Нам нужны некоторые данные для персонализации.",
        profileAvatarContentDescription = "Аватар профиля",
        usernameLabel = "Имя пользователя",
        usernameError = "Используйте 3–20 символов: буквы, цифры, точка или подчеркивание.",
        birthDateLabel = "Дата рождения",
        birthDatePlaceholder = "ГГГГ-ММ-ДД",
        birthDateFormatError = "Неверный формат. Используйте ГГГГ-ММ-ДД.",
        continueButton = "Продолжить",
    ),
    settings = fallbackAppStrings.settings.copy(
        subtitle = "Управляйте профилем и настройками",
        refreshProfile = "Обновить профиль",
        signOut = "Выйти",
    ),
    navigation = fallbackAppStrings.navigation.copy(
        onboardingProfile = "Заполните профиль",
        astrology = "Астрология",
        birthChart = "Натальная сущность",
        synastry = "Синастрия",
        profile = "Профиль",
        settings = "Настройки",
        oracle = "Оракул",
        guide = "Гид",
        rituals = "Ритуалы",
        dailyRitual = "Ритуал дня",
        habits = "Привычки",
        pendulum = "Маятник",
        horoscopeDaily = "Ежедневный гороскоп",
    ),
    common = fallbackAppStrings.common.copy(
        languageSectionTitle = "Язык приложения",
        languageSectionSubtitle = "Изменение применится к уже мигрированным экранам.",
    ),
)

private val frenchAppStrings = fallbackAppStrings.copy(
    auth = fallbackAppStrings.auth.copy(
        subtitle = "Connecte-toi pour poursuivre ton chemin magique",
        continueWithGoogle = "Continuer avec Google",
        emailLabel = "E-mail",
        passwordLabel = "Mot de passe",
        signIn = "Se connecter",
        signUp = "Créer un compte",
        connecting = "Connexion…",
        googleSignInErrorFallback = "Impossible de se connecter avec Google",
    ),
    onboarding = fallbackAppStrings.onboarding.copy(
        profileTitle = "Complète ton profil",
        profileSubtitle = "Nous avons besoin de quelques infos pour personnaliser ton expérience.",
        profileAvatarContentDescription = "Avatar du profil",
        usernameLabel = "Nom d'utilisateur",
        usernameError = "Utilise 3 à 20 caractères : lettres, chiffres, point ou underscore.",
        birthDateLabel = "Date de naissance",
        birthDatePlaceholder = "AAAA-MM-JJ",
        birthDateFormatError = "Format invalide. Utilise AAAA-MM-JJ.",
        continueButton = "Continuer",
    ),
    settings = fallbackAppStrings.settings.copy(
        subtitle = "Gère ton profil et tes préférences",
        refreshProfile = "Actualiser le profil",
        signOut = "Se déconnecter",
    ),
    navigation = fallbackAppStrings.navigation.copy(
        onboardingProfile = "Complète ton profil",
        astrology = "Astrologie",
        birthChart = "Essence natale",
        synastry = "Synastrie",
        profile = "Profil",
        settings = "Paramètres",
        oracle = "Oracle",
        guide = "Guide",
        rituals = "Rituels",
        dailyRitual = "Rituel du jour",
        habits = "Habitudes",
        pendulum = "Pendule",
        horoscopeDaily = "Horoscope du jour",
    ),
    common = fallbackAppStrings.common.copy(
        languageSectionTitle = "Langue de l'application",
        languageSectionSubtitle = "Ce changement s'appliquera aux écrans migrés.",
    ),
)

private val italianAppStrings = fallbackAppStrings.copy(
    auth = fallbackAppStrings.auth.copy(
        subtitle = "Accedi per continuare il tuo percorso magico",
        continueWithGoogle = "Continua con Google",
        emailLabel = "Email",
        passwordLabel = "Password",
        signIn = "Accedi",
        signUp = "Crea account",
        connecting = "Connessione…",
        googleSignInErrorFallback = "Accesso con Google non riuscito",
    ),
    onboarding = fallbackAppStrings.onboarding.copy(
        profileTitle = "Completa il tuo profilo",
        profileSubtitle = "Ci servono alcuni dati per personalizzare la tua esperienza.",
        profileAvatarContentDescription = "Avatar del profilo",
        usernameLabel = "Nome utente",
        usernameError = "Usa da 3 a 20 caratteri: lettere, numeri, punto o underscore.",
        birthDateLabel = "Data di nascita",
        birthDatePlaceholder = "AAAA-MM-GG",
        birthDateFormatError = "Formato non valido. Usa AAAA-MM-GG.",
        continueButton = "Continua",
    ),
    settings = fallbackAppStrings.settings.copy(
        subtitle = "Gestisci il tuo profilo e le preferenze",
        refreshProfile = "Aggiorna profilo",
        signOut = "Disconnetti",
    ),
    navigation = fallbackAppStrings.navigation.copy(
        onboardingProfile = "Completa il tuo profilo",
        astrology = "Astrologia",
        birthChart = "Essenza natale",
        synastry = "Sinastria",
        profile = "Profilo",
        settings = "Impostazioni",
        oracle = "Oracolo",
        guide = "Guida",
        rituals = "Rituali",
        dailyRitual = "Rituale del giorno",
        habits = "Abitudini",
        pendulum = "Pendolo",
        horoscopeDaily = "Oroscopo giornaliero",
    ),
    common = fallbackAppStrings.common.copy(
        languageSectionTitle = "Lingua dell'app",
        languageSectionSubtitle = "La modifica sarà applicata alle schermate migrate.",
    ),
)

private val germanAppStrings = fallbackAppStrings.copy(
    auth = fallbackAppStrings.auth.copy(
        subtitle = "Melde dich an, um deinen magischen Weg fortzusetzen",
        continueWithGoogle = "Mit Google fortfahren",
        emailLabel = "E-Mail",
        passwordLabel = "Passwort",
        signIn = "Anmelden",
        signUp = "Konto erstellen",
        connecting = "Verbinden…",
        googleSignInErrorFallback = "Google-Anmeldung fehlgeschlagen",
    ),
    onboarding = fallbackAppStrings.onboarding.copy(
        profileTitle = "Vervollständige dein Profil",
        profileSubtitle = "Wir benötigen einige Daten, um dein Erlebnis zu personalisieren.",
        profileAvatarContentDescription = "Profilbild",
        usernameLabel = "Benutzername",
        usernameError = "Verwende 3 bis 20 Zeichen: Buchstaben, Zahlen, Punkt oder Unterstrich.",
        birthDateLabel = "Geburtsdatum",
        birthDatePlaceholder = "JJJJ-MM-TT",
        birthDateFormatError = "Ungültiges Format. Nutze JJJJ-MM-TT.",
        continueButton = "Weiter",
    ),
    settings = fallbackAppStrings.settings.copy(
        subtitle = "Verwalte dein Profil und deine Einstellungen",
        refreshProfile = "Profil aktualisieren",
        signOut = "Abmelden",
    ),
    navigation = fallbackAppStrings.navigation.copy(
        onboardingProfile = "Vervollständige dein Profil",
        astrology = "Astrologie",
        birthChart = "Geburtsessenz",
        synastry = "Synastrie",
        profile = "Profil",
        settings = "Einstellungen",
        oracle = "Orakel",
        guide = "Leitfaden",
        rituals = "Rituale",
        dailyRitual = "Tagesritual",
        habits = "Gewohnheiten",
        pendulum = "Pendel",
        horoscopeDaily = "Tageshoroskop",
    ),
    common = fallbackAppStrings.common.copy(
        languageSectionTitle = "App-Sprache",
        languageSectionSubtitle = "Diese Änderung gilt für bereits migrierte Screens.",
    ),
)
