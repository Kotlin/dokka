
/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.dokka.gradle.dsl.registerDokkaJarTask

plugins {
    id("org.jetbrains.dokka")
    `maven-publish`
}

val dokkaJarTask = dokka.registerDokkaJarTask {
    // custom config here if needed
}

publishing.publications.withType<MavenPublication>().configureEach {
    artifact(dokkaJarTask)
}
