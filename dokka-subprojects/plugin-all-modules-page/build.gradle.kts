/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("all-modules-page-plugin")

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.analysisKotlinApi)

    implementation(projects.pluginBase)
    implementation(projects.pluginTemplating)

    implementation(projects.analysisMarkdownJb)

    implementation(libs.kotlinx.html)

    testImplementation(kotlin("test"))
    testImplementation(projects.pluginBase)
    testImplementation(projects.pluginBaseTestUtils)
    testImplementation(projects.pluginGfm)
    testImplementation(projects.pluginGfmTemplateProcessing)
    testImplementation(projects.coreContentMatcherTestUtils)
    testImplementation(projects.coreTestApi)
}
