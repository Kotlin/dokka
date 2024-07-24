/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*


class KotlinDslAccessorsTest : FunSpec({

    val project = initProject()

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
})


private fun initProject(
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
