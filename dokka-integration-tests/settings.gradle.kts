/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage")

rootProject.name = "dokka-integration-tests"

pluginManagement {
    includeBuild("../build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("../dokka-runners/gradle-plugin-classic")
includeBuild("../dokka-runners/cli")
includeBuild("../dokka-runners/maven-plugin")
includeBuild("../dokka-subprojects")

include(
    ":cli",
    ":gradle",
    ":maven",
    ":integration-test-utilities",
)
