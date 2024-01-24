/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-shadow")
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
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
