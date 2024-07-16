/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("dokka-base-test-utils")

dependencies {
    compileOnly(projects.dokkaCore)
    compileOnly(projects.pluginBase)

    api(projects.analysisKotlinApi)

    implementation(kotlin("reflect"))
    implementation(libs.jsoup)

    implementation(kotlin("test"))
    implementation(projects.dokkaTestApi)

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaTestApi)
}
