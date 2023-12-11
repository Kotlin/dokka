/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.analysisKotlinApi)

    api(libs.kotlin.compiler)

    implementation(projects.analysisMarkdownJb)
    implementation(projects.analysisJavaPsi)

    testImplementation(kotlin("test"))
    testImplementation(projects.coreContentMatcherTestUtils)
    testImplementation(projects.coreTestApi)

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
