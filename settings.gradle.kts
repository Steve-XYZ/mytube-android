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
        // youtubedl-android is distributed through JitPack.
        maven("https://jitpack.io")
    }
}

rootProject.name = "mytube-android"
include(":app")
