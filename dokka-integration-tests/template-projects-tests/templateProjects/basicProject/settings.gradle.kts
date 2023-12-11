/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "it-basic"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "%{KOTLIN_VERSION}%"
        id("org.jetbrains.dokka") version "%{DOKKA_VERSION}%"
    }

    repositories {
        /* %{PROJECT_LOCAL_MAVEN_DIR}% */
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        /* %{PROJECT_LOCAL_MAVEN_DIR}% */
        mavenCentral()
    }
}
