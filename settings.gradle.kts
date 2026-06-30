rootProject.name = "BWitch"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("android.arch.lifecycle")
                includeGroupAndSubgroups("android.arch.core")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
    }
}


include(":composeApp")
include(":shared:domain")
include(":shared:data")
include(":shared:presentation")
include(":shared:di")
