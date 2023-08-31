/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

registerDokkaArtifactPublication("dokkaAllModulesPage") {
    artifactId = "all-modules-page-plugin"
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.plugins.base)
    implementation(projects.plugins.templating)

    implementation(projects.subprojects.analysisMarkdownJb)

    implementation(libs.kotlinx.html)

    testImplementation(kotlin("test"))
    testImplementation(projects.plugins.base)
    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.plugins.gfm)
    testImplementation(projects.plugins.gfm.gfmTemplateProcessing)
    testImplementation(projects.core.contentMatcherTestUtils)
    testImplementation(projects.core.testApi)
}
