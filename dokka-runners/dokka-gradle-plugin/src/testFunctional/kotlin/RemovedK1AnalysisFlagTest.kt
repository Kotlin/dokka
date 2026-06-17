/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.gradle.utils.projects.initNoConfigMultiModuleProject

/**
 * K1 (descriptors) analysis has been removed. The `org.jetbrains.dokka.experimental.tryK2` flag
 * (and its `.noWarn` companion) no longer have any effect, so DGP warns if they are still set.
 */
class RemovedK1AnalysisFlagTest : FunSpec({
    val removedK1AnalysisWarning = "warning: Dokka K1 analysis has been removed"

    context("given multi-module project") {
        val project = initNoConfigMultiModuleProject {
            gradleProperties {
                // disable project iso, because all projects must be configured to trigger the logged warnings
                gradle.isolatedProjects = false
            }
        }
        for(flag in listOf("true", "false")) {
            context("when the removed tryK2 flag is set to $flag") {
                project.runner
                    .addArguments(
                        ":help",
                        "--dry-run",
                        "-Porg.jetbrains.dokka.experimental.tryK2=$flag",
                    )
                    .build {
                        test("output should contain the removed-K1-analysis warning") {
                            output shouldContain removedK1AnalysisWarning
                            output shouldContain "org.jetbrains.dokka.experimental.tryK2"
                        }
                    }
            }

            context("when the deprecated tryK2.noWarn flag is set to $flag") {
                project.runner
                    .addArguments(
                        ":help",
                        "--dry-run",
                        "-Porg.jetbrains.dokka.experimental.tryK2.noWarn=$flag",
                    )
                    .build {
                        test("output should contain the removed-K1-analysis warning") {
                            output shouldContain removedK1AnalysisWarning
                            output shouldContain "org.jetbrains.dokka.experimental.tryK2.noWarn"
                        }
                    }
            }
        }
        context("when no removed flags are set") {
            project.runner
                .addArguments(
                    ":help",
                    "--dry-run",
                )
                .build {
                    test("output should NOT contain the removed-K1-analysis warning") {
                        output shouldNotContain removedK1AnalysisWarning
                    }
                }
        }
    }
})
