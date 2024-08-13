/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.utils.excludeGradleEmbeddedDependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.gradle.kotlin.kotlin-dsl")
    id("dokkabuild.java")
    id("dokkabuild.publish-gradle-plugin")
}

kotlin {
    compilerOptions {
        // Must use Kotlin 1.4 to support Gradle 7
        languageVersion = @Suppress("DEPRECATION") KotlinVersion.KOTLIN_1_4
        apiVersion = @Suppress("DEPRECATION") KotlinVersion.KOTLIN_1_4
    }
}

tasks.compileKotlin {
    compilerOptions {
        // `kotlin-dsl` plugin overrides the versions at the task level,
        // which takes priority over the `kotlin` project extension.
        // So, fix it by manually setting the LV per-task.
        languageVersion.set(kotlin.compilerOptions.languageVersion)
        apiVersion.set(kotlin.compilerOptions.apiVersion)
    }
}

tasks.validatePlugins {
    enableStricterValidation = true
}

excludeGradleEmbeddedDependencies(sourceSets.main)
