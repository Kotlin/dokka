/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.gradle.utils.projects.initNoConfigMultiModuleProject

class PluginFeaturesServiceTest : FunSpec({
    context("given multi-module project") {
        val project = initNoConfigMultiModuleProject()

        context("when PluginMode has invalid value") {
            project.runner
                .addArguments(
                    ":help",
                    "--dry-run",
                    "--warn",
                    "-Porg.jetbrains.dokka.experimental.gradle.pluginMode=blah",
                )
                .addArguments()
                .build {
                    test("output should contain V1 warning") {
                        output shouldContain "Invalid value for org.jetbrains.dokka.experimental.gradle.pluginMode. Got 'blah' but expected one of: [V1Enabled, V2EnabledWithHelpers, V2Enabled]"
                    }
                }
        }
    }
})
