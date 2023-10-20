/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

plugins {
    id("org.jetbrains.conventions.base-java")
    kotlin("jvm")
}

kotlin {
    explicitApi()
    compilerOptions {
        allWarningsAsErrors.set(true)
        languageVersion.set(dokkaBuild.kotlinLanguageLevel)
        apiVersion.set(dokkaBuild.kotlinLanguageLevel)

        freeCompilerArgs.addAll(
            // need 1.4 support, otherwise there might be problems
            // with Gradle 6.x (it's bundling Kotlin 1.4)
            "-Xsuppress-version-warnings",
            "-Xjsr305=strict",
            "-Xskip-metadata-version-check",
        )
        if (rootProject.name != "dokka-integration-tests") {
            optIn.addAll(
                "kotlin.RequiresOptIn",
                "org.jetbrains.dokka.InternalDokkaApi"
            )
        }
    }
}
