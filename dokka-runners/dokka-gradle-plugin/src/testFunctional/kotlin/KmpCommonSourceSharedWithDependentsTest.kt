/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.sequences.shouldBeEmpty
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.walk

class KmpCommonSourceSharedWithDependentsTest : FunSpec({
    test("common source set is propagated to dependents") {
        val project = initProject()

        project
            .runner
            .addArguments(":dokkaGenerate")
            .build {
                shouldHaveTasksWithAnyOutcome(
                    ":dokkaGenerate" to listOf(UP_TO_DATE, SUCCESS),
                )

                val htmlOutputDir = project.projectDir.resolve("build/dokka/html")

                val filesWithErrors = htmlOutputDir.walk()
                    .filter { it.isRegularFile() }
                    .filter {
                        it.readText().lineSequence().any { line ->
                            "Error class: unknown class" in line || "Error type: Unresolved type" in line
                        }
                    }

                withClue(
                    "files with errors: ${filesWithErrors.joinToString("\n") { " - ${it.toUri()}" }}"
                ) {
                    filesWithErrors.shouldBeEmpty()
                }
            }
    }
})


private fun initProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    return gradleKtsProjectTest("KmpCommonSourceSharedWithDependentsTest") {

        buildGradleKts = """
            |plugins {
            |  kotlin("multiplatform") version embeddedKotlinVersion
            |  id("org.jetbrains.dokka") version "$DOKKA_VERSION"
            |}
            |
            |kotlin {
            |    jvm()
            |    iosX64()
            |    iosArm64()
            |}
            |
            """.trimMargin()

        createKotlinFile(
            "src/commonMain/kotlin/CommonMainCls.kt", """
            package a.b.c
            
            /** commonMain class */
            class CommonMainCls
        """.trimIndent()
        )

        createKotlinFile(
            "src/iosMain/kotlin/IosMainCls.kt", """
            package a.b.c
            
            /** iosMain class */
            class IosMainCls
        """.trimIndent()
        )

        createKotlinFile(
            "src/iosX64Main/kotlin/iosX64Fn.kt", """
            package a.b.c
            
            /** iosX64Main function */
            fun iosX64Fn(a: CommonMainCls, b: IosMainCls) {
              println(a)
              println(b)
            }
        """.trimIndent()
        )

        config()
    }
}
