/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// root build.gradle.kts

plugins {
    id("org.jetbrains.dokka")
}

val dokkaOutputDir = "../versions"

dokka {
    outputDirectory.set(file(projectDir.toPath().resolve(dokkaOutputDir).resolve(configuredVersion)))
    pluginConfiguration("org.jetbrains.dokka.versioning.VersioningPlugin") {
        property("version", configuredVersion)
        property("olderVersionsDir", dokkaOutputDir)
    }
}
