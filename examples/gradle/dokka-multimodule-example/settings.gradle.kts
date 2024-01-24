/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion
    }
}

include(":parentProject")
include(":parentProject:childProjectA")
include(":parentProject:childProjectB")

rootProject.name = "dokka-multimodule-example"
