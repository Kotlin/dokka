/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.api.logging.LogLevel.WARN
import org.jetbrains.dokka.gradle.utils.GradleProjectTest
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.projects.initNoConfigMultiModuleProject

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

        listOf(
            null,
            "$PLUGIN_MODE_FLAG=V1Enabled",
        ).forEach { dgpFlag ->
            context("when ${dgpFlag ?: "no"} DGP flag is set") {

                context("and log level is WARN") {
                    val output = project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = null, logLevel = WARN)

                    test("output should contain V1 warning") {
                        output shouldContainOnlyOnce expectedV1Warning
                    }
                    test("output should NOT contain V1 message") {
                        output shouldNotContain expectedV1Message
                    }
                }

                context("and log level is LIFECYCLE") {

                    context("and pluginModeNoWarn is unset") {
                        val output =
                            project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = null, logLevel = LIFECYCLE)

                        test("output should contain V1 warning") {
                            output shouldContainOnlyOnce expectedV1Warning
                        }
                        test("output should contain V1 message") {
                            output shouldContainOnlyOnce expectedV1Message
                        }
                    }

                    context("and pluginModeNoWarn=true") {
                        val output =
                            project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = true, logLevel = LIFECYCLE)

                        test("output should NOT contain V1 warning") {
                            output shouldNotContain expectedV1Warning
                        }
                        test("output should NOT contain V1 message") {
                            output shouldNotContain expectedV1Message
                        }
                    }
                }
            }
        }

        context("when V2EnabledWithHelpers DGP flag is set") {
            val pluginMode = "V2EnabledWithHelpers"

            context("and log level is WARN") {
                val output = project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = null, logLevel = WARN)

                test("output should contain V2 migration helpers warning") {
                    output shouldContainOnlyOnce expectedV2MigrationWarning
                }
                test("output should NOT contain V2 migration helpers message") {
                    output shouldNotContain expectedV2MigrationMessage
                    output shouldNotContain expectedV2MigrationMessage.trim().lines().first()
                }
            }

            context("and log level is LIFECYCLE") {

                context("and pluginModeNoWarn is unset") {
                    val output =
                        project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = null, logLevel = LIFECYCLE)

                    test("output should contain V2 migration helpers warning") {
                        output shouldContainOnlyOnce expectedV2MigrationWarning
                    }
                    test("output should contain V2 migration helpers message") {
                        output shouldContainOnlyOnce expectedV2MigrationMessage
                        output shouldContainOnlyOnce expectedV2MigrationMessage.trim().lines().first()
                    }
                }

                context("and pluginModeNoWarn=true") {
                    val output =
                        project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = true, logLevel = LIFECYCLE)

                    test("output should contain V2 migration helpers warning") {
                        output shouldContainOnlyOnce expectedV2MigrationWarning
                    }
                    test("output should NOT contain V2 migration helpers message") {
                        output shouldNotContain expectedV2MigrationMessage
                        output shouldNotContain expectedV2MigrationMessage.trim().lines().first()
                    }
                }
            }
        }

        context("when V2Enabled DGP flag is set") {
            val pluginMode = "V2Enabled"

            context("and log level is WARN") {
                val output = project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = null, logLevel = WARN)

                test("output should NOT contain V2 message") {
                    output shouldNotContain expectedV2Message
                }
            }

            context("and log level is LIFECYCLE") {

                context("and pluginModeNoWarn is unset") {
                    val output =
                        project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = null, logLevel = LIFECYCLE)

                    test("output should contain V2 message") {
                        output shouldContain expectedV2Message
                    }
                }

                context("and pluginModeNoWarn=true") {
                    val output =
                        project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = true, logLevel = LIFECYCLE)

                    test("output should NOT contain V2 message") {
                        output shouldNotContain expectedV2Message
                    }
                }
            }
        }
    }
}) {
    companion object {

        private fun GradleProjectTest.getOutput(
            pluginMode: String?,
            pluginModeNoWarn: Boolean?,
            logLevel: LogLevel,
        ): String {
            val args = buildList {
                this.add(":help")
                this.add("--dry-run")
                pluginMode?.let { this.add("-P$PLUGIN_MODE_FLAG=$it") }
                if (logLevel != LIFECYCLE) add("--${logLevel.name.lowercase()}")
                pluginModeNoWarn?.let { add("-P$PLUGIN_MODE_NO_WARN_FLAG_PRETTY=$it") }
            }
            return runner
                .addArguments(args)
                .build().output
        }

        private const val PLUGIN_MODE_FLAG =
            "org.jetbrains.dokka.experimental.gradle.pluginMode"

        private const val PLUGIN_MODE_NO_WARN_FLAG_PRETTY =
            "org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn"

        private val expectedV1Warning = /* language=text */ """
            |warning: Dokka Gradle plugin V1 is deprecated
            """.trimMargin()

        private val expectedV1Message = /* language=text */ """
            |Dokka Gradle plugin V1 is deprecated, and will be removed in Dokka version 2.1.0
            |Please migrate to Dokka Gradle plugin V2. This will require updating your project.
            |To learn about migrating read the migration guide https://kotl.in/dokka-gradle-migration
            |
            |To start migrating to Dokka Gradle plugin V2 add
            |    ${PLUGIN_MODE_FLAG}=V2EnabledWithHelpers
            |into your project's `gradle.properties` file.
            |
            |We would appreciate your feedback!
            | - Please report any feedback or problems https://kotl.in/dokka-issues
            | - Chat with the community visit #dokka in https://kotlinlang.slack.com/ (To sign up visit https://kotl.in/slack)
            """.trimMargin().prependIndent()

        private val expectedV2MigrationWarning = /* language=text */ """
            |warning: Dokka Gradle plugin V2 migration helpers are enabled
            """.trimMargin()

        private val expectedV2MigrationMessage = /* language=text */ """
            |Thank you for migrating to Dokka Gradle plugin V2!
            |Migration is in progress, and helpers have been enabled.
            |To learn about migrating read the migration guide https://kotl.in/dokka-gradle-migration
            |
            |Once you have finished migrating disable the migration helpers by adding
            |    ${PLUGIN_MODE_FLAG}=V2Enabled
            |to your project's `gradle.properties` file.
            |
            |We would appreciate your feedback!
            | - Please report any feedback or problems https://kotl.in/dokka-issues
            | - Chat with the community visit #dokka in https://kotlinlang.slack.com/ (To sign up visit https://kotl.in/slack)
            """.trimMargin().prependIndent()

        private val expectedV2Message = /* language=text */ """
            |Thank you for enabling Dokka Gradle plugin V2!
            |To learn about migrating read the migration guide https://kotl.in/dokka-gradle-migration
            |
            |We would appreciate your feedback!
            | - Please report any feedback or problems https://kotl.in/dokka-issues
            | - Chat with the community visit #dokka in https://kotlinlang.slack.com/ (To sign up visit https://kotl.in/slack)
            |
            |You can suppress this message by adding
            |    ${PLUGIN_MODE_NO_WARN_FLAG_PRETTY}=true
            |to your project's `gradle.properties` file.
            """.trimMargin().prependIndent()
    }
}
