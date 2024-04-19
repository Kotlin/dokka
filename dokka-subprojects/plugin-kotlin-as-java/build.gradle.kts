/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    id("dokkabuild.test-k2")
}

overridePublicationArtifactId("kotlin-as-java-plugin")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)
    compileOnly(projects.dokkaSubprojects.analysisKotlinApi)

    implementation(projects.dokkaSubprojects.pluginBase)

    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.jsoup)
    testImplementation(projects.dokkaSubprojects.pluginBase)
    symbolsTestImplementation(project(path = ":dokka-subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestImplementation(project(path = ":dokka-subprojects:analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.dokkaSubprojects.pluginBaseTestUtils) {
        exclude(module = "analysis-kotlin-descriptors")
    }
    testImplementation(projects.dokkaSubprojects.coreContentMatcherTestUtils)
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
}
