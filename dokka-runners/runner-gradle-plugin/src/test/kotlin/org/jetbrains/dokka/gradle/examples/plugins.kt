/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.examples

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.dsl.DokkaMultiModuleFileLayout

fun Project.configureSomething() {
    dokka {
        formats.html {
            // TODO: this will set the same directory for `module` and for `aggregate`
            //  though, it's not really an issue, as to fall into this there is a requirement:
            //  1. module should have sources which will be documented
            //  2. module should have aggregates
            //  3. outputDirectory configuration should be set on root level
            //  Though, it's possible to fall into this in settings - TBD
            outputDirectory.set(file(""))
        }
        generation {
            moduleName.set("root")
            sourceSets {

            }
            formats.html {
                outputDirectory.set(file(""))
            }
        }
        aggregation {
            includeSubprojects()
            useMultiModule(DokkaMultiModuleFileLayout.NoCopy)
            formats.html {
                outputDirectory.set(file(""))
            }
        }
        offlineMode.set(true)

        formats.html {
            separateInheritedMembers.set(true)
        }

        plugins.versioning {
            currentVersion.set(version.toString())
        }

        aggregation.includeSubprojects()
        includeSubprojects()

        includeDocumentation("")
    }
}

fun Project.customPlugin() {
    dokka {
        plugins.custom(
            "org.example.customplugin.DokkaHtmlMermaidPlugin",
            "com.example.customplugin:dokka-mermaid-plugin:1.2.0"
        )
        plugins.custom(
            "org.example.customplugin.DokkaHtmlMermaidPlugin",
            "com.example.customplugin:dokka-mermaid-plugin:1.2.0"
        ) {
            properties {
                booleanProperty("ligthTheme", true)
            }
        }
        plugins.custom {
            pluginClassName.set("org.example.customplugin.DokkaHtmlMermaidPlugin")
            dependency("com.example.customplugin:dokka-mermaid-plugin:1.2.0")
            properties {
                booleanProperty("ligthTheme", true)
            }
        }
    }
}
