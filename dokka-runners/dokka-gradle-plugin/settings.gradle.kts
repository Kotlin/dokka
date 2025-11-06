/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "dokka-gradle-plugin"

pluginManagement {
    includeBuild("../../build-logic")
    includeBuild("../../build-settings-logic")

    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }
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
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }
        google {
            setUrl("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2")
            name = "Google-JBCache"
            mavenContent {
                // https://github.com/gradle/gradle/issues/35562
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("androidx")
            }
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
