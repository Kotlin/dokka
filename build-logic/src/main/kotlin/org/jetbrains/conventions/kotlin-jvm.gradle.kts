/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

import org.jetbrains.configureDokkaVersion
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.conventions.base-java")
    kotlin("jvm")
}

configureDokkaVersion()

kotlin {
    explicitApi = ExplicitApiMode.Strict

    compilerOptions {
        allWarningsAsErrors.set(true)
        languageVersion.set(dokkaBuild.kotlinLanguageLevel)
        apiVersion.set(dokkaBuild.kotlinLanguageLevel)

        freeCompilerArgs.addAll(
            listOf(
                // need 1.4 support, otherwise there might be problems
                // with Gradle 6.x (it's bundling Kotlin 1.4)
                "-Xsuppress-version-warnings",
                "-Xjsr305=strict",
                "-Xskip-metadata-version-check",
            )
        )
    }
}

val projectsWithoutInternalDokkaApiUsage = setOf(
    ":integration-tests",
    ":integration-tests:gradle",
    ":integration-tests:maven",
    ":integration-tests:cli"
)

tasks.withType<KotlinCompile>().configureEach {
    // By path because Dokka has multiple projects with the same name (i.e. 'cli')
    if (project.path in projectsWithoutInternalDokkaApiUsage) {
        return@configureEach
    }
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=org.jetbrains.dokka.InternalDokkaApi",
            )
        )
    }
}
