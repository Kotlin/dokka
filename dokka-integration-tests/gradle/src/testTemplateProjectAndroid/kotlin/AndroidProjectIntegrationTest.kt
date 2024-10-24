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
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

/**
 * Integration test for the `it-android-0` project.
 */
class AndroidProjectIntegrationTest {

    @DokkaGradlePluginTest(
        sourceProjectDir = "/Users/dev/projects/jetbrains/dokka/dokka-integration-tests/gradle/projects/it-android-0-v2",
    )
    @TestAndroidGradlePlugin
    fun `generate dokka HTML`(
        project: GradleProject
    ) {
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
            val expectedHtml = expectedDataDir()

            val dokkatooHtmlDir = project.projectDir.resolve("build/dokka/html")

            val expectedFileTree = expectedHtml.toTreeString()
            val actualFileTree = dokkatooHtmlDir.toTreeString()
            withClue((actualFileTree to expectedFileTree).sideBySide()) {
                expectedFileTree shouldBe actualFileTree

                dokkatooHtmlDir.toFile().shouldHaveSameStructureAs(expectedHtml.toFile())
                dokkatooHtmlDir.toFile().shouldHaveSameStructureAndContentAs(expectedHtml.toFile())
            }
        }
    }

    @DokkaGradlePluginTest(
        sourceProjectDir = "/Users/dev/projects/jetbrains/dokka/dokka-integration-tests/gradle/projects/it-android-0-v2",
    )
    @TestAndroidGradlePlugin
    fun `Dokka tasks should be cacheable`(
        project: GradleProject
    ) {
        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--stacktrace",
                "--build-cache",
            )
            .build {
                output shouldContainAll listOf(
                    "Task :dokkaGenerate UP-TO-DATE",
                )
            }
    }

    @DokkaGradlePluginTest(
        sourceProjectDir = "/Users/dev/projects/jetbrains/dokka/dokka-integration-tests/gradle/projects/it-android-0-v2",
    )
    @TestAndroidGradlePlugin
    fun `expect Dokkatoo is compatible with Gradle Configuration Cache`(
        project: GradleProject
    ) {
        project.file(".gradle/configuration-cache").deleteRecursively()
        project.file("build/reports/configuration-cache").deleteRecursively()

        val configCacheRunner =
            project.runner.addArguments(
                "clean",
                ":dokkaGenerate",
                "--stacktrace",
                "--no-build-cache",
                "--configuration-cache",
            )

        withClue("first build should store the configuration cache") {
            configCacheRunner.build {
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
                output shouldContain "BUILD SUCCESSFUL"
                output shouldContain "Configuration cache entry reused"
            }
        }
    }

    companion object {
        private val baseExpectedDataDir: Path by systemProperty(::Path)
        private fun expectedDataDir(format: String = "html"): Path =
            baseExpectedDataDir.resolve(format)
    }
}
