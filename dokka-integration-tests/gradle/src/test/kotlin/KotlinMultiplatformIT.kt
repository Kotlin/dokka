/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.matchers.sequences.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.TaskOutcome.*
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.it.gradle.junit.DokkaGradlePluginTest
import org.jetbrains.dokka.it.gradle.junit.DokkaGradleProjectRunner
import org.jetbrains.dokka.it.gradle.junit.TestsDGPv2
import org.jetbrains.dokka.it.gradle.junit.TestsKotlinMultiplatform
import org.junit.jupiter.api.Disabled
import kotlin.io.path.*

/**
 * Integration test for the `it-kotlin-multiplatform` project.
 */
@TestsKotlinMultiplatform
@TestsDGPv2
class KotlinMultiplatformIT {

    @Disabled("KMP: References to shared code is not linked when there is an intermediate level https://github.com/Kotlin/dokka/issues/3382")
    @DokkaGradlePluginTest(sourceProjectName = "it-kotlin-multiplatform")
    fun `generate dokka HTML`(project: DokkaGradleProjectRunner) {
        project.runner
            .addArguments(
                ":dokkaGenerate",
            )
            .build {
                withClue("expect project builds successfully") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(SUCCESS, UP_TO_DATE)
                }
            }

        withClue("expect actual generated HTML matches expected HTML") {
            val expectedHtml = project.projectDir.resolve("expectedData/html")
            val actualHtmlDir = project.projectDir.resolve("build/dokka/html")

            val dokkaConfigurationJsonFiles = project.findFiles { it.name == "dokka-configuration.json" }
            val dokkaConfigContent = dokkaConfigurationJsonFiles.joinToString("\n\n") { dcFile ->
                // re-encode the JSON to a compact format, to prevent the log output being completely spammed
                val compactJson = Json.parseToJsonElement(dcFile.readText())
                """
                - ${dcFile.invariantSeparatorsPathString}
                  $compactJson
                """.trimIndent()
            }

            withClue(
                """
                |expectedHtml: ${expectedHtml.toUri()}
                |actualHtmlDir: ${actualHtmlDir.toUri()}
                |dokkaConfigurationJsons [${dokkaConfigurationJsonFiles.count()}]:
                |$dokkaConfigContent
                """.trimMargin()
            ) {
                val expectedFileTree = expectedHtml.toTreeString()
                val actualFileTree = actualHtmlDir.toTreeString()
                withClue((actualFileTree to expectedFileTree).sideBySide()) {
                    actualHtmlDir.shouldBeADirectoryWithSameContentAs(expectedHtml)
                }
            }
        }
    }

    @Disabled("KMP: References to shared code is not linked when there is an intermediate level https://github.com/Kotlin/dokka/issues/3382")
    @DokkaGradlePluginTest(sourceProjectName = "it-kotlin-multiplatform")
    fun `verify generated HTML contains no class resolution errors`(project: DokkaGradleProjectRunner) {
        project.runner
            .addArguments(
                ":dokkaGenerate",
            )
            .build {
                withClue("expect project builds successfully") {
                    shouldHaveTask(":dokkaGenerate").shouldHaveOutcome(SUCCESS, UP_TO_DATE)
                }
            }

        withClue("expect actual generated HTML contains no class resolution errors") {
            val actualHtmlDir = project.projectDir.resolve("build/dokka/html")

            val filesWithErrors = actualHtmlDir.walk()
                .filter { it.isRegularFile() }
                .filter { file ->
                    file.useLines { lines ->
                        lines.any { line ->
                            "Error class: unknown class" in line || "Error type: Unresolved type" in line
                        }
                    }
                }

            withClue(
                "${filesWithErrors.count()} file(s) with errors:\n${filesWithErrors.joinToString("\n") { " - ${it.toUri()}" }}"
            ) {
                filesWithErrors.shouldBeEmpty()
            }
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
                ":dokkaGenerate",
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

        // Calculating task graph as configuration cache cannot be reused because directory '../../../../../../../home/.konan/kotlin-native-prebuilt-linux-x86_64-2.1.21/klib/platform/linux_x64' has changed
        withClue("second build - because sometimes KGP needs to finish installing kotlin-native-prebuilt") {
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
