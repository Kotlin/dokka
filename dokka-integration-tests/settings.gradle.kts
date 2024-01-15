/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage")

rootProject.name = "dokka-integration-tests"

pluginManagement {
    includeBuild("../build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("../dokka-runners/runner-gradle-plugin-classic")
includeBuild("../dokka-runners/runner-maven-plugin")
includeBuild("../dokka-runners/runner-cli")
includeBuild("../.") // include the very root aggregating build so that we can depend on its tasks

include(
    ":cli",
    ":gradle",
    ":maven",
    ":utilities",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
