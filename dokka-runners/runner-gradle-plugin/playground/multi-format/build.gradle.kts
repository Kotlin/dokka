/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDelicateApi
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginExperimentalApi
import org.jetbrains.dokka.gradle.dsl.registerDokkaJarTask

plugins {
    id("org.jetbrains.dokka")
}


dokka {
    @OptIn(DokkaGradlePluginDelicateApi::class, DokkaGradlePluginExperimentalApi::class)
    variants {
        // note: in PoC `main` is not created, so we need to use `register` here instead of `named`
        register("main") {
            currentProject {
                outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
            }
        }
        register("javadoc") {
            plugins("org.jetbrains.dokka:javadoc-plugin")
            currentProject {
                outputDirectory.set(layout.buildDirectory.dir("dokka/javadoc"))
            }
        }
    }
}

// javadoc.jar from `javadoc` variant
@OptIn(DokkaGradlePluginExperimentalApi::class)
val dokkaJavadocJarTask = dokka.registerDokkaJarTask(variant = "javadoc")
