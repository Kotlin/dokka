/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    // TODO: File bug report for gradle: :moduleA:moduleB:dokkaHtml is missing kotlin gradle plugin from
    //  the runtime classpath during execution without this plugin in the parent project
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

allprojects {
    tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask> {
        pluginsMapConfiguration.set(
            mapOf(
                "org.jetbrains.dokka.base.DokkaBase" to """{ "homepageLink" : "https://github.com/Kotlin/dokka/tree/master/dokka-integration-tests/gradle/projects/it-multimodule-0/" }"""
            )
        )
    }
}
