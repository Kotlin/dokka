/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginExperimentalApi

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    @OptIn(DokkaGradlePluginExperimentalApi::class)
    versioning {
        currentVersion = "0.10.0"
        versionsOrdering = listOf(
            "0.10.0",
            "1.0.0",
            "2.0.0"
        )
        oldVersionsDirectory = layout.buildDirectory.dir("dokka/old")
        // or
        oldVersionsDirectory = file("build/dokka/old")
    }
}
