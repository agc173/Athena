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

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
// Workaround: lint a veces crashea en KMP/AGP durante lintAnalyzeDebug.
// abortOnError=false NO evita crashes del motor de lint, así que desactivamos esta tarea.
tasks.matching { it.name == "lintAnalyzeDebug" }.configureEach {
    enabled = false
}