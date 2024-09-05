rootProject.name = "multimodule-example"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")
