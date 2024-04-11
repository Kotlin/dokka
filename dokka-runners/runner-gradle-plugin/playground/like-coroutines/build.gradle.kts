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

    externalLinkToJdk(jdkVersion = 11)
    externalLinkToKotlinStdlib(enabled = false)
    externalLink("https://kotlinlang.org/api/latest/jvm/stdlib/") {
        packageListLocation = rootDir.resolve("site/stdlib.package.list").toURI()
    }
    externalLink("https://kotlinlang.org/api/kotlinx.coroutines/$coreModule/") {
        packageListLocation = rootDir.resolve("kotlinx-coroutines-core/build/dokka/htmlPartial/package-list").toURI()
    }

    aggregation {
        excludeProjects(projectsWithoutDokka)
    }
}

// README.md is different in each module
subprojects {
    plugins.apply("org.jetbrains.dokka")
    dokka {
        includedDocumentation.from("README.md")
    }
}
