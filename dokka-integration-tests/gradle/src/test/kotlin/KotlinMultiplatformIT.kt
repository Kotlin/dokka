/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome.*
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.it.gradle.junit.DokkaGradlePluginTest
import org.jetbrains.dokka.it.gradle.junit.DokkaGradleProjectRunner
import org.jetbrains.dokka.it.gradle.junit.TestsDGPv2
import org.jetbrains.dokka.it.gradle.junit.TestsKotlinMultiplatform
import kotlin.io.path.deleteRecursively
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Integration test for the `it-kotlin-multiplatform` project.
 */
@TestsKotlinMultiplatform
@TestsDGPv2
class KotlinMultiplatformIT {

    @DokkaGradlePluginTest(sourceProjectName = "it-kotlin-multiplatform")
    fun `generate dokka HTML`(project: DokkaGradleProjectRunner) {
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
                    actualFileTree shouldBe expectedFileTree

                    actualHtmlDir.shouldBeADirectoryWithSameContentAs(expectedHtml, TestConstants.DokkaHtmlAssetsFiles)
                }
            }

            assertNoUnknownClassErrorsInHtml(actualHtmlDir)
        }
    }

    @DokkaGradlePluginTest(sourceProjectName = "it-kotlin-multiplatform")
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
                    shouldHaveTask(":dokkaGeneratePublicationHtml").shouldHaveOutcome(FROM_CACHE, SUCCESS)
                }
                withClue("only one project is documented, so expect no Dokka module generation") {
                    shouldNotHaveRunTask(":dokkaGenerateModuleHtml")
                }
            }

        project.runner
            .addArguments(
                ":clean",
                "--build-cache",
            ).build {
                withClue("expect clean runs successfully") {
                    shouldHaveTask(":clean").shouldHaveOutcome(SUCCESS)
                }
            }

        project.runner
            .addArguments(
                ":dokkaGenerate",
                "--build-cache",
            )
            .build {
                withClue("expect dokkaGenerate lifecycle task is up-to-date") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE)
                }
                withClue("expect dokkaGenerate* work tasks are loaded from cache") {
                    shouldHaveTask(":dokkaGeneratePublicationHtml").shouldHaveOutcome(FROM_CACHE)
                }
                withClue("only one project is documented, so expect no Dokka module generation") {
                    shouldNotHaveRunTask(":dokkaGenerateModuleHtml")
                }
            }
    }

    @DokkaGradlePluginTest(sourceProjectName = "it-kotlin-multiplatform")
    fun `expect Dokka is compatible with Gradle Configuration Cache`(
        project: DokkaGradleProjectRunner,
    ) {
        fun clearCcReports() {
            project.file(".gradle/configuration-cache").deleteRecursively()
            project.file("build/reports/configuration-cache").deleteRecursively()
        }

        val configCacheRunner =
            project.runner.addArguments(
                "clean",
                ":dokkaGenerate",
                "--no-build-cache",
                "--configuration-cache",
            )

        withClue("first build should store the configuration cache") {
            clearCcReports()
            configCacheRunner.build {
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)

                output shouldContain "Configuration cache entry stored"

                loadConfigurationCacheReportData(projectDir = project.projectDir)
                    .asClue { ccReport ->
                        ccReport.totalProblemCount shouldBe 0
                    }
            }
        }

        withClue("second build - because sometimes KGP needs to finish installing kotlin-native-prebuilt") {
            // without this second build the test fails on TeamCity, because the CC entry isn't reused because of:
            // Calculating task graph as configuration cache cannot be reused because directory '../../../../../../../home/.konan/kotlin-native-prebuilt-linux-x86_64-2.1.21/klib/platform/linux_x64' has changed
            // Probably is related to KT-77218
            clearCcReports()
            configCacheRunner.build {
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)

                output shouldContain "Configuration cache entry stored"

                loadConfigurationCacheReportData(projectDir = project.projectDir)
                    .asClue { ccReport ->
                        ccReport.totalProblemCount shouldBe 0
                    }
            }
        }

        withClue("third build should reuse the configuration cache") {
            configCacheRunner.build {
                shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(UP_TO_DATE, SUCCESS)
                output shouldContain "Configuration cache entry reused"
            }
        }
    }
}
