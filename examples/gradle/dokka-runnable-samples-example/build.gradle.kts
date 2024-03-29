/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.20"
}

dependencies {
    testImplementation(kotlin("test-junit"))
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        samples.from("src/test")
    }
}
