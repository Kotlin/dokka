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
                
                repositories {
                  maven(file("$testMavenRepoDir"))
                  mavenCentral()
                }
            """.trimIndent()
        }.runner
            .withArguments("tasks")
            .build()

        println(build.output.prependIndent("   | "))

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
