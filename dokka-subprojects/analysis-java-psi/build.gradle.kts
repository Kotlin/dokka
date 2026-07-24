/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.kotlin-jvm")
}

dependencies {
    compileOnly(projects.dokkaSubprojects.dokkaCore)
    compileOnly(projects.dokkaSubprojects.analysisKotlinApi)

    // We exclude `log4j` as it's not used in our codebase,
    // and we do override intellij logger with NOOP logger
    // `log4j` dependency triggers errors by dependency vulnerability checkers
    implementation(libs.intellij.java.psi.impl) {
        exclude("org.jetbrains.intellij.deps", "log4j")
    }
    // Since intellij-platform 261, core PSI API classes such as `com.intellij.psi.PsiElement` are no longer
    // on the `java-psi(-impl)` classpath. They're needed only at compile time here — at runtime they're
    // provided by `kotlin-compiler` (which bundles the IntelliJ core).
    compileOnly(libs.intellij.platform.core)
    implementation(libs.intellij.util)
    implementation(projects.dokkaSubprojects.analysisMarkdownJb)
    implementation(libs.jetbrains.markdown)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
}
