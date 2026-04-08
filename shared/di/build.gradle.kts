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
            baseName = "di"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(project(":shared:data"))
            implementation(project(":shared:presentation"))

            implementation(libs.koin.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.agc.bwitch.shared.di"
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
// LINT WORKAROUND (KMP + Android Lint instability)
//
// Why this exists:
// - This module can hit lint analysis crashes on KMP code paths in debug analysis.
//
// Scope:
// - Limited to debug lint tasks in this module.
// - Release lint checks remain enabled via `checkReleaseBuilds = true`.
//
// Revisit plan:
// - Re-test on AGP/Kotlin update batch and remove these disables incrementally.
// ---------------------------------------------------------------------------------------------
tasks.matching {
    it.name == "lintAnalyzeDebug" ||
            it.name == "lintDebug"
}.configureEach {
    enabled = false
}
