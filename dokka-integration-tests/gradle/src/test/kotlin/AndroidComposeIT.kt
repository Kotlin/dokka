/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.file.shouldBeAFile
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
 * Integration test for the `it-android-compose` project.
 */
@TestsDGPv2
@TestsAndroidGradlePlugin
@TestsCompose
@WithGradleProperties(GradlePropertiesProvider.Android::class)
class AndroidComposeIT {

    @DokkaGradlePluginTest(sourceProjectName = "it-android-compose")
    fun `generate dokka HTML`(
        project: DokkaGradleProjectRunner,
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

            withClue(
                """
                expectedHtml: ${expectedHtml.toUri()}
                actualHtmlDir: ${actualHtmlDir.toUri()}
                """.trimIndent()
            ) {
                val expectedFileTree = expectedHtml.toTreeString()
                val actualFileTree = actualHtmlDir.toTreeString()
                withClue((actualFileTree to expectedFileTree).sideBySide()) {
                    expectedFileTree shouldBe actualFileTree

                    actualHtmlDir shouldBeDirectoryWithSameContentAs expectedHtml
                }
            }
        }
    }

    @DokkaGradlePluginTest(sourceProjectName = "it-android-compose")
    fun `Dokka tasks should be build cacheable`(
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
                withClue("expect dokkaGenerate tasks are loaded from cache") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE)
                }
            }
    }

    @DokkaGradlePluginTest(sourceProjectName = "it-android-compose")
    fun `expect Dokka is compatible with Gradle Configuration Cache`(
        project: DokkaGradleProjectRunner,
    ) {
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
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)

                output shouldContain "Configuration cache entry stored"

                loadConfigurationCacheReportData(projectDir = project.projectDir)
                    .asClue { ccReport ->
                        ccReport.totalProblemCount shouldBe 0
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
