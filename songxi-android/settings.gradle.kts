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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "songxi-android"

include(":app")
include(":core:model")
include(":core:data")
include(":core:sensory")
include(":core:designsystem")
include(":feature:splashad")
include(":feature:stretch")
include(":feature:sanctuary")
