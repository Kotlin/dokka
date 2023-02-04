package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.utils.buildGradleKts
import org.jetbrains.dokka.gradle.utils.gradleKtsProjectTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DokkaPluginFunctionalTest {

    @Test
    fun `expect Dokka Plugin creates Dokka tasks`() {
        val build = gradleKtsProjectTest {
            buildGradleKts = """
                plugins {
                    id("org.jetbrains.dokka2") version "2.0.0"
                }
            """.trimIndent()
        }.runner
            .withArguments("tasks")
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

    @Test
    fun `expect Dokka Plugin creates Dokka outgoing variants`() {
        val build = gradleKtsProjectTest {
            buildGradleKts = """
                plugins {
                    id("org.jetbrains.dokka2") version "2.0.0"
                }
            """.trimIndent()
        }.runner
            .withArguments("outgoingVariants")
            .build()

        withClue("dokkaConfigurationElements") {
            build.output shouldContain """
                --------------------------------------------------
                Variant dokkaConfigurationElements
                --------------------------------------------------
                Provide Dokka Generator Configuration files to other subprojects

                Capabilities
                    - :test:unspecified (default capability)
                Attributes
                    - org.jetbrains.dokka.base     = dokka
                    - org.jetbrains.dokka.category = configuration
                Artifacts
            """.trimIndent()
        }

        withClue("dokkaGeneratorProvider") {
            build.output shouldContain """
                --------------------------------------------------
                Variant dokkaGeneratorClasspathElements
                --------------------------------------------------
                Provide Dokka Generator classpath to other subprojects

                Capabilities
                    - :test:unspecified (default capability)
                Attributes
                    - org.gradle.category            = library
                    - org.gradle.dependency.bundling = external
                    - org.gradle.jvm.environment     = standard-jvm
                    - org.gradle.libraryelements     = jar
                    - org.gradle.usage               = java-runtime
                    - org.jetbrains.dokka.base       = dokka
                    - org.jetbrains.dokka.category   = generator-classpath
            """.trimIndent()
        }

        withClue("dokkaModuleDescriptorElements") {
            build.output shouldContain """
                --------------------------------------------------
                Variant dokkaModuleDescriptors
                --------------------------------------------------
                Provide Dokka Module descriptor files to other subprojects

                Capabilities
                    - :test:unspecified (default capability)
                Attributes
                    - org.jetbrains.dokka.base     = dokka
                    - org.jetbrains.dokka.category = module-descriptor
                Artifacts
            """.trimIndent()
        }
    }

    @Test
    fun `expect Dokka Plugin creates Dokka resolvable configurations`() {
        val build = gradleKtsProjectTest {
            buildGradleKts = """
                plugins {
                    id("org.jetbrains.dokka2") version "2.0.0"
                }
            """.trimIndent()
        }.runner
            .withArguments("resolvableConfigurations")
            .build()

        withClue("Configuration dokka") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokka
                --------------------------------------------------
                Fetch all Dokka files from all configurations in other subprojects
                
                Attributes
                    - org.jetbrains.dokka.base = dokka
            """.trimIndent()
        }

        withClue("dokkaConfigurations") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokkaConfiguration
                --------------------------------------------------
                Fetch Dokka Generator Configuration files from other subprojects
                
                Attributes
                    - org.jetbrains.dokka.base     = dokka
                    - org.jetbrains.dokka.category = configuration
                Extended Configurations
                    - dokka
            """.trimIndent()
        }

        withClue("dokkaModuleDescriptor") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokkaModule
                --------------------------------------------------
                Fetch Dokka Module descriptor files from other subprojects
                
                Attributes
                    - org.jetbrains.dokka.base     = dokka
                    - org.jetbrains.dokka.category = module-descriptor
                
                """.trimIndent()
        }

        withClue("dokkaPlugin") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokkaPlugin
                --------------------------------------------------
                Dokka Plugins classpath
                
                Attributes
                    - org.gradle.category            = library
                    - org.gradle.dependency.bundling = external
                    - org.gradle.jvm.environment     = standard-jvm
                    - org.gradle.libraryelements     = jar
                    - org.gradle.usage               = java-runtime
                    - org.jetbrains.dokka.base       = dokka
                    - org.jetbrains.dokka.category   = plugins-classpath
                Extended Configurations
                    - dokka
            """.trimIndent()
        }

        withClue("dokkaGenerator") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokkaGeneratorClasspath
                --------------------------------------------------
                Dokka Generator runtime classpath - will be used in Dokka Worker. Should contain all plugins, and transitive dependencies, so Dokka Worker can run.
                
                Attributes
                    - org.gradle.category            = library
                    - org.gradle.dependency.bundling = external
                    - org.gradle.jvm.environment     = standard-jvm
                    - org.gradle.libraryelements     = jar
                    - org.gradle.usage               = java-runtime
                    - org.jetbrains.dokka.base       = dokka
                    - org.jetbrains.dokka.category   = generator-classpath
                Extended Configurations
                    - dokka
                    - dokkaPlugin
            """.trimIndent()
        }
    }
}
