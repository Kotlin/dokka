/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("templating-plugin")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)

    api(libs.jsoup)

    implementation(projects.dokkaSubprojects.pluginBase)

    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiterParams)

    testImplementation(projects.dokkaSubprojects.pluginBaseTestUtils)
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
    testImplementation(libs.kotlinx.html)
}
