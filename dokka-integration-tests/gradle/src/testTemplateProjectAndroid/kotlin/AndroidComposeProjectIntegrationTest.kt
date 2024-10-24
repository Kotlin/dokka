/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldHaveSameStructureAndContentAs
import io.kotest.matchers.file.shouldHaveSameStructureAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.it.systemProperty
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

/**
 * Integration test for the `it-android-compose` project.
 */
class AndroidComposeProjectIntegrationTest {

    @DokkaGradlePluginTest(sourceProjectDir = sourceProjectDir)
    @TestAndroidGradlePlugin
    fun `generate dokka HTML`(
        project: GradleProject
    ) {
        assumeTrue(project.versions.kgp == SemVer("1.9.24")) {
            "Compose Multiplatform 1.5.14 must use Kotlin 1.9.24"
        }
        assumeTrue(project.versions.agp!!.major >= 8) {
            "TODO make project versions paramterised"
        }

        println("Testing project ${project.projectDir.toUri()}")

        project.runner
            .addArguments(
                "clean",
                ":dokkaGenerate",
                "--stacktrace",
                "--rerun-tasks",
            )
            .build {
                withClue("expect project builds successfully") {
                    output shouldContain "BUILD SUCCESSFUL"
                }

                withClue("expect all dokka workers are successful") {
                    project
                        .findFiles { it.name == "dokka-worker.log" }
                        .shouldForAll { dokkaWorkerLog ->
                            dokkaWorkerLog.shouldBeAFile()
                            dokkaWorkerLog.readText().shouldNotContainAnyOf(
                                "[ERROR]",
                                "[WARN]",
                            )
                        }
                }

                withClue("expect configurations are not resolved during configuration time") {
                    output shouldNotContain Regex("""Configuration '.*' was resolved during configuration time""")
                    output shouldNotContain "https://github.com/gradle/gradle/issues/2298"
                }
            }


        withClue("expect the same HTML is generated") {

            // TODO expected HTML contains error:
            //      fun TopAppBarAction(menuItem: <Error class: unknown class>, modifier: Modifier = Modifier)
            val expectedHtml = expectedDataDir()

            val actualHtmlDir = project.projectDir.resolve("build/dokka/html")

            val expectedFileTree = expectedHtml.toTreeString()
            val actualFileTree = actualHtmlDir.toTreeString()
            withClue((actualFileTree to expectedFileTree).sideBySide()) {
                expectedFileTree shouldBe actualFileTree

                actualHtmlDir.toFile().shouldHaveSameStructureAs(expectedHtml.toFile())
                actualHtmlDir.toFile().shouldHaveSameStructureAndContentAs(expectedHtml.toFile())
            }
        }
    }

    @DokkaGradlePluginTest(sourceProjectDir = sourceProjectDir)
    @TestAndroidGradlePlugin
    fun `Dokka tasks should be cacheable`(
        project: GradleProject
    ) {
        assumeTrue(project.versions.kgp == SemVer("1.9.24")) {
            "Compose Multiplatform 1.5.14 must use Kotlin 1.9.24"
        }
        assumeTrue(project.versions.agp!!.major >= 8) {
            "TODO make project versions paramterised"
        }

        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--build-cache",
            )
            .build {
                withClue("expect project builds successfully") {
                    // TODO use proper test assertions
                    output shouldContain "BUILD SUCCESSFUL"
                }
            }

        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--build-cache",
            )
            .build {
                output shouldContainAll listOf(
                    // TODO use proper test assertions
                    "Task :dokkaGenerate UP-TO-DATE",
                )
            }
    }

    @DokkaGradlePluginTest(sourceProjectDir = sourceProjectDir)
    @TestAndroidGradlePlugin
    fun `expect Dokka is compatible with Gradle Configuration Cache`(
        project: GradleProject
    ) {
        assumeTrue(project.versions.kgp == SemVer("1.9.24")) {
            "Compose Multiplatform 1.5.14 must use Kotlin 1.9.24"
        }
        assumeTrue(project.versions.agp!!.major >= 8) {
            "TODO make project versions paramterised"
        }

        project.file(".gradle/configuration-cache").deleteRecursively()
        project.file("build/reports/configuration-cache").deleteRecursively()

        val configCacheRunner =
            project.runner.addArguments(
                "clean",
                ":dokkaGenerate",
                "--no-build-cache",
                "--configuration-cache",
            )

        withClue("first build should store the configuration cache") {
            configCacheRunner.build {
                // TODO use proper test assertions
                output shouldContain "BUILD SUCCESSFUL"
                output shouldContain "Configuration cache entry stored"

                // this if is to workaround Gradle 7 having different logging - remove when minimum tested Gradle is 8+
                if ("0 problems were found storing the configuration cache." !in output) {
                    output shouldNotContain "problems were found storing the configuration cache"
                }
            }
        }

        withClue("second build should reuse the configuration cache") {
            configCacheRunner.build {
                // TODO use proper test assertions
                output shouldContain "BUILD SUCCESSFUL"
                output shouldContain "Configuration cache entry reused"
            }
        }
    }

    companion object {
        // TODO remove hardcoded dir
        private const val sourceProjectDir =
            "/Users/dev/projects/jetbrains/dokka/dokka-integration-tests/gradle/projects/it-android-compose"

        private val baseExpectedDataDir: Path by systemProperty(::Path)

        // TODO 'format' parameter doesn't match, move AndroidComposeProjectIntegrationTest to different source set?
        private fun expectedDataDir(format: String = "it-android-compose/html"): Path =
            baseExpectedDataDir.resolve(format)
    }
}
