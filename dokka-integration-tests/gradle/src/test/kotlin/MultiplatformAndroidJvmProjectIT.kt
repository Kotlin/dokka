/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.gradle.utils.findFiles
import org.jetbrains.dokka.gradle.utils.shouldBeADirectoryWithSameContentAs
import org.jetbrains.dokka.gradle.utils.shouldNotContainAnyOf
import org.jetbrains.dokka.gradle.utils.sideBySide
import org.jetbrains.dokka.gradle.utils.toTreeString
import org.jetbrains.dokka.it.gradle.junit.*
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Integration test for the `it-multiplatform-android-jvm` project.
 */
@TestsAndroid
@TestsDGPv2
@WithGradleProperties(GradlePropertiesProvider.Android::class)
class MultiplatformAndroidJvmProjectIT {

    @DokkaGradlePluginTest(sourceProjectName = "it-multiplatform-android-jvm")
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
}
