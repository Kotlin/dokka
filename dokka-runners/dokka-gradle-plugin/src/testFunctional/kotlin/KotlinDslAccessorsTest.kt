/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.resource.resourceAsString
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.GradleProjectTest.Companion.settingsRepositories
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals

/**
 * Test the Kotlin DSL accessors that Gradle generates.
 *
 * These accessors are part of Dokka's public ABI, and any modifications could break build scripts.
 * These tests are similar to the BCV API Dump checks.
 */
class KotlinDslAccessorsTest : FunSpec({

    context("Kotlin DSL generated accessors") {
        val project = initMultiModuleProject("KotlinDslAccessorsTest") {
            gradleProperties {
                dokka {
                    v2Plugin = false
                    v2MigrationHelpers = false
                }
            }
        }

        /**
         * Verify the current accessors match the dumped accessors.
         *
         * If this test is failing after intentional changes then update the dump file.
         * (The test assertion messages contains links to files containing the actual and expected accessors,
         * so copy the actual accessors to the expected.)
         */
        suspend fun FunSpecContainerScope.testKotlinDslAccessors(
            actualAccessors: GeneratedDokkaAccessors,
            @Language("file-reference")
            expected: String,
        ) {
            test("generated from project ${actualAccessors.projectPath} should match $expected") {

                val expectedAccessors = resourceAsString(expected)

                assertEquals(
                    expected = expectedAccessors.trim(),
                    actual = actualAccessors.joined.trim(),
                    message = """
                        Task ${actualAccessors.projectPath} generated unexpected accessors.
                           Actual:   ${actualAccessors.file.toUri()}
                           Expected: ${Path("./src/testFunctional/resources/$expected").toUri()}
                        """.trimIndent()
                )
            }
        }

        context("default accessors") {
            testKotlinDslAccessors(
                actualAccessors = project.generateDokkaAccessors(projectPath = ":"),
                expected = "/KotlinDslAccessorsTest/root-project.txt",
            )
            testKotlinDslAccessors(
                actualAccessors = project.generateDokkaAccessors(projectPath = ":subproject-hello"),
                expected = "/KotlinDslAccessorsTest/subproject-hello.txt",
            )
            testKotlinDslAccessors(
                actualAccessors = project.generateDokkaAccessors(projectPath = ":subproject-goodbye"),
                expected = "/KotlinDslAccessorsTest/subproject-goodbye.txt",
            )
        }

        context("v2 migration helpers") {
            testKotlinDslAccessors(
                actualAccessors = project.generateDokkaV2MigrationHelpersAccessors(":"),
                expected = "/KotlinDslAccessorsTest/root-project-v2-migration-helpers.txt",
            )
            testKotlinDslAccessors(
                actualAccessors = project.generateDokkaV2MigrationHelpersAccessors(":subproject-hello"),
                expected = "/KotlinDslAccessorsTest/subproject-hello-v2-migration-helpers.txt",
            )
            testKotlinDslAccessors(
                actualAccessors = project.generateDokkaV2MigrationHelpersAccessors(":subproject-goodbye"),
                expected = "/KotlinDslAccessorsTest/subproject-goodbye-v2-migration-helpers.txt",
            )
        }
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


private class GeneratedDokkaAccessors(
    val projectPath: String,
    val list: List<String>,
    projectDir: Path,
) {
    val joined: String = list.joinToString(separator = "\n\n", postfix = "\n")

    val file: Path = projectDir.resolve("dokka-accessors-${System.currentTimeMillis()}.txt").apply {
        writeText(joined)
    }
}

/**
 * Specifically extract the V2 migration helpers.
 */
private fun GradleProjectTest.generateDokkaV2MigrationHelpersAccessors(
    projectPath: String,
): GeneratedDokkaAccessors {
    val accessorsWithoutV2Helpers = generateDokkaAccessors(
        projectPath = projectPath,
        enableV2MigrationHelpers = false,
    ).list

    val accessorsWithV2Helpers = generateDokkaAccessors(
        projectPath = projectPath,
        enableV2MigrationHelpers = true,
    ).list

    val v2Helpers = accessorsWithV2Helpers - accessorsWithoutV2Helpers.toSet()

    return GeneratedDokkaAccessors(
        projectPath = projectPath,
        list = v2Helpers,
        projectDir = projectDir
    )
}

/**
 * Generate Kotlin DSL Accessors by running `:` in [projectPath].
 *
 * @returns Dokka-specific accessors, and a file of all accessors (to be used in assertion failure messages.)
 */
private fun GradleProjectTest.generateDokkaAccessors(
    projectPath: String,
    enableV2MigrationHelpers: Boolean? = null,
): GeneratedDokkaAccessors {
    runner
        .addArguments(
            buildList {
                add(org.gradle.util.Path.path(projectPath).relativePath("kotlinDslAccessorsReport").toString())
                add("--quiet")
                add("-Porg.jetbrains.dokka.experimental.gradlePlugin.enableV2=true")
                enableV2MigrationHelpers?.let {
                    add("-Porg.jetbrains.dokka.experimental.gradlePlugin.enableV2MigrationHelpers=$it")
                }
            }
        )
        .build {
            val dokkaAccessors = splitDslAccessors(output)
                .filter { it.contains("dokka", ignoreCase = true) }

            return GeneratedDokkaAccessors(
                projectPath = projectPath,
                list = dokkaAccessors,
                projectDir = projectDir,
            )
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
        .map {
            it.toString()
                .trimIndent()
                .substringAfter("*/") // remove KDoc, it's not relevant.
                .trim()
        }
        .distinct()
        .sorted()
}
