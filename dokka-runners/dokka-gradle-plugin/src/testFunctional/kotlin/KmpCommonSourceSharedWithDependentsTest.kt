/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.sequences.shouldBeEmpty
import org.gradle.testkit.runner.TaskOutcome.*
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import kotlin.io.path.isRegularFile
import kotlin.io.path.useLines
import kotlin.io.path.walk

@Ignored("KMP: References is not linked if they are in shared code and there is an intermediate level between them https://github.com/Kotlin/dokka/issues/3382")
class KmpCommonSourceSharedWithDependentsTest : FunSpec({
    context("common source set is propagated to dependents") {
        val project = initProject()

        test("expect project can be built") {
            project
                .runner
                .addArguments(":build")
                .build {
                    shouldHaveTasksWithAnyOutcome(
                        ":build" to listOf(SUCCESS, UP_TO_DATE, FROM_CACHE)
                    )
                }
        }

        test("expect dokkaGenerate runs successfully") {
            project
                .runner
                .addArguments(":dokkaGenerate")
                .build {
                    shouldHaveTasksWithAnyOutcome(
                        ":dokkaGenerate" to listOf(UP_TO_DATE, SUCCESS, FROM_CACHE),
                    )

                    val htmlOutputDir = project.projectDir.resolve("build/dokka/html")

                    val filesWithErrors = htmlOutputDir.walk()
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
    }
})


private fun initProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    @Suppress("KDocUnresolvedReference") // IJ gets confused and tries to resolve the KDoc from createKotlinFile(...)
    return gradleKtsProjectTest("KmpCommonSourceSharedWithDependentsTest") {

        buildGradleKts = """
            |plugins {
            |  kotlin("multiplatform") version embeddedKotlinVersion
            |  id("org.jetbrains.dokka") version "$DOKKA_VERSION"
            |}
            |
            |kotlin {
            |    jvm()
            |    linuxX64()
            |    iosX64()
            |    iosArm64()
            |}
            |
            """.trimMargin()

        createKotlinFile(
            "src/commonMain/kotlin/CommonMainCls.kt",
            """
            |package a.b.c
            |
            |/** A class defined in `commonMain`. */
            |class CommonMainCls
            |
            """.trimMargin()
        )

        createKotlinFile(
            "src/iosMain/kotlin/IosMainCls.kt",
            """
            |package a.b.c
            |
            |/** A class defined in `iosMain`. */
            |class IosMainCls
            |
            """.trimMargin()
        )

        createKotlinFile(
            "src/linuxMain/kotlin/LinuxMainCls.kt",
            """
            |package a.b.c
            |
            |/** A class defined in `linuxMain`. */
            |class LinuxMainCls
            |
            """.trimMargin()
        )

        createKotlinFile(
            "src/iosX64Main/kotlin/iosX64Fn.kt",
            """
            |package a.b.c
            |
            |/** A `iosX64Main` function that uses [CommonMainCls] from `commonMain` and [IosMainCls] from `iosMain` */
            |fun iosX64MainFn(a: CommonMainCls, b: IosMainCls) {
            |  println(a)
            |  println(b)
            |}
            |
            """.trimMargin()
        )

        createKotlinFile(
            "src/linuxX64Main/kotlin/linuxX64Fn.kt",
            """
            |package a.b.c
            |
            |/** A `linuxX64Main` function that uses [CommonMainCls] from `commonMain` and [LinuxMainCls] from `linuxMain` */
            |fun linuxX64Fn(a: CommonMainCls, b: LinuxMainCls) {
            |  println(a)
            |  println(b)
            |}
            |
            """.trimMargin()
        )

        config()
    }
}
