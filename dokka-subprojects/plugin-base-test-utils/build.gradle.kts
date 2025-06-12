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

    implementation(libs.kotlin.reflect)
    implementation(libs.jsoup)

    implementation(libs.kotlin.test)
    implementation(projects.dokkaSubprojects.dokkaTestApi)

    testImplementation(libs.kotlin.test)
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
}
