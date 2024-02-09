/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// settings.gradle.kts

plugins {
    id("org.jetbrains.dokka")
}

val knit_version: String by settings

private val coreModuleDocsUrl = "https://kotlinlang.org/api/kotlinx.coroutines/$coreModule/"
private val coreModuleDocsPackageList = "$rootDir/kotlinx-coroutines-core/build/dokka/htmlPartial/package-list"

dokka {
    html {
        templatesDir.set(rootDir.resolve("dokka-templates"))
    }
    plugin("org.jetbrains.kotlinx:dokka-pathsaver-plugin:$knit_version")

    suppressInheritedMembers.set(true)

    includedDocumentation.from("README.md") // TODO[whyoleg]: is it possible?

    externalLinkToJdk(version = 11)
    externalLinkToStdlib(enabled = false)
    externalDocumentationLinkFrom(
        "https://kotlinlang.org/api/latest/jvm/stdlib/",
        rootDir.resolve("site/stdlib.package.list")
    )
    externalDocumentationLinkFrom(coreModuleDocsUrl, coreModuleDocsPackageList)

    sourceLink("https://github.com/kotlin/kotlinx.coroutines/tree/master")
}
