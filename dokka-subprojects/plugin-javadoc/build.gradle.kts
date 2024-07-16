/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    id("dokkabuild.test-k2")
}

overridePublicationArtifactId("javadoc-plugin")

dependencies {
    compileOnly(projects.dokkaCore)
    compileOnly(projects.analysisKotlinApi)

    implementation(projects.pluginBase)
    implementation(projects.pluginKotlinAsJava)

    implementation(kotlin("reflect"))
    implementation(libs.korlibs.template)
    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    symbolsTestImplementation(project(path = ":analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestImplementation(project(path = ":analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.pluginBaseTestUtils)
    testImplementation(projects.dokkaTestApi)
    testImplementation(libs.jsoup)
}
