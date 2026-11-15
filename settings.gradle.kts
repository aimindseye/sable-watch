pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sablewatch"
// The Watch Face Format face (resource-only, no code).
include(":watchface")
// The watch app: complication data sources such as Sable World Clock. Same package as :phone.
include(":watch")
// The phone companion, built as `sable` (SableOS) and `generic` (any Android phone).
include(":phone")
// What the phone sends the watch: the data contract both apps share, and its formatting.
include(":shared")
