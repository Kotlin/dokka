/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.gradle.kotlin.kotlin-dsl")
    id("org.jetbrains.conventions.base-java")
    kotlin("jvm")
    id("org.jetbrains.conventions.publishing-gradle")
}

// org.gradle.kotlin.kotlin-dsl sets languageVersion and apiVersion to 1.8 by default starting from Gradle 8
// as we want to be compatible with previous Gradle versions, we need to set it back to 1.4
// Note: we should do it directly on tasks and not via top-level `kotlin.compilerOptions`
//       because kotlin-dsl plugin declares them on task level, and so top-level config is overridden
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(dokkaBuild.kotlinLanguageLevel)
        apiVersion.set(dokkaBuild.kotlinLanguageLevel)

        freeCompilerArgs.addAll(
            // need 1.4 support, otherwise there might be problems
            // with Gradle 6.x (it's bundling Kotlin 1.4)
            "-Xsuppress-version-warnings",
            "-Xjsr305=strict",
            "-Xskip-metadata-version-check",
        )
    }
}

// Gradle will put its own version of the stdlib in the classpath, so not pull our own we will end up with
// warnings like 'Runtime JAR files in the classpath should have the same version'
listOf(
    configurations.api,
    configurations.implementation,
    configurations.runtimeOnly
).forEach {
    it.configure { excludeGradleCommonDependencies() }
}

/**
 * These dependencies will be provided by Gradle, and we should prevent version conflict
 * Code taken from the Kotlin Gradle plugin:
 * https://github.com/JetBrains/kotlin/blob/70e15b281cb43379068facb82b8e4bcb897a3c4f/buildSrc/src/main/kotlin/GradleCommon.kt#L72
 */
fun Configuration.excludeGradleCommonDependencies() {
    dependencies
        .withType<ModuleDependency>()
        .configureEach {
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-script-runtime")
        }
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}