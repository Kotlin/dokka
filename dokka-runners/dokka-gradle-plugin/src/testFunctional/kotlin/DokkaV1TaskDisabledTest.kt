/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.buildAndFail
import org.jetbrains.dokka.gradle.utils.projects.initNoConfigMultiModuleProject
import org.jetbrains.dokka.gradle.utils.shouldHaveRunTask

class DokkaV1TaskDisabledTest : FunSpec({
    context("given multi-module project") {

        val project = initNoConfigMultiModuleProject {
            gradleProperties {
                dokka {
                    pluginMode = "V2EnabledWithHelpers"
                }
            }
        }

        context("v1 tasks should fail (because v2 is enabled by default)") {
            test("dokkaHtml") {
                project.runner
                    .addArguments("dokkaHtml")
                    .buildAndFail {
                        shouldHaveRunTask(":subproject-one:dokkaHtml", FAILED)
                        output shouldContain /* language=text */ """
                            |> Cannot run Dokka V1 tasks when V2 mode is enabled.
                            |  Dokka Gradle plugin V1 mode is deprecated, and scheduled to be removed in Dokka v2.2.0.
                            |  To finish migrating to V2 mode, please check the migration guide https://kotl.in/dokka-gradle-migration
                            |  Suggestion: Use `dokkaGenerate` or `dokkaGenerateHtml` tasks instead.
                            """.trimMargin()
                    }
            }
            test("dokkaJavadoc") {
                project.runner
                    .addArguments("dokkaJavadoc")
                    .buildAndFail {
                        shouldHaveRunTask(":subproject-one:dokkaJavadoc", FAILED)
                        output shouldContain /* language=text */ """
                            |> Cannot run Dokka V1 tasks when V2 mode is enabled.
                            |  Dokka Gradle plugin V1 mode is deprecated, and scheduled to be removed in Dokka v2.2.0.
                            |  To finish migrating to V2 mode, please check the migration guide https://kotl.in/dokka-gradle-migration
                            |  Suggestion: Use `dokkaGenerate` or `dokkaGenerateJavadoc` tasks instead.
                            """.trimMargin()
                    }
            }
            test("dokkaGfm") {
                project.runner
                    .addArguments("dokkaGfm")
                    .buildAndFail {
                        shouldHaveRunTask(":subproject-one:dokkaGfm", FAILED)
                        output shouldContain /* language=text */ """
                            |> Cannot run Dokka V1 tasks when V2 mode is enabled.
                            |  Dokka Gradle plugin V1 mode is deprecated, and scheduled to be removed in Dokka v2.2.0.
                            |  To finish migrating to V2 mode, please check the migration guide https://kotl.in/dokka-gradle-migration
                            """.trimMargin()
                    }
            }
            test("dokkaJekyll") {
                project.runner
                    .addArguments("dokkaJekyll")
                    .buildAndFail {
                        shouldHaveRunTask(":subproject-one:dokkaJekyll", FAILED)
                        output shouldContain /* language=text */ """
                            |> Cannot run Dokka V1 tasks when V2 mode is enabled.
                            |  Dokka Gradle plugin V1 mode is deprecated, and scheduled to be removed in Dokka v2.2.0.
                            |  To finish migrating to V2 mode, please check the migration guide https://kotl.in/dokka-gradle-migration
                            """.trimMargin()
                    }
            }
        }
    }
})
