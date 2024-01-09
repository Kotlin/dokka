/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.examples

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.dsl.aggregation.DokkaMultiModuleFileLayout

fun Project.configureSomething() {
    dokka {
        sourceSets {

        }
        offlineMode.set(true)

        formats.html {
            separateInheritedMembers.set(true)
        }
        plugins.versioning {
            currentVersion.set(version.toString())
        }
        multiModule.includeSubprojects()

        multiModule {
            fileLayout.set(DokkaMultiModuleFileLayout.NoCopy)
        }

        includeDocumentation("")
    }
}

fun Project.rawPlugin() {
    dokka {
        plugins.custom(
            "org.example.customplugin.DokkaHtmlMermaidPlugin",
            "com.example.customplugin:dokka-mermaid-plugin:1.2.0"
        ) {
            property("ligthTheme", true)
        }
        plugins.custom("org.example.customplugin.DokkaHtmlMermaidPlugin") {
            dependency("com.example.customplugin:dokka-mermaid-plugin:1.2.0")
            property("ligthTheme", true)
        }
    }
}
