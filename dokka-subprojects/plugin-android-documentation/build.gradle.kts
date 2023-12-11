/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    id("dokkabuild.test-k2")
}

overridePublicationArtifactId("android-documentation-plugin")

dependencies {
    compileOnly(projects.core)

    implementation(projects.pluginBase)

    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(projects.pluginBase)
    testImplementation(projects.coreTestApi)

    symbolsTestConfiguration(project(path = ":analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestConfiguration(project(path = ":analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.dokkaSubprojects.pluginBaseTestUtils) {
        exclude(module = "analysis-kotlin-descriptors")
    }
}
