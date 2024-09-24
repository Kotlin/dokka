/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    `kotlin-dsl`
}

description = "Conventions for use in settings.gradle.kts scripts"

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.gradlePlugin.gradle.develocity)
    implementation(libs.gradlePlugin.gradle.customUserData)
}

tasks.jar {
    // Sync the JAR into a local dir and commit it to git, so the build-settings-logic project
    // can use its own convention plugin.
    destinationDirectory = layout.projectDirectory.dir("libs")

    // Create a fat jar, and fold in the Develocity plugins, so build-settings-logic can apply
    // its own plugin.
    // (Gradle requires the build cache settings are consistent in all projects).
    val archives = serviceOf<ArchiveOperations>()
    val jarNamePrefixes = setOf(
        "common-custom-user-data-gradle-plugin",
        "develocity-gradle-plugin",
    )
    dependsOn(configurations.runtimeClasspath)
    from(
        configurations.runtimeClasspath.map { classpath ->
            classpath
                .filter { file -> file.extension == "jar" }
                .filter { file -> jarNamePrefixes.any { prefix -> file.name.startsWith(prefix) } }
                .map { archives.zipTree(it) }
        }
    )
}
