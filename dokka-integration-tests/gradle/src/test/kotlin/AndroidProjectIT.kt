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
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.it.gradle.junit.*
import kotlin.io.path.deleteRecursively

/**
 * Integration test for the `it-android` project.
 */
@TestsAndroidGradlePlugin
@TestsDGPv2
@WithGradleProperties(GradlePropertiesProvider.Android::class)
class AndroidProjectIT {

    @DokkaGradlePluginTest(sourceProjectName = "it-android")
    fun `generate dokka HTML`(
        project: DokkaGradleProjectRunner
    ) {
        project.runner
            .addArguments(
                "clean",
                ":dokkaGenerate",
                "--stacktrace",
                "--rerun-tasks",
            )
            .build {
                withClue("expect project builds successfully") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(SUCCESS)
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
            val expectedHtml = project.projectDir.resolve("expectedData/html")

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


    @DokkaGradlePluginTest(sourceProjectName = "it-android-compose")
    fun `Dokka tasks should be cacheable`(
        project: DokkaGradleProjectRunner,
    ) {
        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--build-cache",
            )
            .build {
                withClue("expect dokkaGenerate runs successfully") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)
                }
            }

        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--build-cache",
            )
            .build {
                withClue("expect dokkaGenerate runs is loaded from cache") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE)
                }
            }
    }

    @DokkaGradlePluginTest(sourceProjectName = "it-android")
    fun `expect Dokka is compatible with Gradle Configuration Cache`(
        project: DokkaGradleProjectRunner
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
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)

                output shouldContain "Configuration cache entry stored"

                // this if is to workaround Gradle 7 having different logging - remove when minimum tested Gradle is 8+
                if ("0 problems were found storing the configuration cache." !in output) {
                    output shouldNotContain "problems were found storing the configuration cache"
                }
            }
        }

        withClue("second build should reuse the configuration cache") {
            configCacheRunner.build {
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)
                output shouldContain "Configuration cache entry reused"
            }
        }
    }
}
