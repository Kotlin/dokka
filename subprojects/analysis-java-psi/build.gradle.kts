/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)

    api(libs.intellij.java.psi.api)

    implementation(projects.subprojects.analysisMarkdownJb)

    implementation(libs.intellij.java.psi.impl)
    implementation(libs.intellij.platform.util.api)
    implementation(libs.intellij.platform.util.rt)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
}
