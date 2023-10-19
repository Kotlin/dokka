/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("org.jetbrains.conventions.base-unit-test")
}

dependencies {
    compileOnly(projects.dokkaCore)

    implementation(projects.pluginBase)

    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(projects.pluginBase)
    testImplementation(projects.coreTestApi)

    symbolsTestConfiguration(project(path = ":analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestConfiguration(project(path = ":analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.pluginBaseTestUtils) {
        exclude(module = "analysis-kotlin-descriptors")
    }
}

registerDokkaArtifactPublication("androidDocumentationPlugin") {
    artifactId = "android-documentation-plugin"
}
