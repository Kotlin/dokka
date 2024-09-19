/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.TaskOutcome.*
import org.jetbrains.dokka.gradle.internal.DokkaConstants
import org.jetbrains.dokka.gradle.utils.*

class BuildSrcKotlinDslAccessorsTest : FunSpec({

    val project = initProjectWithBuildSrcConvention()

    context("when DGPv2 is enabled") {
        project
            .runner
            .addArguments(
                ":compileKotlin",
                "--project-dir", "buildSrc",
            )
            .build {
                test("expect DGPv2 can be used in a convention plugin") {
                    shouldHaveTasksWithAnyOutcome(":compileKotlin" to listOf(SUCCESS, UP_TO_DATE, FROM_CACHE))
                }
            }
    }
})

private fun initProjectWithBuildSrcConvention(
    rootProjectName: String? = null,
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {

    return gradleKtsProjectTest(
        projectLocation = "BuildSrcKotlinDslAccessorsTest",
        rootProjectName = rootProjectName,
    ) {

        buildGradleKts = """
            |plugins {
            |  kotlin("jvm") version embeddedKotlinVersion
            |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
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
                |  implementation("org.jetbrains.dokka:dokka-gradle-plugin:${DokkaConstants.DOKKA_VERSION}")
                |}
                |
                """.trimMargin()


            settingsGradleKts = """
                |rootProject.name = "buildSrc"
                |
                |${settingsRepositories()}
                |
            """.trimMargin()

            createFile(
                "src/main/kotlin/dokka-convention.gradle.kts",
                /* language=TEXT */ """
                |plugins {
                |  id("org.jetbrains.dokka")
                |}
                |
                |dokka {
                |    moduleName.set("custom-module-name")
                |}
                |
                """.trimMargin()
            )
        }

        gradleProperties {
            dokka {
                v2Plugin = true
                v2MigrationHelpers = true
            }
        }

        config()
    }
}
