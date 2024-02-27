/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "build-logic"

pluginManagement {
    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }

        val kotlin_repo: String? by settings
        kotlin_repo?.let { maven(it) }
    }
    includeBuild("../build-settings-logic")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }

        val kotlin_repo: String? by settings
        kotlin_repo?.let { maven(it) }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
            val kotlin_version: String? = providers.gradleProperty("kotlin_version").orNull
            val kotlin_api_version: String? = providers.gradleProperty("kotlin_api_version").orNull ?: providers.gradleProperty("org.jetbrains.dokka.kotlinLanguageLevel").orNull
            val kotlin_language_version: String? = providers.gradleProperty("kotlin_language_version").orNull ?: providers.gradleProperty("org.jetbrains.dokka.kotlinLanguageLevel").orNull
            if (kotlin_version != null) {
                version("gradlePlugin-kotlin", kotlin_version)
                version("bta-kotlin-compiler", kotlin_version)
            }
            if (kotlin_language_version != null) {
                println("Configured LanguageVersion $kotlin_language_version")
                version("bta-kotlin-language", kotlin_language_version)
            }

            if (kotlin_api_version != null) {
                println("Configured ApiVersion $kotlin_api_version")
                version("bta-kotlin-api", kotlin_api_version)
            }
        }
    }
}

plugins {
    id("dokkasettings")
}
