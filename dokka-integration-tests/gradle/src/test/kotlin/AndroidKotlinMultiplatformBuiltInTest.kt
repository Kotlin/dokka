/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import io.kotest.assertions.withClue
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.it.gradle.junit.*
import org.jetbrains.dokka.it.gradle.junit.TestedVersions
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Integration test for the `it-android-kotlin-mp-builtin` project.
 */
@TestsDGPv2
@TestsAndroid
@TestsKotlinMultiplatform
class AndroidKotlinMultiplatformBuiltInTest {

    @DokkaGradlePluginTest(sourceProjectName = "it-android-kotlin-mp-builtin")
    fun `generate dokka HTML`(
        project: DokkaGradleProjectRunner,
        testedVersions: TestedVersions.Android,
    ) {
        assumeTrue(
            testedVersions.agp.major >= 8,
            "The com.android.kotlin.multiplatform.library plugin is only available in AGP 8.0.0+ " +
                    "(current AGP ${testedVersions.agp}). " +
                    "See https://developer.android.com/kotlin/multiplatform/plugin#prerequisites",
        )
        assumeTrue(
            testedVersions.kgp.major >= 2,
            "The com.android.kotlin.multiplatform.library requires kotlin-multiplatform 2.0.0+ (current KGP ${testedVersions.kgp})" +
                    "(current KGP ${testedVersions.kgp}). " +
                    "See https://developer.android.com/kotlin/multiplatform/plugin#prerequisites",
        )

        if (testedVersions.agp.major < 9) {
            // AGP 8 uses KotlinTarget JVM instead of AndroidJVM.
            // This is an AGP 8 bug - it's fixed in AGP 9.
            // For AGP <9, workaround the bug by manually enabling the Android documentation link.
            project.buildGradleKts += """
            |dokka.dokkaSourceSets.configureEach {
            |    enableAndroidDocumentationLink.set(true)
            |}
            |""".trimMargin()
        }

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
        }
    }
}
