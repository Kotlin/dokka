/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContainOnlyOnce
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject
import org.jetbrains.dokka.gradle.utils.shouldNotContainAnyOf

class TryK2MessagesTest : FunSpec({

    context("given multi-module project") {

        // Test with a multi-module project to verify that even though there
        // are multiple subprojects with Dokka only one message is logged.
        val project = initMultiModuleProject("TryK2MessagesTest")

        context("when K2 enabled") {
            project.runner

                .addArguments(
                    ":dokkaGenerate",
                    "-P$K2_ANALYSIS_ENABLED_FLAG=true",
                )
                .addArguments()
                .build {
                    test("output should contain K2 analysis warning") {
                        output shouldContainOnlyOnce """
                            ┌───────────────────────────────────────────────────────────────────────┐
                            │ Dokka K2 Analysis is enabled                                          │
                            │                                                                       │
                            │   This feature is Experimental and is still under active development. │
                            │   It can cause build failures or generate incorrect documentation.    │
                            │                                                                       │
                            │   We would appreciate your feedback!                                  │
                            │   Please report any feedback or problems to Dokka GitHub Issues       │
                            │       https://github.com/Kotlin/dokka/issues/                         │
                            │                                                                       │
                            │   You can suppress this message by adding                             │
                            │       org.jetbrains.dokka.experimental.tryK2.noWarn=true              │
                            │   to your project's `gradle.properties`                               │
                            └───────────────────────────────────────────────────────────────────────┘
                            """.trimIndent()
                    }
                }

            listOf(
                K2_ANALYSIS_NO_WARN_FLAG,
                K2_ANALYSIS_NO_WARN_FLAG_PRETTY,
            ).forEach { noWarnFlag ->
                context("and message is suppressed with $noWarnFlag") {
                    project.runner
                        .addArguments(
                            ":dokkaGenerate",
                            "-P$K2_ANALYSIS_ENABLED_FLAG=true",
                            "-P$noWarnFlag=true",
                        )
                        .build {
                            test("output should not contain any Dokka plugin message") {
                                output.shouldNotContainAnyOf(
                                    "Dokka K2 Analysis",
                                    "https://github.com/Kotlin/dokka/issues/",
                                    "org.jetbrains.dokka.experimental.gradlePlugin",
                                    K2_ANALYSIS_ENABLED_FLAG,
                                    K2_ANALYSIS_NO_WARN_FLAG,
                                    K2_ANALYSIS_NO_WARN_FLAG_PRETTY,
                                    noWarnFlag,
                                )
                            }
                        }
                }
            }
        }
    }
}) {
    companion object {

        private const val K2_ANALYSIS_ENABLED_FLAG =
            "org.jetbrains.dokka.experimental.tryK2"

        private const val K2_ANALYSIS_NO_WARN_FLAG =
            "$K2_ANALYSIS_ENABLED_FLAG.nowarn"

        private const val K2_ANALYSIS_NO_WARN_FLAG_PRETTY =
            "$K2_ANALYSIS_ENABLED_FLAG.noWarn"
    }
}
