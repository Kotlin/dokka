/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "dokka"

pluginManagement {
    includeBuild("build-logic")
    includeBuild("build-settings-logic")

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
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }

        maven("https://cache-redirector.jetbrains.com/intellij-repository/releases") {
            name = "IjRepository-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies") {
            name = "IjDependencies-JBCache"
        }

        val kotlin_repo: String? by settings
        kotlin_repo?.let { maven(it) }

        //region Declare the Node.js & Yarn download repositories
        // Required by Gradle Node plugin: https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
        exclusiveContent {
            forRepository {
                ivy("https://cache-redirector.jetbrains.com/nodejs.org/dist/") {
                    name = "Nodejs-JBCache"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("org.nodejs", "node") }
                }
            }
            filter { includeGroup("org.nodejs") }
        }

        exclusiveContent {
            forRepository {
                ivy("https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn-JBCache"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("com.yarnpkg", "yarn") }
                }
            }
            filter { includeGroup("com.yarnpkg") }
        }
        //endregion
    }
}

plugins {
    id("dokkasettings")
}

includeBuild("dokka-integration-tests")
includeBuild("dokka-runners/dokka-gradle-plugin")
includeBuild("dokka-runners/runner-maven-plugin")
includeBuild("dokka-runners/runner-cli")

include(
    ":docs-developer",

    ":dokka-subprojects",
    ":dokka-subprojects:analysis-java-psi",
    ":dokka-subprojects:analysis-kotlin-api",
    ":dokka-subprojects:analysis-kotlin-descriptors",
    ":dokka-subprojects:analysis-kotlin-descriptors-compiler",
    ":dokka-subprojects:analysis-kotlin-descriptors-ide",
    ":dokka-subprojects:analysis-kotlin-symbols",
    ":dokka-subprojects:analysis-markdown-jb",
    ":dokka-subprojects:core",
    ":dokka-subprojects:core-content-matcher-test-utils",
    ":dokka-subprojects:core-test-api",
    ":dokka-subprojects:plugin-all-modules-page",
    ":dokka-subprojects:plugin-android-documentation",
    ":dokka-subprojects:plugin-base",
    ":dokka-subprojects:plugin-base-frontend",
    ":dokka-subprojects:plugin-base-test-utils",
    ":dokka-subprojects:plugin-gfm",
    ":dokka-subprojects:plugin-gfm-template-processing",
    ":dokka-subprojects:plugin-javadoc",
    ":dokka-subprojects:plugin-jekyll",
    ":dokka-subprojects:plugin-jekyll-template-processing",
    ":dokka-subprojects:plugin-kotlin-as-java",
    ":dokka-subprojects:plugin-kotlin-playground-samples",
    ":dokka-subprojects:plugin-mathjax",
    ":dokka-subprojects:plugin-templating",
    ":dokka-subprojects:plugin-versioning"
)

// This hack is required for included build support.
// The name of the published artifact is `dokka-core`, but the module is named `core`.
// For some reason, dependency substitution doesn't work in this case. Maybe we fall under one of the unsupported
// cases: https://docs.gradle.org/current/userguide/composite_builds.html#included_build_substitution_limitations.
// Should no longer be a problem once Dokka's artifacts are relocated, see #3245.
project(":dokka-subprojects:core").name = "dokka-core"
project(":dokka-subprojects:core-test-api").name = "dokka-test-api"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// OVERWRITING CATALOG VERSIONS
// for testing against the latest dev version of Analysis API
// currently, Analysis API is used only in the analysis-kotlin-symbols project
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
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

            val kotlinCompilerK2Version = providers.gradleProperty(
                "org.jetbrains.dokka.build.overrideAnalysisAPIVersion"
            ).orNull
            if (kotlinCompilerK2Version != null) {
                logger.lifecycle("Using the override version $kotlinCompilerK2Version of Analysis API")
                version("kotlin-compiler-k2", kotlinCompilerK2Version)
            }
        }
    }
}
