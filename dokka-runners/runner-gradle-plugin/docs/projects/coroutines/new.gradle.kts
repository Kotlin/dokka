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
    configurePathsaver()
    condigureDokkaSetup()
    configureExternalLinks()
}

// Setup top-level with templates
dokka {
    setupDokkaTemplatesDir()

    plugin("org.jetbrains.kotlinx:dokka-pathsaver-plugin:$knit_version")
}

// Dependencies for Knit processing: Knit plugin to work with Dokka
private fun Project.configurePathsaver() {
    dokka {
        plugin("org.jetbrains.kotlinx:dokka-pathsaver-plugin:$knit_version")
    }
}

// Configure Dokka setup
private fun Project.condigureDokkaSetup() {
    dokka {
        setupDokkaTemplatesDir()
        suppressInheritedMembers.set(true)
        includedDocumentation.from("README.md")

        externalLinkToJdk(version = 11)
        externalLinkToStdlib(enabled = false)
        externalDocumentationLinkFrom(
            "https://kotlinlang.org/api/latest/jvm/stdlib/",
            rootDir.resolve("site/stdlib.package.list")
        )

        sourceLink("https://github.com/kotlin/kotlinx.coroutines/tree/master")
    }
}

// TODO[whyoleg]: why is this needed?
private fun Project.configureExternalLinks() {
    dokka {
        externalDocumentationLinkFrom(coreModuleDocsUrl, coreModuleDocsPackageList)
    }
}

/**
 * Setups Dokka templates. While this directory is empty in our repository,
 * 'kotlinlang' build pipeline adds templates there when preparing our documentation
 * to be published on kotlinlang.
 *
 * See:
 * - Template setup: https://github.com/JetBrains/kotlin-web-site/blob/master/.teamcity/builds/apiReferences/kotlinx/coroutines/KotlinxCoroutinesPrepareDokkaTemplates.kt
 * - Templates repository: https://github.com/JetBrains/kotlin-web-site/tree/master/dokka-templates
 */
private fun DokkaExtension.setupDokkaTemplatesDir() {
    html {
        templatesDir.set(project.rootDir.resolve("dokka-templates"))
    }
}
