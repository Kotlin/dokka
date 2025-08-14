/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject

class MultiModuleAggregationTest : FunSpec({

    context("when aggregating subprojects and root project in root project") {

        context("and default modulePath") {
            val project = project()
            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--stacktrace",
                    "--rerun",
                )
                .forwardOutput()
                .build {
                    test("expect validation success") {
                        shouldHaveRunTask(":dokkaGeneratePublicationHtml", TaskOutcome.SUCCESS)
                    }
                }
        }

        context("and empty modulePath") {
            val project = project {
                dir("subproject-goodbye") {
                    buildGradleKts += """
                        |dokka {
                        |    modulePath.set("")
                        |}
                        |""".trimMargin()
                }
                dir("subproject-hello") {
                    buildGradleKts += """
                        |dokka {
                        |    modulePath.set(".")
                        |}
                        |""".trimMargin()
                }
            }
            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--stacktrace",
                    "--rerun",
                )
                .forwardOutput()
                .buildAndFail {
                    test("expect validation failure") {
                        shouldHaveRunTask(":dokkaGeneratePublicationHtml", TaskOutcome.FAILED)
                        output shouldContain """
                            ┆> [:dokkaGeneratePublicationHtml] Found 2 modules with output directories that resolve to the same directory as the Dokka output directory.
                            ┆    All module output directories must be a subdirectory inside the Dokka output directory.
                            ┆    Specify `modulePath` in these modules:
                            ┆    - subproject-goodbye (modulePath: '')
                            ┆    - subproject-hello (modulePath: '.')
                            ┆""".trimMargin("┆")
                    }
                }
        }

        context("and custom valid modulePath") {
            val project = project {
                buildGradleKts += """
                    |dokka {
                    |    modulePath.set("root")
                    |}
                    |""".trimMargin()
            }
            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--stacktrace",
                    "--rerun",
                )
                .forwardOutput()
                .build {
                    test("expect validation success") {
                        shouldHaveRunTask(":dokkaGeneratePublicationHtml", TaskOutcome.SUCCESS)
                    }
                }
        }

        context("and modulePath escapes the output dir") {
            val project = project {
                buildGradleKts += """
                dokka {
                    modulePath.set("../../root")
                }
             """.trimIndent()
            }
            project.runner
                .addArguments(
                    ":dokkaGeneratePublicationHtml",
                    "--stacktrace",
                    "--rerun",
                )
                .forwardOutput()
                .buildAndFail {
                    test("expect validation failure") {
                        shouldHaveRunTask(":dokkaGeneratePublicationHtml", TaskOutcome.FAILED)
                        output shouldContain """
                            ┆> [:dokkaGeneratePublicationHtml] Found 1 modules with directories that are outside the Dokka output directory.
                            ┆    All module output directories must be a subdirectory inside the Dokka output directory.
                            ┆    Update the `modulePath` in these modules:
                            ┆    - root-aggregate (modulePath: '../../root')
                            ┆""".trimMargin("┆")
                    }
                }
        }
    }
})


private fun TestScope.project(
    configure: GradleProjectTest.() -> Unit = {},
): GradleProjectTest =
    initMultiModuleProject(
        testName = "root-aggregate-${testCase.name.testName.replaceNonAlphaNumeric()}",
        rootProjectName = "root-aggregate",
    ) {
        buildGradleKts = buildGradleKts
            .replace(
                """kotlin("jvm") version embeddedKotlinVersion apply false""",
                """kotlin("jvm") version embeddedKotlinVersion""",
            )

        buildGradleKts += """
                |dependencies {
                |  dokka(project)
                |}
                |""".trimMargin()

        createKotlinFile(
            "src/main/kotlin/RootProjectClass.kt",
            """
            |class RootProjectClass {
            |  fun thisClassIsInTheRootProject() {}
            |}
            |""".trimMargin()
        )

        configure()
    }
