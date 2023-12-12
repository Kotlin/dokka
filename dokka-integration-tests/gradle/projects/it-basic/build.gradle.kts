/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.kotlinSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.DokkaConfiguration
import java.net.URL

plugins {
    kotlin("jvm") version "%{KOTLIN_VERSION}%"
    id("org.jetbrains.dokka") version "%{DOKKA_VERSION}%"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:%{DOKKA_VERSION}%")
    }
}

version = "1.9.20-SNAPSHOT"

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

    val logoStylesCss = file("customResources/logo-styles.css")
    val customStyleToAddCss = file("customResources/custom-style-to-add.css")
    val customResourceSvg = file("customResources/custom-resource.svg")
    inputs.files(
        logoStylesCss,
        customStyleToAddCss,
        customResourceSvg,
    )

    pluginsMapConfiguration.set(
        mapOf(
            DokkaBase::class.qualifiedName to """
                { 
                  "customStyleSheets": [
                    "${logoStylesCss.invariantSeparatorsPath}",
                    "${customStyleToAddCss.invariantSeparatorsPath}"
                  ],
                  "customAssets": [
                    "${customResourceSvg.invariantSeparatorsPath}"
                  ]
                }
            """.trimIndent()
        )
    )
}
