/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("all-modules-page-plugin")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)
    compileOnly(projects.dokkaSubprojects.analysisKotlinApi)

    implementation(projects.dokkaSubprojects.pluginBase)
    implementation(projects.dokkaSubprojects.pluginTemplating)

    implementation(projects.dokkaSubprojects.analysisMarkdownJb)

    implementation(libs.kotlinx.html)

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaSubprojects.pluginBase)
    testImplementation(projects.dokkaSubprojects.pluginBaseTestUtils)
    testImplementation(projects.dokkaSubprojects.pluginGfm)
    testImplementation(projects.dokkaSubprojects.pluginGfmTemplateProcessing)
    testImplementation(projects.dokkaSubprojects.coreContentMatcherTestUtils)
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
}
