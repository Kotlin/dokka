/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.overridePublicationArtifactId

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.publishing-default")
}

overridePublicationArtifactId("javadoc-plugin")

dependencies {
    compileOnly(projects.dokkaCore)
    compileOnly(projects.analysisKotlinApi)

    implementation(projects.pluginBase)
    implementation(projects.pluginKotlinAsJava)

    implementation(kotlin("reflect"))
    implementation(libs.korlibs.template)
    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(projects.pluginBaseTestUtils)
    testImplementation(projects.coreTestApi)
    testImplementation(libs.jsoup)
}