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

        test("v1 tasks should fail (because v2 is enabled by default)") {
            project.runner
                .addArguments("dokkaHtml")
                .buildAndFail {
                    shouldHaveRunTask(":subproject-one:dokkaHtml", FAILED)
                    output shouldContain """
                        Cannot run Dokka V1 task when V2 mode is enabled. Dokka Gradle plugin V1 mode is deprecated, and scheduled to be removed in Dokka v2.2.0. Migrate to V2 mode https://kotl.in/dokka-gradle-migration
                    """.trimIndent()
                }
        }
    }
})
