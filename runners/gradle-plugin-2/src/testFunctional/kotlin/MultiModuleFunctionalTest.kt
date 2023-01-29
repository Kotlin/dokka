package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.gradle.utils.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MultiModuleFunctionalTest {

    // TODO WIP - need to fix error where Dokka tries to create a Markdown class, but it can't
    @Test
    fun `expect Dokka Plugin creates Dokka tasks`() {
        val build = gradleKtsProjectTest {

            settingsGradleKts += """
                
                include(":subproject")
            """.trimIndent()

            buildGradleKts = """
                plugins {
                    id("org.jetbrains.dokka2") version "2.0.0"
                }
                
                dependencies {
                    dokka(":subproject")
                }
            """.trimIndent()

            createKtsFile("subproject/build.gradle.kts", """
                import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
                import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
                
                plugins {
                    `embedded-kotlin`
                    id("org.jetbrains.dokka2") version "2.0.0"
                }
                
                tasks.withType<DokkaConfigurationTask>().configureEach {
                    sourceSets.add(
                        DokkaConfigurationKxs.DokkaSourceSetKxs(
                            displayName = "DEFAULT",
                            sourceSetID = org.jetbrains.dokka.DokkaSourceSetID("DEFAULT", "DEFAULT"),
                            classpath = emptyList(),
                            sourceRoots = setOf(file("src/main/kotlin")),
                            dependentSourceSets = emptySet(),
                            samples = emptySet(),
                            includes = emptySet(),
                            documentedVisibilities = emptySet(),
                            reportUndocumented = false,
                            skipEmptyPackages = true,
                            skipDeprecated = false,
                            jdkVersion = 8,
                            sourceLinks = emptySet(),
                            perPackageOptions = emptyList(),
                            externalDocumentationLinks = emptySet(),
                            languageVersion = null,
                            apiVersion = null,
                            noStdlibLink = false,
                            noJdkLink = false,
                            suppressedFiles = emptySet(),
                            analysisPlatform = org.jetbrains.dokka.Platform.DEFAULT
                        )
                    )
                }

            """.trimIndent())

            createKotlinFile("subproject/src/main/kotlin/Hello.kt", """
                package com.project.hello
                
                /** The Hello class */
                class Hello {
                    /** prints `Hello` to the console */  
                    fun sayHello() = println("Hello")
                }
            """.trimIndent())

        }.runner
            .withArguments(":subproject:dokkaGenerate","--stacktrace", "--info")
            .forwardOutput()
            .build()

        assertTrue(
            build.output.contains(
                """
                    Dokka tasks
                    -----------
                    createDokkaConfiguration - Assembles Dokka a configuration file, to be used when executing Dokka
                    createDokkaModuleConfiguration
                    dokkaGenerate
                """.trimIndent()
            ),
            "expect output contains dokka tasks\n\n${build.output.prependIndent("  | ")}"
        )
    }

}
