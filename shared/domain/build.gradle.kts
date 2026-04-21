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
            baseName = "domain"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Domain debe ser lo más puro posible.
            // Si necesitas fechas en modelos, puedes añadir:
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)


        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.agc.bwitch.shared.domain"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = true
    }
}

// ---------------------------------------------------------------------------------------------
// LINT WORKAROUND (KMP metadata incompatibility in Android debug lint)
//
// Scope:
// - Temporarily disable debug lint entry-points in this module only, including unit/androidTest variants.
// - Keep release lint policy enabled (`checkReleaseBuilds = true`).
// ---------------------------------------------------------------------------------------------
tasks.matching {
    it.name in setOf(
        "lintAnalyzeDebug",
        "lintAnalyzeDebugUnitTest",
        "lintAnalyzeDebugAndroidTest",
        "lintDebug",
        "lintDebugUnitTest",
        "lintDebugAndroidTest",
    )
}.configureEach {
    enabled = false
}
