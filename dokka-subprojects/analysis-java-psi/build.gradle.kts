/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.kotlin-jvm")
}

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)
    compileOnly(projects.dokkaSubprojects.analysisKotlinApi)

    api(libs.intellij.java.psi.api)

    implementation(projects.dokkaSubprojects.analysisMarkdownJb)

    implementation(libs.intellij.java.psi.impl)
    implementation(libs.intellij.platform.util.api)
    implementation(libs.intellij.platform.util.rt)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
}
