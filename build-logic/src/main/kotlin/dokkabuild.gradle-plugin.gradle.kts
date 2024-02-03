/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.gradle.kotlin.kotlin-dsl")
    id("dokkabuild.java")
    kotlin("jvm")
    id("dokkabuild.publish-gradle-plugin")
}

// org.gradle.kotlin.kotlin-dsl sets languageVersion and apiVersion to 1.8 by default starting from Gradle 8.
// As we need to be compatible with previous Gradle versions, we need to set it back to 1.4.
// Note: we should do it directly on tasks and not via top-level `kotlin.compilerOptions`
//       because `kotlin-dsl plugin` declares them on task level, and so top-level config is overridden
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion = dokkaBuild.kotlinLanguageLevel
        apiVersion = dokkaBuild.kotlinLanguageLevel

        freeCompilerArgs.addAll(
            // need 1.4 support, otherwise there might be problems
            // with Gradle 6.x (it's bundling Kotlin 1.4)
            "-Xsuppress-version-warnings",
            "-Xjsr305=strict",
            "-Xskip-metadata-version-check",
        )
    }
}

tasks.validatePlugins {
    enableStricterValidation = true
}
