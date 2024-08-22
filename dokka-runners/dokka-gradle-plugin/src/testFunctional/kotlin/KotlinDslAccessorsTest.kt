/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.resource.resourceAsString
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class KotlinDslAccessorsTest : FunSpec({

    context("Kotlin DSL generated accessors") {
        val project = initMultiModuleProject("KotlinDslAccessorsTest")

        suspend fun testKotlinDslAccessors(
            taskPath: String,
            @Language("file-reference")
            expected: String,
        ) {
            test("generated from $taskPath should match $expected") {
                val (actualAccessors, actualAccessorsFile) = project.generateAccessors(taskPath)

                val expectedAccessors = resourceAsString(expected).trim()

                assertEquals(
                    expected = expectedAccessors,
                    actual = actualAccessors,
                    message = """
                        Task $taskPath generated unexpected accessors.
                           Actual:   ${actualAccessorsFile.toUri()}
                           Expected: ${Path("./src/testFunctional/resources/$expected").toUri()}
                        """.trimIndent()
                )
            }
        }

        testKotlinDslAccessors(
            taskPath = ":kotlinDslAccessorsReport",
            expected = "/KotlinDslAccessorsTest/root-project.txt",
        )
        testKotlinDslAccessors(
            taskPath = ":subproject-hello:kotlinDslAccessorsReport",
            expected = "/KotlinDslAccessorsTest/subproject-hello.txt"
        )
        testKotlinDslAccessors(
            taskPath = ":subproject-goodbye:kotlinDslAccessorsReport",
            expected = "/KotlinDslAccessorsTest/subproject-goodbye.txt"
        )
    }

    context("when project has Dokka buildSrc convention") {
        val project = initProjectWithBuildSrcConvention()

        test("DGP DSL accessors do not trigger compilation warnings") {
            project
                .runner
                .forwardOutput()
                .addArguments(
                    ":clean",
                    ":compileKotlin",
                    "--project-dir", "buildSrc",
                    "--rerun-tasks",
                    "--no-build-cache",
                    "--no-configuration-cache",
                )
                .build {
                    shouldHaveTaskWithOutcome(":compileKotlin", SUCCESS)
                }
        }
    }
})


private fun initProjectWithBuildSrcConvention(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    return gradleKtsProjectTest("kotlin-dsl-accessors-test") {

        buildGradleKts = """
            |plugins {
            |  base
            |  id("dokka-convention")
            |}
            |
            """.trimMargin()

        dir("buildSrc") {
            buildGradleKts = """
                |plugins {
                |  `kotlin-dsl`
                |}
                |
                |dependencies {
                |  implementation("org.jetbrains.dokka:dokka-gradle-plugin:$DOKKA_VERSION")
                |}
                |
                |kotlin {
                |  compilerOptions {
                |    allWarningsAsErrors.set(true)
                |  }
                |}
                |
                """.trimMargin()

            settingsGradleKts = """
                |rootProject.name = "buildSrc"
                |
                |${settingsRepositories()}
                |
                """.trimMargin()

            createKtsFile(
                "src/main/kotlin/dokka-convention.gradle.kts",
                """
                |plugins {
                |  id("org.jetbrains.dokka")
                |}
                |
                """.trimMargin()
            )
        }

        config()
    }
}

/**
 * Generate Kotlin DSL Accessors by running [taskPath].
 *
 * @returns Dokka-specific accessors, and a file of all accessors (to be used in assertion failure messages.)
 */
private fun GradleProjectTest.generateAccessors(taskPath: String): Pair<String, Path> {
    runner
        .addArguments(
            taskPath,
            "--quiet",
        )
        .build {
            val dokkaAccessors = splitDslAccessors(output)
                .filter { it.contains("dokka", ignoreCase = true) }
                .joinToString("\n\n")

            val dokkaAccessorsFile = projectDir.resolve("dokkaAccessors.txt")
                .apply { writeText(dokkaAccessors) }

            return dokkaAccessors to dokkaAccessorsFile
        }
}

/**
 * Gradle dumps the generated Kotlin DSL accessors to output.
 * This function splits them into individual sections, so they can be individually filtered.
 *
 * The accessors always have KDoc, and end with a blank line.
 */
private fun splitDslAccessors(
    output: String,
): List<String> {
    return output.lineSequence()
        .fold(ArrayDeque<StringBuilder>()) { acc, line ->
            if (acc.isEmpty() && line.isBlank()) {
                // no section started yet, ignore the preceding whitespace
            } else {
                if (line.trim() == "/**") {
                    // start a new section
                    acc.addLast(StringBuilder())
                } else if (acc.isEmpty()) {
                    // Somehow a section starts without KDoc...
                    // Unexpected. But let's be lenient.
                    acc.addLast(StringBuilder())
                }

                acc.last().appendLine(line)
            }

            acc
        }
        .map { it.toString().trimIndent().trim() }
        .sorted()
}
