pluginManagement {
    repositories {
        maven {
            setUrl("https://jitpack.io")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven {
            setUrl("https://jitpack.io")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "GUI Injector"
include(":app")