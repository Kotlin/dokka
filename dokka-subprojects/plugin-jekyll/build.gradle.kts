/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
}

overridePublicationArtifactId("jekyll-plugin")

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)

    implementation(projects.dokkaSubprojects.pluginBase)
    implementation(projects.dokkaSubprojects.pluginGfm)

    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
}
