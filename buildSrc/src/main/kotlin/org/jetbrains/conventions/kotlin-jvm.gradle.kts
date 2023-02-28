package org.jetbrains.conventions

import org.jetbrains.configureDokkaVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.conventions.base")
    id("org.jetbrains.conventions.base-java")
    kotlin("jvm")
}

configureDokkaVersion()

val language_version: String by project

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjsr305=strict",
            "-Xskip-metadata-version-check",
        )
        allWarningsAsErrors = true
        languageVersion = language_version
        apiVersion = language_version
    }
}
