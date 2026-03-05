import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}


kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "data"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))

            implementation(libs.koin.core)

            // Networking / JSON
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Firebase KMP (GitLive) si tus libs.firebase.* ya son KMP:
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.functions)

            // Coroutines si lo tienes en el catalog:
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.firebase.storage)


        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.agc.bwitch.shared.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = true
    }
}
// ---------------------------------------------------------------------------------------------
// LINT WORKAROUND (Compose Multiplatform + AGP bug)
//
// Context:
// Android Lint crashes with "Unexpected failure during lint analysis"
// when analyzing KMP expect/actual code (e.g., SettingsFactory.android.kt).
//
// This is a known issue in Android Gradle Plugin / Lint with Kotlin Multiplatform.
//
// We disable only the crashing analysis tasks for this module to keep builds stable,
// while preserving lint functionality in other modules.
//
// Safe because:
// - Does not affect runtime
// - Does not affect release builds
// - Only skips lint analysis for this module
//
// Remove when upgrading AGP/Kotlin if lint becomes stable.
// ---------------------------------------------------------------------------------------------

tasks.matching {
    it.name == "lintAnalyzeDebug" ||
            it.name == "lintDebug"
}.configureEach {
    enabled = false
}