/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    id("dokkabuild.test-k2")
}

overridePublicationArtifactId("mathjax-plugin")

dependencies {
    compileOnly(projects.dokkaCore)

    implementation(projects.pluginBase)

    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.jsoup)
    testImplementation(projects.coreContentMatcherTestUtils)
    testImplementation(projects.dokkaTestApi)

    symbolsTestImplementation(project(path = ":analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestImplementation(project(path = ":analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.pluginBaseTestUtils)
}
