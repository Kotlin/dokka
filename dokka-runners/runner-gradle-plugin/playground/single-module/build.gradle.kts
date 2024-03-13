/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.dsl.DokkaDeclarationVisibility
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDelicateApi

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    documentedVisibilities.add(DokkaDeclarationVisibility.INTERNAL)
    currentProject {
        outputDirectory.set(layout.buildDirectory.dir("customDokkaDir"))

        @OptIn(DokkaGradlePluginDelicateApi::class)
        sourceSets.configureEach {
            samples.from("src/$name/samples")

            suppressedSourceFiles.from("build/generated")
        }
    }
}
