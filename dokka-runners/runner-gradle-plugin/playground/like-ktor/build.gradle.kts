/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginExperimentalApi

plugins {
    id("org.jetbrains.dokka")
}

val dokkaOutputDir = "../versions"

dokka {
    // TODO: recheck links
    externalLink("https://kotlinlang.org/api/kotlinx.coroutines/")
    externalLink("https://kotlinlang.org/api/kotlinx.serialization/")

    currentProject {
        outputDirectory = projectDir.resolve(dokkaOutputDir).resolve(project.version.toString())
    }

    // `enabled = true` is optional, just for explicitness
    @OptIn(DokkaGradlePluginExperimentalApi::class)
    versioning(enabled = true) {
        currentVersion = project.version.toString()
        oldVersionsDirectory = file(dokkaOutputDir)
    }
}

subprojects {
    plugins.apply("org.jetbrains.dokka")
}
