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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

class TestBasicGradleExample {

    val exampleProject = initProject(projectTestTempDir)

    @Test
    fun `check name`() {
        exampleProjectDir.name shouldBe "basic-gradle-example"
    }

    @Nested
    @DisplayName("verify generated HTML")
    inner class Html {
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
        }

        @Nested
        @DisplayName("expect Dokka HTML matches expected data")
        inner class ExpectedData {
            private val exampleDataDir = expectedDataDir.resolve("html")
            private val dokkaHtmlDir = exampleProject.projectDir.resolve("build/dokka/html")

            @Test
            fun `expect file trees are the same`() {
                val expectedFileTree = exampleDataDir.toTreeString()
                val actualFileTree = dokkaHtmlDir.toTreeString()
                println((actualFileTree to expectedFileTree).sideBySide())
                expectedFileTree shouldBe actualFileTree
            }

            @Test
            fun `expect directories are the same`() {
                withClue(
                    "dokkaHtmlDir[${dokkaHtmlDir.walk().toList()}], " +
                            "exampleDataDir[${exampleDataDir.walk().toList()}]"
                ) {
                    dokkaHtmlDir.shouldHaveSameStructureAs(exampleDataDir, skipEmptyDirs = true)
                    dokkaHtmlDir.shouldHaveSameStructureAndContentAs(exampleDataDir, skipEmptyDirs = true)
                }
            }
        }
    }

    @Nested
    @DisplayName("Gradle caching")
    inner class GradleCaching {
        @Test
        fun `expect DGP is compatible with Gradle Build Cache`() {
            exampleProject.runner
                .addArguments(
                    "clean",
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
            //.forwardOutput()

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
        private val projectTestTempDir by org.jetbrains.dokka.it.systemProperty(::Path)
        private val exampleProjectDir by org.jetbrains.dokka.it.systemProperty(::Path)
        private val expectedDataDir by org.jetbrains.dokka.it.systemProperty(::Path)

        private fun initProject(
            destinationDir: Path,
        ): GradleProjectTest {

            return GradleProjectTest(destinationDir).apply {
                exampleProjectDir.copyToRecursively(projectDir, overwrite = true, followLinks = false)

                gradleProperties {
                    dokka {
                        v2Plugin = true
                        v2MigrationHelpers = false
                        v2PluginNoWarn = true
                    }
                }

                projectDir.walk()
                    .filter { it.name == "settings.gradle.kts" }
                    .forEach { p ->
                        val repoLine = p.useLines { it.firstOrNull { l -> l.trim() == "repositories {" } }
                            ?: return@forEach
                        val ind = repoLine.substringBefore("repositories {")
                        p.writeText(
                            p.readText().replace(
                                "repositories {",
                                "repositories {\n${mavenRepositories.prependIndent(ind)}\n",
                            )
                        )
                    }
            }
        }

        /** file-based Maven repositories with Dokka dependencies */
        private val devMavenRepositories: List<Path> by org.jetbrains.dokka.it.systemProperty { repos ->
            repos.split(",").map { Paths.get(it) }
        }

        private val dokkaVersionOverride: String? by org.jetbrains.dokka.it.optionalSystemProperty()
        private val dokkaVersion: String by org.jetbrains.dokka.it.systemProperty { dokkaVersionOverride ?: it }

        private val mavenRepositories: String by lazy {
            val reposSpecs = if (dokkaVersionOverride != null) {
                println("Dokka version overridden with $dokkaVersionOverride")
                // if `DOKKA_VERSION_OVERRIDE` environment variable is provided,
                // we allow running tests on a custom Dokka version from specific repositories
                """
                maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test"),
                maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev"),
                mavenCentral(),
                mavenLocal()
                """.trimIndent()
            } else {
                // otherwise - use locally published versions via `devMavenPublish`
                devMavenRepositories.withIndex().joinToString(",\n") { (i, repoPath) ->
                    // Exclusive repository containing local Dokka artifacts.
                    // Must be compatible with both Groovy and Kotlin DSL.
                    """
                    |maven {
                    |    setUrl("${repoPath.invariantSeparatorsPathString}")
                    |    name = "DokkaDevMavenRepo${i}"
                    |}
                    """.trimMargin()
                }
            }

            """
            |exclusiveContent {
            |    forRepositories(
            |      $reposSpecs
            |    )
            |    filter {
            |        includeGroup("org.jetbrains.dokka")
            |    }
            |}
            |
            """.trimMargin()
        }
    }
}
