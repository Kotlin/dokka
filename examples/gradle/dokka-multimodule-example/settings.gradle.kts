pluginManagement {
    plugins {
        kotlin("jvm") version "1.7.0"
        id("org.jetbrains.dokka") version ("1.7.0")
    }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")

rootProject.name = "dokka-multimodule-example"
