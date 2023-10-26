/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage")

rootProject.name = "dokka-subprojects"

pluginManagement {
    includeBuild("../build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
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

include(
    ":analysis-java-psi",
    ":analysis-kotlin-api",
    ":analysis-kotlin-descriptors",
    ":analysis-kotlin-descriptors-compiler",
    ":analysis-kotlin-descriptors-ide",
    ":analysis-kotlin-symbols",
    ":analysis-markdown-jb",
    ":core",
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

// This hack is required for included build support.
// The name of the published artifact is `dokka-core`, but the module is named `core`.
// For some reason, dependency substitution doesn't work in this case. Maybe we fall under one of the unsupported
// cases: https://docs.gradle.org/current/userguide/composite_builds.html#included_build_substitution_limitations.
// Should no longer be a problem once Dokka's artifacts are relocated, see #3245.
project(":core").name = "dokka-core"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
