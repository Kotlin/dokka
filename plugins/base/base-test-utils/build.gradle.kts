/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.plugins.base)

    api(projects.subprojects.analysisKotlinApi)

    // TODO [beresnev] analysis switcher
    //runtimeOnly(project(path = ":subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    runtimeOnly(project(path = ":subprojects:analysis-kotlin-descriptors", configuration = "shadow"))

    implementation(kotlin("reflect"))
    implementation(libs.jsoup)

    implementation(kotlin("test"))
    implementation(projects.core.testApi)

    testImplementation(kotlin("test"))
    testImplementation(projects.core.testApi)
}

registerDokkaArtifactPublication("dokkaBaseTestUtils") {
    artifactId = "dokka-base-test-utils"
}
