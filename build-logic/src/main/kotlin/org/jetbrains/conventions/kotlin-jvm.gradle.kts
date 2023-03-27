package org.jetbrains.conventions

import org.jetbrains.configureDokkaVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.conventions.base")
    id("org.jetbrains.conventions.base-java")
    kotlin("jvm")
}

configureDokkaVersion()

val projectsWithoutOptInDependency = setOf(
    ":integration-tests", ":integration-tests:gradle", ":integration-tests:maven", ":integration-tests:cli")

tasks.withType<KotlinCompile>().configureEach {
    // By path because Dokka has multiple projects with the same name (i.e. 'cli')
    if (project.path in projectsWithoutOptInDependency) return@configureEach
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=org.jetbrains.dokka.InternalDokkaApi",
                "-Xjsr305=strict",
                "-Xskip-metadata-version-check",
                // need 1.4 support, otherwise there might be problems with Gradle 6.x (it's bundling Kotlin 1.4)
                "-Xsuppress-version-warnings",
            )
        )
        allWarningsAsErrors.set(true)
        languageVersion.set(dokkaBuild.kotlinLanguageLevel)
        apiVersion.set(dokkaBuild.kotlinLanguageLevel)
    }
}
