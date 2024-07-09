/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "runner-gradle-plugin-classic"

pluginManagement {
    includeBuild("../../build-logic")
    includeBuild("../../build-settings-logic")

    repositories {
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    id("dokkasettings.gradle-enterprise")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        mavenCentral()
        google()
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
