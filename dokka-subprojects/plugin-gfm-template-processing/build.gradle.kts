/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("gfm-template-processing-plugin")

dependencies {
    compileOnly(projects.dokkaCore)

    implementation(projects.pluginBase)
    implementation(projects.pluginGfm)
    implementation(projects.pluginAllModulesPage)
    implementation(projects.pluginTemplating)

    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaTestApi)
}
