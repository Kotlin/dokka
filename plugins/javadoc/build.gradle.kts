/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.plugins.base)
    implementation(projects.plugins.kotlinAsJava)

    implementation(kotlin("reflect"))
    implementation(libs.korlibs.template)
    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.core.testApi)
    testImplementation(libs.jsoup)
}

registerDokkaArtifactPublication("javadocPlugin") {
    artifactId = "javadoc-plugin"
}
