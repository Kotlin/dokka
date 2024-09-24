/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.api.initialization.resolve.RepositoriesMode.PREFER_SETTINGS

rootProject.name = "build-settings-logic"

pluginManagement {
    repositories {
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") {
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }
}

buildscript {
    dependencies {
        // Fetch the build-settings-logic's own convention plugin, because
        // Gradle requires that all projects have consistent build cache settings.
        classpath(files("libs/build-settings-logic.jar"))
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = PREFER_SETTINGS
    repositories {
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") {
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

pluginManager.apply(Dokkasettings_gradleEnterprisePlugin::class)
