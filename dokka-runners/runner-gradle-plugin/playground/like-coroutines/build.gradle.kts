/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.dokka")
}

// adopted from current kotlinx.coroutines setup

// defined somewhere else
val unpublished = listOf<String>()
val jdk8ObsoleteModule = listOf<String>()
val coreModule = ""
val knit_version: String = ""

val projectsWithoutDokka = unpublished + "kotlinx-coroutines-bom" + jdk8ObsoleteModule

dokka {
    suppressInheritedMembers = true
    sourceLink("https://github.com/kotlin/kotlinx.coroutines/tree/master")

    plugin("org.jetbrains.kotlinx:dokka-pathsaver-plugin:$knit_version")

    html {
        templatesDirectory = rootDir.resolve("dokka-templates")
    }

    documentationLinks {
        linkToJdk(jdkVersion = 11)
        linkToKotlinStdlib(enabled = false)
        externalLink("https://kotlinlang.org/api/latest/jvm/stdlib/") {
            packageListFile(rootDir.resolve("site/stdlib.package.list"))
        }
        externalLink("https://kotlinlang.org/api/kotlinx.coroutines/$coreModule/") {
            packageListFile(rootDir.resolve("kotlinx-coroutines-core/build/dokka/htmlPartial/package-list"))
        }
    }

    aggregation {
        applyPluginToIncludedProjects = true
        includeAllProjects {
            exclude(projectsWithoutDokka)
        }
    }
}

// README.md is different in each module
subprojects {
    dokka {
        includedDocumentation.from("README.md")
    }
}
