/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("templating-plugin")

dependencies {
    compileOnly(projects.core)

    api(libs.jsoup)

    implementation(projects.pluginBase)

    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiterParams)

    testImplementation(projects.pluginBaseTestUtils)
    testImplementation(projects.coreTestApi)
    testImplementation(libs.kotlinx.html)
}
