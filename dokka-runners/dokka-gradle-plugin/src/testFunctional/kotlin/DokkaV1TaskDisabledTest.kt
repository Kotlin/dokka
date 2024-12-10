/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
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

        test("v1 tasks should be skipped when v2 is enabled") {
            project.runner
                .addArguments("dokkaHtml")
                .build {
                    shouldHaveRunTask(":subproject-one:dokkaHtml", SKIPPED)
                    shouldHaveRunTask(":subproject-two:dokkaHtml", SKIPPED)
                }
        }
    }
})
