pluginManagement {
    plugins {
        kotlin("jvm") version "1.6.10"
        id("org.jetbrains.dokka") version ("1.6.10")
    }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")

rootProject.name = "dokka-multimodule-example"
