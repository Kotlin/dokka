/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

//rootProject.name = "dokka-subprojects"
//
//pluginManagement {
//    includeBuild("../build-logic")
//    includeBuild("../build-settings-logic")
//
//    repositories {
//        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") {
//            name = "MavenCentral-JBCache"
//        }
//        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
//            name = "GradlePluginPortal-JBCache"
//        }
//    }
//}
//
//dependencyResolutionManagement {
//    @Suppress("UnstableApiUsage")
//    repositories {
//        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide") {
//            name = "KotlinIde-JBCache"
//        }
//        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies") {
//            name = "KotlinIdePluginDependencies-JBCache"
//        }
//
//        maven("https://cache-redirector.jetbrains.com/intellij-repository/releases") {
//            name = "IjRepository-JBCache"
//        }
//        maven("https://cache-redirector.jetbrains.com/intellij-third-party-dependencies") {
//            name = "IjThirdParty-JBCache"
//        }
//
//        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") {
//            name = "MavenCentral-JBCache"
//        }
//        maven("https://cache-redirector.jetbrains.com/dl.google.com.android.maven2") {
//            name = "Google-JBCache"
//        }
//
//        //region Declare the Node.js & Yarn download repositories
//        // Required by Gradle Node plugin: https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
//        exclusiveContent {
//            forRepository {
//                ivy("https://cache-redirector.jetbrains.com/nodejs.org/dist/") {
//                    name = "Node Distributions at $url"
//                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
//                    metadataSources { artifact() }
//                    content { includeModule("org.nodejs", "node") }
//                }
//            }
//            filter { includeGroup("org.nodejs") }
//        }
//
//        exclusiveContent {
//            forRepository {
//                ivy("https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download") {
//                    name = "Yarn Distributions at $url"
//                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
//                    metadataSources { artifact() }
//                    content { includeModule("com.yarnpkg", "yarn") }
//                }
//            }
//            filter { includeGroup("com.yarnpkg") }
//        }
//        //endregion
//    }
//
//    versionCatalogs {
//        create("libs") {
//            from(files("../gradle/libs.versions.toml"))
//
//            // OVERWRITING CATALOG VERSIONS
//            // for testing against the latest dev version of Analysis API
//            // currently, Analysis API is used only in the analysis-kotlin-symbols project
//            val kotlinCompilerK2Version = providers.gradleProperty(
//                "org.jetbrains.dokka.build.overrideAnalysisAPIVersion"
//            ).orNull
//            if (kotlinCompilerK2Version != null) {
//                logger.lifecycle("Using the override version $kotlinCompilerK2Version of Analysis API")
//                version("kotlin-compiler-k2", kotlinCompilerK2Version)
//            }
//        }
//    }
//}
//
//plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
//    id("dokkasettings.gradle-enterprise")
//    id("dokkasettings.build-cache")
//}
//
////includeBuild("dokka-subprojects")
////includeBuild("dokka-integration-tests")
////includeBuild("dokka-runners/dokka-gradle-plugin")
////includeBuild("dokka-runners/runner-gradle-plugin-classic")
////includeBuild("dokka-runners/runner-maven-plugin")
////includeBuild("dokka-runners/runner-cli")
//
//include(
//    ":docs-developer",
//
////    ":dokka-subprojects",
//    ":analysis-java-psi",
//    ":analysis-kotlin-api",
//    ":analysis-kotlin-descriptors",
//    ":analysis-kotlin-descriptors-compiler",
//    ":analysis-kotlin-descriptors-ide",
//    ":analysis-kotlin-symbols",
//    ":analysis-markdown-jb",
//    ":dokka-core",
//    ":dokka-test-api",
//    ":core-content-matcher-test-utils",
//    ":core-test-api",
//    ":plugin-all-modules-page",
//    ":plugin-android-documentation",
//    ":plugin-base",
//    ":plugin-base-frontend",
//    ":plugin-base-test-utils",
//    ":plugin-gfm",
//    ":plugin-gfm-template-processing",
//    ":plugin-javadoc",
//    ":plugin-jekyll",
//    ":plugin-jekyll-template-processing",
//    ":plugin-kotlin-as-java",
//    ":plugin-mathjax",
//    ":plugin-templating",
//    ":plugin-versioning",
//)
//
//// This hack is required for included build support.
//// The name of the published artifact is `dokka-core`, but the module is named `core`.
//// For some reason, dependency substitution doesn't work in this case. Maybe we fall under one of the unsupported
//// cases: https://docs.gradle.org/current/userguide/composite_builds.html#included_build_substitution_limitations.
//// Should no longer be a problem once Dokka's artifacts are relocated, see #3245.
////project("core").name = "dokka-core"
////project("core-test-api").name = "dokka-test-api"
//
//enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
