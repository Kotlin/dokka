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
    compileOnly(projects.dokkaSubprojects.dokkaCore)
    compileOnly(projects.dokkaSubprojects.pluginBase)

    api(projects.dokkaSubprojects.analysisKotlinApi)

    // TODO [beresnev] analysis switcher
    //runtimeOnly(project(path = ":subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    runtimeOnly(project(path = ":dokka-subprojects:analysis-kotlin-descriptors", configuration = "shadow"))

    implementation(kotlin("reflect"))
    implementation(libs.jsoup)

    implementation(kotlin("test"))
    implementation(projects.dokkaSubprojects.dokkaTestApi)

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
}
