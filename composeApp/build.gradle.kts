import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.services)
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
            implementation(enforcedPlatform("com.google.firebase:firebase-bom:33.13.0"))
            implementation(libs.androidx.core)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity)
            implementation(libs.androidx.activity.ktx)

            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kamel.image.default)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.multiplatform.settings)


            // Google Sign-In (Credential Manager)
            implementation("androidx.credentials:credentials:1.5.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
            implementation("io.insert-koin:koin-android:3.5.6")

            // Firebase App Check (native Android SDK only) to fetch App Check tokens.
            // GitLive remains the source for Auth/Firestore/Functions integrations.
            implementation("com.google.firebase:firebase-appcheck-debug")
            // TODO(prod): implementation("com.google.firebase:firebase-appcheck-playintegrity:18.0.0")

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
    namespace = "com.agc.bwitch"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.agc.bwitch"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

configurations.matching {
    it.name.contains("RuntimeClasspath") || it.name.contains("CompileClasspath")
}.all {
    resolutionStrategy.force(
        "androidx.core:core:1.13.1",
        "androidx.core:core-ktx:1.13.1"
    )
}
