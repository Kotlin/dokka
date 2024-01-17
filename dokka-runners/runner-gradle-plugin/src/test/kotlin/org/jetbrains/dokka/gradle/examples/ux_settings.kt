/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.examples

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.jetbrains.dokka.gradle.dsl.DokkaDeclarationVisibility

// no hacks in dokka plugin
// everything in `allprojects` lambda can then be easily converted to convention plugin

// 1. Generate API specification for the proposed project
fun settings_task1(
    rootProject: Project,
    settings: Settings
) {
    rootProject.run {
        plugins.apply("org.jetbrains.dokka")

        dokka {
            includeSubprojects()
        }
    }

    settings.run {
        // will apply plugin automatically to all projects with kotlin plugin (jvm, mpp, android)
        plugins.apply("org.jetbrains.dokka")
    }
}

// 2. The module 'common-utils' is considered internal. Please exclude it from the documentation.
fun settings_task2(
    rootProject: Project,
    settings: Settings
) {
    rootProject.run {
        plugins.apply("org.jetbrains.dokka")

        dokka {
            // this will exclude from generated multi-module documentation,
            // but there will be still dokka applied to `common-utils` - it's fine, I think
            includeSubprojects(exclude = listOf("common-utils"))
        }
    }
    settings.run {
        // will apply plugin automatically to all projects with kotlin plugin (jvm, mpp, android)
        plugins.apply("org.jetbrains.dokka")
    }
}

// 3. Now you need to add a protected API to the generated documentation. Please change the setup so that the documentation contains protected APIs.
fun settings_task3(
    rootProject: Project,
    settings: Settings
) {
    rootProject.run {
        plugins.apply("org.jetbrains.dokka")

        dokka {
            // this will exclude from generated multi-module documentation,
            // but there will be still dokka applied to `common-utils` - it's fine, I think
            includeSubprojects(exclude = listOf("common-utils"))
        }
    }

    settings.run {
        // will apply plugin automatically to all projects with kotlin plugin (jvm, mpp, android)
        plugins.apply("org.jetbrains.dokka")

        dokka {
            documentedVisibilities.addAll(DokkaDeclarationVisibility.PROTECTED)
        }
    }
}

// 4. Provide your readers with the ability to find the source code for each declaration.
fun settings_task4(
    rootProject: Project,
    settings: Settings
) {
    rootProject.run {
        plugins.apply("org.jetbrains.dokka")

        dokka {
            // this will exclude from generated multi-module documentation,
            // but there will be still dokka applied to `common-utils` - it's fine, I think
            includeSubprojects(exclude = listOf("common-utils"))
        }
    }

    settings.run {
        // will apply plugin automatically to all projects with kotlin plugin (jvm, mpp, android)
        plugins.apply("org.jetbrains.dokka")

        dokka {
            documentedVisibilities.addAll(DokkaDeclarationVisibility.PROTECTED)
            sourceLink("https://github.com/kotlin/dokka/tree/master")

            // just to show what's possible
            formats.html {
                homepageLink.set("https://github.com/kotlin/dokka")
            }
        }
    }
}

// 5. You want to communicate to other developers the following information about the module subprojectA:
//  "This is the documentation for the module. This text helps users of the API understand what is the purpose of the module, its structure, and usage samples."
//  Please add it to the documentation.
fun settings_task5(
    rootProject: Project,
    settings: Settings
) {
    rootProject.run {
        plugins.apply("org.jetbrains.dokka")

        dokka {
            // this will exclude from generated multi-module documentation,
            // but there will be still dokka applied to `common-utils` - it's fine, I think
            includeSubprojects(exclude = listOf("common-utils"))
        }
    }

    settings.run {
        // will apply plugin automatically to all projects with kotlin plugin (jvm, mpp, android)
        plugins.apply("org.jetbrains.dokka")

        dokka {
            documentedVisibilities.addAll(DokkaDeclarationVisibility.PROTECTED)
            sourceLink("https://github.com/kotlin/dokka/tree/master")

            // just to show what's possible
            formats.html {
                homepageLink.set("https://github.com/kotlin/dokka")
            }

            // or just in subprojectA build file
            // TODO: what should we match here
            perModule(":subprojectA") {
                // or from file (more complex way)
                includeDocumentation(
                    """
                       This is the documentation for the module.
                       This text helps users of the API understand what is the purpose of the module, its structure, and usage samples.
                    """.trimIndent()
                )
            }
        }
    }
}
