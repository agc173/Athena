import org.gradle.api.GradleException
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.services)
}

configurations.configureEach {
    resolutionStrategy {
        force(
            "androidx.core:core:1.15.0",
            "androidx.core:core-ktx:1.15.0",
            "com.google.firebase:firebase-common:20.4.2",
            "com.google.firebase:firebase-common-ktx:20.4.2",
        )
    }
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
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            // Android deps are declared explicitly (no Firebase BoM in this module)

            implementation(libs.androidx.core)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity)
            implementation(libs.androidx.activity.ktx)

            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kamel.image.default)
            implementation(libs.multiplatform.settings)

            // Google Sign-In (Credential Manager)
            implementation("androidx.credentials:credentials:1.5.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
            implementation("io.insert-koin:koin-android:3.5.6")

            // Firebase App Check (native Android SDK only) to fetch App Check tokens.
            // GitLive remains the source for Auth/Firestore/Functions integrations.
            implementation("com.google.firebase:firebase-appcheck-debug:18.0.0")
            implementation("com.google.firebase:firebase-appcheck-playintegrity:18.0.0")
            implementation("com.google.firebase:firebase-messaging:24.1.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
            implementation("com.google.android.gms:play-services-ads:24.3.0")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }


        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.compose)
            implementation(project(":shared:di"))
            implementation(project(":shared:domain"))
            implementation(project(":shared:presentation"))
            implementation(project(":shared:data"))
            implementation(libs.kamel.image.default)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}


android {
    val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
    // Guard release-capable entry points. `contains("release")` covers variant-specific tasks
    // such as `:composeApp:assembleRelease`, `:composeApp:bundleRelease`, and publishRelease*;
    // aggregate assemble/build/bundle/publish tasks can also produce release artifacts.
    val releaseBuildRequested = gradle.startParameter.taskNames.any { taskName ->
        val normalized = taskName.lowercase()
        normalized.contains("release") ||
            normalized == "assemble" ||
            normalized.endsWith(":assemble") ||
            normalized == "build" ||
            normalized.endsWith(":build") ||
            normalized.startsWith("bundle") ||
            normalized.endsWith(":bundle") ||
            normalized == "publish" ||
            normalized.endsWith(":publish")
    }

    fun requiredReleaseAdMobValue(name: String): String {
        val value = providers.gradleProperty(name)
            .orElse(providers.environmentVariable(name))
            .orNull
            ?.trim()
            .orEmpty()

        if (releaseBuildRequested && value.isBlank()) {
            throw GradleException(
                "$name is required for release builds. " +
                    "Define it as a Gradle property or environment variable."
            )
        }

        return value
    }

    val releaseAdmobAppId = requiredReleaseAdMobValue("ADMOB_APP_ID")
    val releaseRewardedAdUnitId = requiredReleaseAdMobValue("ADMOB_REWARDED_AD_UNIT_ID")

    namespace = "com.agc.bwitch"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.agc.bwitch"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 17
        versionName = "1.1.6"
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["ADMOB_APP_ID"] = admobTestAppId
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"\"")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            manifestPlaceholders["ADMOB_APP_ID"] = releaseAdmobAppId
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$releaseRewardedAdUnitId\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        // TEMP WORKAROUND (Fase 1):
        // There is an AGP/Lint vs Kotlin metadata incompatibility in release lint analysis
        // (lint expects older metadata while project/deps are already on newer Kotlin metadata),
        // and it can crash in androidx.lifecycle.lint.NonNullableMutableLiveDataDetector.
        // Keep this scoped and explicit until the planned AGP/Kotlin/Compose upgrade phase.
        disable += "NullSafeMutableLiveData"
        disable += "FrequentlyChangingValue"

        // Main blocker for `./gradlew build` is release lint (`lintVitalAnalyzeRelease`).
        // Temporarily avoid gating release builds on that lint phase until tooling versions are aligned.
        checkReleaseBuilds = false

        // Keep non-blocking lint while we progressively stabilize the KMP/AGP setup.
        abortOnError = false
    }
}

// ---------------------------------------------------------------------------------------------
// LINT WORKAROUND (composeApp / AGP-Lint-Kotlin metadata incompatibilities)
//
// Why this exists:
// - `./gradlew build` can still execute debug/release lint tasks in this module and hit
//   detector crashes / metadata incompatibilities in current tooling versions.
//
// Scope:
// - Explicitly restricted to composeApp lint task entry-points used by debug/release checks, including debug test variants.
// - Temporary measure only; remove after planned AGP/Kotlin/Compose alignment batch.
// ---------------------------------------------------------------------------------------------
tasks.matching {
    it.name in setOf(
        "lintDebug",
        "lintAnalyzeDebug",
        "lintAnalyzeDebugUnitTest",
        "lintAnalyzeDebugAndroidTest",
        "lintDebugUnitTest",
        "lintDebugAndroidTest",
        "lintRelease",
        "lintAnalyzeRelease",
        "lintVitalAnalyzeRelease",
    )
}.configureEach {
    enabled = false
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
