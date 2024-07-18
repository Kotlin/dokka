/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.kotlin-jvm")
}

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)
    compileOnly(projects.dokkaSubprojects.analysisKotlinApi)

    api(libs.kotlin.compiler)

    implementation(projects.dokkaSubprojects.analysisMarkdownJb)
    implementation(projects.dokkaSubprojects.analysisJavaPsi)

    testImplementation(kotlin("test"))
    testImplementation(projects.dokkaSubprojects.coreContentMatcherTestUtils)
    testImplementation(projects.dokkaSubprojects.dokkaTestApi)
    testImplementation(projects.dokkaSubprojects.analysisKotlinApi)

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
