package org.jetbrains.dokka.gradle

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

        assertTrue(
            build.output.contains(
                """
                    --------------------------------------------------
                    Variant dokkaConfigurationElements
                    --------------------------------------------------
                    Provide Dokka Configurations files to other subprojects
                    
                    Capabilities
                        - :test:unspecified (default capability)
                    Attributes
                        - org.gradle.category = configuration
                        - org.gradle.usage    = org.jetbrains.dokka
                    
                    --------------------------------------------------
                    Variant dokkaModuleDescriptorElements
                    --------------------------------------------------
                    Provide Dokka module descriptor files to other subprojects
                    
                    Capabilities
                        - :test:unspecified (default capability)
                    Attributes
                        - org.gradle.category = module-descriptor
                        - org.gradle.usage    = org.jetbrains.dokka
                    Artifacts
                        - build/dokka/createDokkaModuleConfiguration.json (artifactType = json)
                    
                """.trimIndent()
            ),
            "expect output contains dokka variants\n\n${build.output.prependIndent("  | ")}"
        )
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

        assertTrue(
            build.output.contains(
                """
                    --------------------------------------------------
                    Configuration dokka
                    --------------------------------------------------
                    Fetch all Dokka files from all configurations in other subprojects
                    
                    Attributes
                        - org.gradle.usage = org.jetbrains.dokka
                """.trimIndent()
            ),
            "expect output contains dokka configurations\n\n${build.output.prependIndent("  | ")}"
        )
        assertTrue(
            build.output.contains(
                """
                    --------------------------------------------------
                    Configuration dokkaConfigurations
                    --------------------------------------------------
                    Fetch Dokka Configuration files from other subprojects
                    
                    Attributes
                        - org.gradle.category = configuration
                        - org.gradle.usage    = org.jetbrains.dokka
                    Extended Configurations
                        - dokka
                """.trimIndent()
            ),
            "expect output contains dokka configurations\n\n${build.output.prependIndent("  | ")}"
        )
        assertTrue(
            build.output.contains(
                """
                    --------------------------------------------------
                    Configuration dokkaModuleDescriptor
                    --------------------------------------------------
                    Fetch Dokka module descriptor files from other subprojects
                    
                    Attributes
                        - org.gradle.category = module-descriptor
                        - org.gradle.usage    = org.jetbrains.dokka
                    Extended Configurations
                        - dokka
                """.trimIndent()
            ),
            "expect output contains dokka configurations\n\n${build.output.prependIndent("  | ")}"
        )
        assertTrue(
            build.output.contains(
                """
                    --------------------------------------------------
                    Configuration dokkaRuntimeClasspath
                    --------------------------------------------------
                    Dokka generation task runtime classpath
                    
                    Attributes
                        - org.gradle.category            = library
                        - org.gradle.dependency.bundling = external
                        - org.gradle.jvm.environment     = standard-jvm
                        - org.gradle.libraryelements     = jar
                """.trimIndent()
            ),
            "expect output contains dokka configurations\n\n${build.output.prependIndent("  | ")}"
        )
        assertTrue(
            build.output.contains(
                """
                    --------------------------------------------------
                    Configuration dokkaPluginsClasspath
                    --------------------------------------------------
                    Dokka Plugins classpath
                    
                    Attributes
                        - org.gradle.category            = library
                        - org.gradle.dependency.bundling = external
                        - org.gradle.jvm.environment     = standard-jvm
                        - org.gradle.libraryelements     = jar
                    
                    -------------------------------------------------
                """.trimIndent()
            ),
            "expect output contains dokka configurations\n\n${build.output.prependIndent("  | ")}"
        )
    }
}
