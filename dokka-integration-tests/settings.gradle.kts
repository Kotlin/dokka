/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "dokka-integration-tests"

pluginManagement {
    includeBuild("../build-logic")
    includeBuild("../build-settings-logic")

    repositories {
        mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }
}

plugins {
    id("dokkasettings")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("../dokka-runners/dokka-gradle-plugin")
includeBuild("../dokka-runners/runner-maven-plugin")
includeBuild("../dokka-runners/runner-cli")
includeBuild("../.") // depend on the root project, so integration-tests can depend on projects in `dokka-subprojects/*`

include(
    ":cli",
    ":gradle",
    ":maven",
    ":utilities",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
