package org.jetbrains.conventions

import org.jetbrains.configureDokkaVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.conventions.base")
    kotlin("jvm")
}

configureDokkaVersion()

val language_version: String by project

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjsr305=strict",
            "-Xskip-metadata-version-check",
            // need 1.4 support, otherwise there might be problems with Gradle 6.x (it's bundling Kotlin 1.4)
            "-Xsuppress-version-warnings"
        )
        allWarningsAsErrors = true
        languageVersion = language_version
        apiVersion = language_version
        jvmTarget = "1.8"
    }
}

// Gradle metadata
java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_1_8
}
