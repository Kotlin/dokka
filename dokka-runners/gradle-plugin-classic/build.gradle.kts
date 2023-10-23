/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.MAVEN_GRADLE_PLUGIN_PUBLICATION_NAME
import org.jetbrains.overridePublicationArtifactId

plugins {
    id("org.jetbrains.conventions.gradle-plugin")
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

overridePublicationArtifactId("dokka-gradle-plugin", MAVEN_GRADLE_PLUGIN_PUBLICATION_NAME)

dependencies {
    // this version is required for Gradle plugin publishing
    api("org.jetbrains.dokka:dokka-core:$version")

    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.kotlin.klibCommonizerApi)
    compileOnly(libs.gradlePlugin.android)

    testImplementation(kotlin("test"))
    testImplementation(libs.gradlePlugin.kotlin)
    testImplementation(libs.gradlePlugin.kotlin.klibCommonizerApi)
    testImplementation(libs.gradlePlugin.android)
}

gradlePlugin {
    plugins {
        create("dokka") {
            id = "org.jetbrains.dokka"
            implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"

            displayName = "Dokka plugin"
            description = "Dokka is an API documentation engine for Kotlin"
            tags.addAll("dokka", "kotlin", "kdoc", "android", "documentation", "api")
        }
    }
}
