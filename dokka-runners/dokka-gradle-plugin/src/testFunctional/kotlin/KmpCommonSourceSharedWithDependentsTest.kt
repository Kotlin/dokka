/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import kotlin.io.path.readText

class KmpCommonSourceSharedWithDependentsTest : FunSpec({
    test("common source set is propagated to dependents") {
        val project = initProject()

        project
            .runner
            .addArguments(":dokkaGenerate")
            .build {
                shouldHaveRunTask(":dokkaGenerate").shouldHaveOutcome(SUCCESS)

                val htmlOutputDir = project.projectDir.resolve("build/dokka/html")

                withClue("htmlOutputDir: ${htmlOutputDir.toUri()}") {
                    val iosX64FnFile =
                        htmlOutputDir.resolve("-kmp-common-source-shared-with-dependents-test/[root]/ios-x64-fn.html")

                    iosX64FnFile.shouldBeAFile()

                    iosX64FnFile.readText().let { text ->
                        text shouldNotContain "Error class: unknown class"
                        text shouldContain "Error type: Unresolved type"
                    }
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
            /** commonMain class */
            class CommonMainCls
        """.trimIndent()
        )

        createKotlinFile(
            "src/iosMain/kotlin/IosMainCls.kt", """
            /** iosMain class */
            class IosMainCls
        """.trimIndent()
        )

        createKotlinFile(
            "src/iosX64Main/kotlin/iosX64Fn.kt", """
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
