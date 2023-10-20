/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.overridePublicationArtifactId

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.publishing-shadow")
}

overridePublicationArtifactId("dokka-cli")

dependencies {
    implementation("org.jetbrains.dokka:dokka-core")
    implementation(libs.kotlinx.cli)

    testImplementation(kotlin("test"))
}

tasks.shadowJar {
    manifest {
        attributes("Main-Class" to "org.jetbrains.dokka.MainKt")
    }
}
