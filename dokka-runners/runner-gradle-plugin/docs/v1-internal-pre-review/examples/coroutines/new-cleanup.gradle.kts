/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// convention plugin applied to root project

plugins {
    id("org.jetbrains.dokka")
}

val knit_version: String by project
private val projetsWithoutDokka = unpublished + "kotlinx-coroutines-bom" + jdk8ObsoleteModule
private val coreModuleDocsUrl = "https://kotlinlang.org/api/kotlinx.coroutines/$coreModule/"
private val coreModuleDocsPackageList = "$projectDir/kotlinx-coroutines-core/build/dokka/htmlPartial/package-list"

configure(subprojects.filterNot { projetsWithoutDokka.contains(it.name) }) {
    apply(plugin = "org.jetbrains.dokka")
    dokka {
        sharedDokkaConfiguration()

        suppressInheritedMembers.set(true)
        includedDocumentation.from("README.md")

        externalLinkToJdk(version = 11)
        externalLinkToStdlib(enabled = false)
        externalDocumentationLinkFrom(
            "https://kotlinlang.org/api/latest/jvm/stdlib/",
            rootDir.resolve("site/stdlib.package.list")
        )
        externalDocumentationLinkFrom(coreModuleDocsUrl, coreModuleDocsPackageList)

        sourceLink("https://github.com/kotlin/kotlinx.coroutines/tree/master")

    }
}

dokka {
    sharedDokkaConfiguration()
}

// for both aggregate and subprojects
fun DokkaExtension.sharedDokkaConfiguration() {
    html {
        templatesDir.set(project.rootDir.resolve("dokka-templates"))
    }
    plugin("org.jetbrains.kotlinx:dokka-pathsaver-plugin:$knit_version")
}
