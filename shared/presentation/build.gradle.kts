import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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
            baseName = "presentation"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)



            // Coroutines/Flow (si lo tienes en catálogo)
            // implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.agc.bwitch.shared.presentation"
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
// - Temporarily disable debug lint entry-points in this module only.
// - Keep release lint policy enabled (`checkReleaseBuilds = true`).
// ---------------------------------------------------------------------------------------------
tasks.matching {
    it.name == "lintAnalyzeDebug" ||
            it.name == "lintDebug"
}.configureEach {
    enabled = false
}
