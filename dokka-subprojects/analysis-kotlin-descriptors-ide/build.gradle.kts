/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.kotlin-jvm")
}

dependencies {
    compileOnly(projects.dokkaCore)
    compileOnly(projects.analysisKotlinApi)

    implementation(projects.analysisKotlinDescriptorsCompiler)

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
