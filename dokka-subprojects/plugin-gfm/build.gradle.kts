/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.overridePublicationArtifactId

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.publishing-default")
}

overridePublicationArtifactId("gfm-plugin")

dependencies {
    compileOnly(projects.dokkaCore)

    implementation(projects.pluginBase)

    implementation(kotlin("reflect"))
    implementation(libs.jackson.kotlin)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }

    testImplementation(kotlin("test"))
    testImplementation(projects.pluginBase)
    testImplementation(projects.pluginBaseTestUtils)
    testImplementation(projects.coreTestApi)
}
