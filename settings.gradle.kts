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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // HU10: librería de gráficos tehras/charts (solo se busca ese grupo en JitPack)
        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\.tehras.*") }
        }
    }
}

rootProject.name = "NexStock"
include(":app")
