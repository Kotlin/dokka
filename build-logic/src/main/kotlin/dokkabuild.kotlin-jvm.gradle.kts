/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.utils.configureGradleKotlinCompatibility

plugins {
    id("dokkabuild.java")
    kotlin("jvm")
}

val rootProjectsWithoutDependencyOnDokkaCore = listOf("dokka-integration-tests")

configureGradleKotlinCompatibility()

kotlin {
    explicitApi()

    compilerOptions {
        allWarningsAsErrors = false

        // These projects know nothing about the `@InternalDokkaApi` annotation, so the Kotlin compiler
        // will complain about an unresolved opt-in requirement marker and fail the build if it's not excluded.
        if (rootProject.name !in rootProjectsWithoutDependencyOnDokkaCore) {
            optIn.addAll(
                "kotlin.RequiresOptIn",
                "org.jetbrains.dokka.InternalDokkaApi"
            )
        }
    }
}

// !! infrastructure for builds as a Kotlin user project
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@{
    // ("unconditionally pass the -Xreport-all-warnings CLI option to the Kotlin compiler")
    // ("unconditionally pass the -Xrender-internal-diagnostic-names CLI option to the Kotlin compiler")
    compilerOptions {
        // output reported warnings even in the presence of reported errors
        freeCompilerArgs.add("-Xreport-all-warnings")
        logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xreport-all-warnings")
        // output kotlin.git-searchable names of reported diagnostics
        freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
        logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xrender-internal-diagnostic-names")
    }
}

// !! infrastructure for builds as a Kotlin user project
// ("implement a Gradle property for passing additional CLI options to the Kotlin compiler")
val kotlinAdditionalCliOptions = providers.gradleProperty("kotlin_additional_cli_options")
    .map { if (it.isNotBlank()) it.split(" ") else emptyList() } // i sure hope that this level of parsing is in fact enough
    .orNull
if (kotlinAdditionalCliOptions != null) {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@{
        compilerOptions {
            freeCompilerArgs.addAll(kotlinAdditionalCliOptions)
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added ${kotlinAdditionalCliOptions.joinToString(" ")}")
        }
    }
}

// !! infrastructure for builds as a Kotlin user project
// ("unconditionally disable the -Werror compiler CLI option")
afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@{
        compilerOptions {
            allWarningsAsErrors.set(false)
            freeCompilerArgs.set(
                freeCompilerArgs.getOrElse(emptyList()).filterNot { it == "-Werror" }
            )
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : disabled -Werror")
        }
    }
}

// !! infrastructure for builds as a Kotlin user project
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@{
    // ("pass the -Wextra CLI option to the Kotlin compiler when the -Werror compiler CLI option is disabled")
    // ("pass the -Xuse-fir-experimental-checkers CLI option to the Kotlin compiler when the -Werror compiler CLI option is disabled")
    compilerOptions {
        // opt into additional warning-reporting compilation checks
        freeCompilerArgs.add("-Wextra")
        logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Wextra")
        // opt into additional compilation checks hidden from users
        freeCompilerArgs.add("-Xuse-fir-experimental-checkers")
        logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xuse-fir-experimental-checkers")
    }
}