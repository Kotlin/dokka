/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kdp"

include(":kotlin-documentation-model") // kotlinx.serialization based machine readable format
//include(":kotlin-documentation-analyzer") // Kotlin Analysis API based analyzer
//include(":kotlin-documentation-renderer") // HTML renderer
