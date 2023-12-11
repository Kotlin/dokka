/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage")

rootProject.name = "dokka-subprojects"

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
        google()

        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")

        maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-third-party-dependencies")

        // Declare the Node.js & Yarn download repositories
        // Required by Gradle Node plugin: https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
        exclusiveContent {
            forRepository {
                ivy("https://cache-redirector.jetbrains.com/nodejs.org/dist/") {
                    name = "Node Distributions at $url"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("org.nodejs", "node") }
                }
            }
            filter { includeGroup("org.nodejs") }
        }

        exclusiveContent {
            forRepository {
                ivy("https://github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn Distributions at $url"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("com.yarnpkg", "yarn") }
                }
            }
            filter { includeGroup("com.yarnpkg") }
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

plugins {
    `gradle-enterprise`
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

include(
    ":analysis-java-psi",
    ":analysis-kotlin-api",
    ":analysis-kotlin-descriptors",
    ":analysis-kotlin-descriptors-compiler",
    ":analysis-kotlin-descriptors-ide",
    ":analysis-kotlin-symbols",
    ":analysis-markdown-jb",
    ":core",
    ":dokka-core",
    ":core-content-matcher-test-utils",
    ":core-test-api",
    ":plugin-all-modules-page",
    ":plugin-android-documentation",
    ":plugin-base",
    ":plugin-base-frontend",
    ":plugin-base-test-utils",
    ":plugin-gfm",
    ":plugin-gfm-template-processing",
    ":plugin-javadoc",
    ":plugin-jekyll",
    ":plugin-jekyll-template-processing",
    ":plugin-kotlin-as-java",
    ":plugin-mathjax",
    ":plugin-templating",
    ":plugin-versioning",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
