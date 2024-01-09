/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.examples

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.dsl.DokkaDeclarationVisibility

// 1. Generate API specification for the proposed project
fun Project.task1() {
    plugins.apply("org.jetbrains.dokka")

    dokka {
        aggregation.includeSubprojects {
            // with newer Gradle it will be `applyDokkaPlugin = true`
            applyDokkaPlugin.set(true)
        }
    }
}

// 2. The module 'common-utils' is considered internal. Please exclude it from the documentation.
fun Project.task2() {
    plugins.apply("org.jetbrains.dokka")

    dokka {
        aggregation.includeSubprojects(exclude = listOf("common-utils")) {
            // with newer Gradle it will be `applyDokkaPlugin = true`
            applyDokkaPlugin.set(true)
        }
    }
}

// 3. Now you need to add a protected API to the generated documentation. Please change the setup so that the documentation contains protected APIs.
fun Project.task3() {
    plugins.apply("org.jetbrains.dokka")

    dokka {
        documentedVisibilities.addAll(DokkaDeclarationVisibility.PROTECTED)

        aggregation.includeSubprojects(
            exclude = listOf("common-utils")
        ) {
            // with newer Gradle it will be `applyDokkaPlugin = true`
            applyDokkaPlugin.set(true)
            inheritConfiguration.set(true)
        }
    }
}

// 4. Provide your readers with the ability to find the source code for each declaration.
fun Project.task4() {
    plugins.apply("org.jetbrains.dokka")

    dokka {
        documentedVisibilities.addAll(DokkaDeclarationVisibility.PROTECTED)
        sourceLink("https://github.com/kotlin/dokka/tree/master")

        // additional
        formats.html {
            homepageLink.set("https://github.com/kotlin/dokka")
        }

        aggregation.includeSubprojects(
            exclude = listOf("common-utils")
        ) {
            // with newer Gradle it will be `applyDokkaPlugin = true`
            applyDokkaPlugin.set(true)
            inheritConfiguration.set(true)
        }
    }
}

// 5. You want to communicate to other developers the following information about the module subprojectA:
//  "This is the documentation for the module. This text helps users of the API understand what is the purpose of the module, its structure, and usage samples."
//  Please add it to the documentation.
fun Project.task5() {
    plugins.apply("org.jetbrains.dokka")

    dokka {
        documentedVisibilities.addAll(DokkaDeclarationVisibility.PROTECTED)
        sourceLink("https://github.com/kotlin/dokka/tree/master")

        aggregation.includeSubprojects(
            exclude = listOf("common-utils")
        ) {
            // with newer Gradle it will be `applyDokkaPlugin = true`
            applyDokkaPlugin.set(true)
            inheritConfiguration.set(true)
        }
    }

    // in subprojectA:

    dokka {
        // or from file (more complex way)
        includeDocumentation(
            """
               This is the documentation for the module.
               This text helps users of the API understand what is the purpose of the module, its structure, and usage samples.
            """.trimIndent()
        )
    }
}

// TODO!!!
// 6. Now you are ready to publish your lib to the Maven central repository.
//  It requires a javadoc.jar, please generate it.
//  (You do not need to publish the library. Only generate the javadoc.jar artifact)
fun Project.task6() {
    plugins.apply("org.jetbrains.dokka")

    dokka {
        documentedVisibilities.addAll(DokkaDeclarationVisibility.PROTECTED)
        sourceLink("https://github.com/kotlin/dokka/tree/master")

        aggregation.includeSubprojects(
            exclude = listOf("common-utils")
        ) {
            // with newer Gradle it will be `applyDokkaPlugin = true`
            applyDokkaPlugin.set(true)
            inheritConfiguration.set(true)
        }
    }

// in subprojectA:

    dokka {
        // or from file (more complex way)
        includeDocumentation(
            """
               This is the documentation for the module.
               This text helps users of the API understand what is the purpose of the module, its structure, and usage samples.
            """.trimIndent()
        )
    }
}
