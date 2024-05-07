/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.PublicationName
import dokkabuild.overridePublicationArtifactId

plugins {
    id("dokkabuild.gradle-plugin")
    id("dokkabuild.dev-maven-publish")
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

overridePublicationArtifactId("dokka-gradle-plugin", PublicationName.GRADLE_PLUGIN)

dependencies {
    // the version is required for Gradle plugin publishing
    api("org.jetbrains.dokka:dokka-core:$version")

    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.kotlin.klibCommonizerApi)
    compileOnly(libs.gradlePlugin.android)

    testImplementation(kotlin("test"))
    testImplementation(libs.gradlePlugin.kotlin)
    testImplementation(libs.gradlePlugin.kotlin.klibCommonizerApi)
    testImplementation(libs.gradlePlugin.android)
    testImplementation("org.jetbrains.dokka:dokka-test-api:$version")
}

gradlePlugin {
    plugins {
        create("dokka") {
            id = "org.jetbrains.dokka"
            implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"

            displayName = "Dokka plugin"
            description = "Dokka is an API documentation engine for Kotlin"
            @Suppress("UnstableApiUsage")
            tags.addAll("dokka", "kotlin", "kdoc", "android", "documentation", "api")
        }
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
