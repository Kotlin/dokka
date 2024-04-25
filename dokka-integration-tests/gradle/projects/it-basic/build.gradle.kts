/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.kotlinSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.DokkaConfiguration
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${providers.gradleProperty("dokka_it_dokka_version").get()}")
    }
}

version = "2.0.20-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test-junit"))
}

tasks.withType<DokkaTask>().configureEach {
    moduleName.set("Basic Project")
    dokkaSourceSets {
        configureEach {
            documentedVisibilities.set(
                setOf(DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PROTECTED)
            )
            suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
            perPackageOption {
                matchingRegex.set("it.suppressedByPackage.*")
                suppress.set(true)
            }
            perPackageOption {
                matchingRegex.set("it.overriddenVisibility.*")
                documentedVisibilities.set(
                    setOf(DokkaConfiguration.Visibility.PRIVATE)
                )
            }
            sourceLink {
                localDirectory.set(file("src/main"))
                remoteUrl.set(
                    URL(
                        "https://github.com/Kotlin/dokka/tree/master/" +
                                "dokka-integration-tests/gradle/projects/it-basic/src/main"
                    )
                )
            }
        }

        register("myTest") {
            kotlinSourceSet(kotlin.sourceSets["test"])
        }
    }
    suppressObviousFunctions.set(false)

    // register resources as inputs for Gradle up-to-date checks
    val customResourcesDir = layout.projectDirectory.dir("customResources")
    inputs.dir(customResourcesDir)
        .withPropertyName("customResourcesDir")
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .normalizeLineEndings()

    val logoStylesCss = customResourcesDir.file("logo-styles.css").asFile.invariantSeparatorsPath
    val customStyleToAddCss = customResourcesDir.file("custom-style-to-add.css").asFile.invariantSeparatorsPath
    val customResourceSvg = customResourcesDir.file("custom-resource.svg").asFile.invariantSeparatorsPath

    pluginsMapConfiguration.set(mapOf(DokkaBase::class.qualifiedName to """
            { 
                "customStyleSheets": [
                    "$logoStylesCss",
                    "$customStyleToAddCss"
                ],
                "customAssets" : [
                    "$customResourceSvg"
                ]
            }
        """.trimIndent()
    ))
}
