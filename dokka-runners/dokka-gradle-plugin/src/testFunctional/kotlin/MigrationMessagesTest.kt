/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContainOnlyOnce
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.gradle.utils.projects.initNoConfigMultiModuleProject
import org.jetbrains.dokka.gradle.utils.shouldNotContainAnyOf

class MigrationMessagesTest : FunSpec({
    context("given multi-module project") {

        val project = initNoConfigMultiModuleProject {
            gradleProperties {
                // Disable the automatic config of these options, so we can control them
                // manually with command line flags in the tests.
                dokka {
                    pluginMode = null
                    pluginModeNoWarn = null
                }
            }
        }

        context("when no plugin flags are set") {
            project.runner
                .addArguments(
                    ":help",
                    "--dry-run",
                    "--warn",
                )
                .addArguments()
                .build {
                    test("output should contain V1 warning") {
                        shouldOnlyContainV1Warning()
                    }
                }
        }

        context("when v1 is enabled") {
            project.runner
                .addArguments(
                    ":help",
                    "--dry-run",
                    "--warn",
                    "-P$PLUGIN_MODE_FLAG=V1Enabled",
                )
                .addArguments()
                .build {
                    test("output should contain V1 warning") {
                        shouldOnlyContainV1Warning()
                    }
                }
        }

        context("when v2 is enabled") {

            listOf(
                "$PLUGIN_MODE_FLAG=V2Enabled",
                "$PLUGIN_MODE_FLAG=V2EnabledWithHelpers",
            ).forEach { v2EnabledFlag ->

                context("with flag $v2EnabledFlag") {
                    project.runner
                        .addArguments(
                            ":help",
                            "--dry-run",
                            "-P$v2EnabledFlag",
                        )
                        .build {
                            test("output should only contain V2 message") {
                                shouldOnlyContainV2Message()
                            }
                        }
                }

                listOf(
                    "$PLUGIN_MODE_NO_WARN_FLAG=true",
                    "$PLUGIN_MODE_NO_WARN_FLAG_PRETTY=true",
                ).forEach { noWarnFlag ->
                    context("and message is suppressed with $noWarnFlag") {
                        project.runner
                            .addArguments(
                                ":help",
                                "--dry-run",
                                "-P$v2EnabledFlag",
                                "-P$noWarnFlag",
                            )
                            .build {
                                test("output should not contain any Dokka plugin message") {
                                    output.shouldNotContainAnyOf(
                                        "Dokka Gradle Plugin V1",
                                        "Dokka Gradle Plugin V2",
                                        "https://kotl.in/dokka-gradle-migration",
                                        "https://github.com/Kotlin/dokka/issues/",
                                        "org.jetbrains.dokka.experimental.gradlePlugin",
                                        PLUGIN_MODE_FLAG,
                                        PLUGIN_MODE_NO_WARN_FLAG,
                                        PLUGIN_MODE_NO_WARN_FLAG_PRETTY,
                                        noWarnFlag,
                                    )
                                }
                            }
                    }
                }
            }
        }
    }
}) {
    companion object {
        private const val PLUGIN_MODE_FLAG =
            "org.jetbrains.dokka.experimental.gradle.pluginMode"

        private const val PLUGIN_MODE_NO_WARN_FLAG =
            "org.jetbrains.dokka.experimental.gradle.pluginMode.nowarn"

        private const val PLUGIN_MODE_NO_WARN_FLAG_PRETTY =
            "org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn"

        private fun BuildResult.shouldOnlyContainV1Warning() {
            output shouldContainOnlyOnce /* language=text */ """
                |┌──────────────────────────────────────────────────────────────────────────────────────┐
                |│ ⚠ Warning: Dokka Gradle Plugin V1 mode is enabled                                    │
                |│                                                                                      │
                |│   V1 mode is deprecated, and will be removed in Dokka version 2.1.0                  │
                |│                                                                                      │
                |│   Please migrate Dokka Gradle Plugin to V2. This will require updating your project. │
                |│   To get started check out the Dokka Gradle Plugin Migration guide                   │
                |│       https://kotl.in/dokka-gradle-migration                                         │
                |│                                                                                      │
                |│   Once you have prepared your project, enable V2 by adding                           │
                |│       org.jetbrains.dokka.experimental.gradle.pluginMode=V2EnabledWithHelpers        │
                |│   to your project's `gradle.properties`                                              │
                |│                                                                                      │
                |│   Please report any feedback or problems to Dokka GitHub Issues                      │
                |│       https://github.com/Kotlin/dokka/issues/                                        │
                |└──────────────────────────────────────────────────────────────────────────────────────┘
                """.trimMargin()
            output shouldNotContain "Dokka Gradle Plugin V2"
        }

        private fun BuildResult.shouldOnlyContainV2Message() {
            output shouldContainOnlyOnce /* language=text */ """
                |┌──────────────────────────────────────────────────────────────────────┐
                |│ Dokka Gradle Plugin V2 is enabled ♡                                  │
                |│                                                                      │
                |│   We would appreciate your feedback!                                 │
                |│   Please report any feedback or problems to Dokka GitHub Issues      │
                |│       https://github.com/Kotlin/dokka/issues/                        │
                |│                                                                      │
                |│   If you need help or advice, check out the migration guide          │
                |│       https://kotl.in/dokka-gradle-migration                         │
                |│                                                                      │
                |│   You can suppress this message by adding                            │
                |│       org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn=true │
                |│   to your project's `gradle.properties`                              │
                |└──────────────────────────────────────────────────────────────────────┘
                """.trimMargin()
            output shouldNotContain "Dokka Gradle Plugin V1"
        }
    }
}
