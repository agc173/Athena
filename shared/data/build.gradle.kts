import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test
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
        androidMain.dependencies {
            implementation(libs.google.play.billing)
            implementation("com.github.cosinekitty:astronomy:v2.1.17")
        }

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
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

android {
    namespace = "com.agc.bwitch.shared.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

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
// LINT WORKAROUND (KMP + Android Lint instability)
//
// Why this exists:
// - In this module, lint analysis may crash with "Unexpected failure during lint analysis"
//   when traversing expect/actual KMP declarations.
//
// Scope:
// - Explicitly limited to DEBUG lint tasks of this module only (main + unit/androidTest variants).
// - We keep `checkReleaseBuilds = true` above, so release lint policy is preserved.
//
// Next safe step to remove:
// 1) Run `:shared:data:lintDebug` after AGP/Kotlin updates in a dedicated tech batch.
// 2) If stable, remove `lintAnalyzeDebug` first, then `lintDebug`.
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

val natalEnginePrecisionReport = layout.buildDirectory.file("reports/natal-engine-precision-report.txt")
val natalAuditSampleSize = providers.systemProperty("natalAuditSampleSize")
    .orElse(providers.gradleProperty("natalAuditSampleSize"))
val isNatalEnginePrecisionAuditRequested = provider {
    gradle.startParameter.taskNames.any { requestedTask ->
        requestedTask == "natalEnginePrecisionAudit" || requestedTask.endsWith(":natalEnginePrecisionAudit")
    }
}

tasks.withType<Test>().configureEach {
    if (name == "testDebugUnitTest") {
        systemProperty("natalPrecisionAuditReportPath", natalEnginePrecisionReport.get().asFile.absolutePath)
        natalAuditSampleSize.orNull?.let { requestedSampleSize ->
            systemProperty("natalAuditSampleSize", requestedSampleSize)
        }
        outputs.file(natalEnginePrecisionReport)

        if (isNatalEnginePrecisionAuditRequested.get()) {
            filter {
                includeTestsMatching("com.agc.bwitch.data.astrology.natal.CommonNatalEnginePrecisionAuditTest")
            }
            doFirst {
                natalEnginePrecisionReport.get().asFile.delete()
            }
        }
    }
}

tasks.register("natalEnginePrecisionAudit") {
    group = "verification"
    description = "Runs the Android-only common natal engine precision audit and writes its report to build/reports/natal-engine-precision-report.txt."
    dependsOn("testDebugUnitTest")

    doLast {
        val reportFile = natalEnginePrecisionReport.get().asFile
        if (!reportFile.isFile) {
            throw GradleException("Natal engine precision audit did not create ${reportFile.absolutePath}")
        }
        logger.lifecycle("Natal engine precision audit report: ${reportFile.absolutePath}")
    }
}
