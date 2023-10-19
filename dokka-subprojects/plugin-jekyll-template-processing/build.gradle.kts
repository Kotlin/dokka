/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.dokkaCore)

    implementation(projects.pluginBase)
    implementation(projects.pluginJekyll)
    implementation(projects.pluginAllModulesPage)
    implementation(projects.pluginTemplating)
    implementation(projects.pluginGfm)
    implementation(projects.pluginGfmTemplateProcessing)

    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(projects.coreTestApi)
}

registerDokkaArtifactPublication("dokkaJekyllTemplateProcessing") {
    artifactId = "jekyll-template-processing-plugin"
}
