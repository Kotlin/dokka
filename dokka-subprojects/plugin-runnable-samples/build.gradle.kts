/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("runnable-samples-plugin")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)
    compileOnly(projects.dokkaSubprojects.analysisKotlinApi)

    implementation(projects.dokkaSubprojects.pluginBase)

    testImplementation(libs.kotlin.test)
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
    testImplementation(projects.dokkaSubprojects.pluginBaseTestUtils)
    testImplementation(projects.dokkaSubprojects.analysisKotlinSymbols)
}
