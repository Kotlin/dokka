pluginManagement {
    plugins {
        kotlin("jvm") version "1.4.32"
        id("org.jetbrains.dokka") version ("1.4.32")
    }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")

rootProject.name = "dokka-multimodule-example"
