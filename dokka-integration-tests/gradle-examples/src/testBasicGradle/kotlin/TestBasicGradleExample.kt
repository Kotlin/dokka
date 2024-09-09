/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.examples

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.it.gradle.loadConfigurationCacheReportData
import org.jetbrains.dokka.it.systemProperty
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.*

class TestBasicGradleExample {

    val exampleProject = initProject(projectTestTempDir)

    @Test
    fun `check name`() {
        exampleProjectDir.name shouldBe "basic-gradle-example"
    }

    @Test
    fun `expect DGP can generate HTML`() {
        exampleProject.runner
            .addArguments(
                ":dokkaGenerate",
                "--stacktrace",
            )
            .forwardOutput()
            .build {
                output shouldContain "BUILD SUCCESSFUL"
            }

        val exampleDataDir = expectedDataDir.resolve("html")
        val dokkaHtmlDir = exampleProject.projectDir.resolve("build/dokka/html")

        withClue("expect file trees are the same") {
            val expectedFileTree = exampleDataDir.toTreeString()
            val actualFileTree = dokkaHtmlDir.toTreeString()
            println((actualFileTree to expectedFileTree).sideBySide())
            expectedFileTree shouldBe actualFileTree
        }

        withClue({
            """
            expect directories are the same
                dokkaHtmlDir   ${dokkaHtmlDir.walk().toList()}
                exampleDataDir ${exampleDataDir.walk().toList()}
            """.trimIndent()
        }) {
            dokkaHtmlDir.shouldHaveSameStructureAs(exampleDataDir, skipEmptyDirs = true)
            dokkaHtmlDir.shouldHaveSameStructureAndContentAs(exampleDataDir, skipEmptyDirs = true)
        }
    }

    @Nested
    @DisplayName("Gradle caching")
    inner class GradleCaching {
        @Test
        fun `expect DGP is compatible with Gradle Build Cache`() {
            exampleProject.runner
                .addArguments(
                    ":dokkaGenerate",
                    "--stacktrace",
                )
                .forwardOutput()
                .build {
                    output shouldContain "BUILD SUCCESSFUL"
                }

            exampleProject.runner
                .addArguments(
                    ":dokkaGenerate",
                    "--stacktrace",
                    "--build-cache",
                )
                .forwardOutput()
                .build {
                    output shouldContainAll listOf(
                        "> Task :dokkaGenerateModuleHtml UP-TO-DATE",
                        "> Task :dokkaGenerate UP-TO-DATE",
                        "BUILD SUCCESSFUL",
                        // expect "1 executed" because :checkKotlinGradlePluginConfigurationErrors always runs (fixed KGP in 2.0?)
                        "2 actionable tasks: 2 up-to-date",
                    )
                }
        }

        @Test
        fun `expect DGP is compatible with Gradle Configuration Cache`() {

            // delete old configuration cache results and reports
            exampleProject.findFiles {
                val isCCDir = it.invariantSeparatorsPath.endsWith(".gradle/configuration-cache")
                val isCCReportDir = it.invariantSeparatorsPath.endsWith("build/reports/configuration-cache")
                it.isDirectory && (isCCReportDir || isCCDir)
            }.forEach { it.deleteRecursively() }

            val configCacheRunner: GradleRunner =
                exampleProject.runner
                    .addArguments(
                        ":dokkaGenerate",
                        "--stacktrace",
                        "--configuration-cache",
                    )
                    .forwardOutput()

            //first build should store the configuration cache
            configCacheRunner.build {
                output shouldContain "BUILD SUCCESSFUL"
                output shouldContain "Configuration cache entry stored"
                output shouldNotContain "problems were found storing the configuration cache"
            }

            val ccReport = exampleProject.loadConfigurationCacheReportData().shouldNotBeNull()

            ccReport.asClue {
                it.totalProblemCount shouldBe 0
            }

            // second build should reuse the configuration cache
            configCacheRunner.build {
                output shouldContain "BUILD SUCCESSFUL"
                output shouldContain "Configuration cache entry reused"
            }
        }
    }

    companion object {
        private val projectTestTempDir by systemProperty(::Path)
        private val exampleProjectDir by systemProperty(::Path)
        private val expectedDataDir by systemProperty(::Path)

        private fun initProject(
            destinationDir: Path,
        ): GradleProjectTest {
            destinationDir.deleteRecursively()

            return GradleProjectTest(destinationDir).apply {
                exampleProjectDir.copyToRecursively(projectDir, overwrite = true, followLinks = false)

                gradleProperties {
                    dokka {
                        v2Plugin = true
                        v2MigrationHelpers = false
                        v2PluginNoWarn = true
                    }
                }

                updateSettingsRepositories()
            }
        }
    }
}
