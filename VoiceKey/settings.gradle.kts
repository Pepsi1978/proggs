pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // WebRTC-VAD (Sprach-Gate vor Vosk) wird nur ueber JitPack verteilt.
        maven(url = "https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\.gkonovalov.*") }
        }
    }
}

rootProject.name = "VoiceKey"

include(":app")
