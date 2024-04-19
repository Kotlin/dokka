/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    id("dokkabuild.test-k2")
}

overridePublicationArtifactId("android-documentation-plugin")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)

    implementation(projects.dokkaSubprojects.pluginBase)

    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaSubprojects.pluginBase)
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)

    symbolsTestImplementation(project(path = ":dokka-subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestImplementation(project(path = ":dokka-subprojects:analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.dokkaSubprojects.pluginBaseTestUtils) {
        exclude(module = "analysis-kotlin-descriptors")
    }
}
