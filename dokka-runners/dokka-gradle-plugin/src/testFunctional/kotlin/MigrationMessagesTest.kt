/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
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

        context("when no DGP flag is set") {
            val dgpFlag = null

            context("and log level is WARN") {
                val output = project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = null, logLevel = WARN)

                testMessagesInOutput(output = output)
            }

            context("and log level is LIFECYCLE") {

                context("and pluginModeNoWarn is unset") {
                    val output = project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = null, logLevel = LIFECYCLE)

                    testMessagesInOutput(output = output)
                }

                context("and pluginModeNoWarn=true") {
                    val output = project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = true, logLevel = LIFECYCLE)

                    testMessagesInOutput(output = output)
                }
            }
        }

        context("when V1Enabled DGP flag is set") {
            val dgpFlag = "V1Enabled"

            context("and log level is WARN") {
                val output = project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = null, logLevel = WARN)

                testMessagesInOutput(
                    output = output,
                    shouldContainV1Warning = true,
                )
            }

            context("and log level is LIFECYCLE") {

                context("and pluginModeNoWarn is unset") {
                    val output =
                        project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = null, logLevel = LIFECYCLE)

                    testMessagesInOutput(
                        output = output,
                        shouldContainV1Warning = true,
                        shouldContainV1Message = true,
                    )
                }

                context("and pluginModeNoWarn=true") {
                    val output =
                        project.getOutput(pluginMode = dgpFlag, pluginModeNoWarn = true, logLevel = LIFECYCLE)

                    testMessagesInOutput(output = output)
                }
            }
        }

        context("when V2EnabledWithHelpers DGP flag is set") {
            val pluginMode = "V2EnabledWithHelpers"

            context("and log level is WARN") {
                val output = project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = null, logLevel = WARN)

                testMessagesInOutput(output = output)
            }

            context("and log level is LIFECYCLE") {

                context("and pluginModeNoWarn is unset") {
                    val output =
                        project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = null, logLevel = LIFECYCLE)

                    testMessagesInOutput(output = output)
                }

                context("and pluginModeNoWarn=true") {
                    val output =
                        project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = true, logLevel = LIFECYCLE)

                    testMessagesInOutput(output = output)
                }
            }
        }

        context("when V2Enabled DGP flag is set") {
            val pluginMode = "V2Enabled"

            context("and log level is WARN") {
                val output = project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = null, logLevel = WARN)

                testMessagesInOutput(output = output)
            }

            context("and log level is LIFECYCLE") {

                context("and pluginModeNoWarn is unset") {
                    val output =
                        project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = null, logLevel = LIFECYCLE)

                    testMessagesInOutput(output = output)
                }

                context("and pluginModeNoWarn=true") {
                    val output =
                        project.getOutput(pluginMode = pluginMode, pluginModeNoWarn = true, logLevel = LIFECYCLE)

                    testMessagesInOutput(output = output)
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
                add(":help")
                add("--dry-run")
                pluginMode?.let { add("-P$PLUGIN_MODE_FLAG=$it") }
                if (logLevel != LIFECYCLE) add("--${logLevel.name.lowercase()}")
                pluginModeNoWarn?.let { add("-P$PLUGIN_MODE_NO_WARN_FLAG_PRETTY=$it") }
            }
            return runner
                .addArguments(args)
                .build().output
        }

        private suspend fun FunSpecContainerScope.testMessagesInOutput(
            output: String,
            shouldContainV1Warning: Boolean = false,
            shouldContainV1Message: Boolean = false,
            shouldContainV2MigrationWarning: Boolean = false,
            shouldContainV2EnabledMessage: Boolean = false,
        ) {
            if (shouldContainV1Warning) {
                test("output should contain V1 warning") {
                    output shouldContainOnlyOnce expectedV1Warning
                }
            } else {
                test("output should NOT contain V1 warning") {
                    output shouldNotContain expectedV1Warning
                }
            }

            if (shouldContainV1Message) {
                test("output should contain V1 message") {
                    output shouldContainOnlyOnce expectedV1Message
                }
            } else {
                test("output should NOT contain V1 message") {
                    output shouldNotContain expectedV1Message
                    output shouldNotContain expectedV1Message.trim().lines().first()
                }
            }

            if (shouldContainV2MigrationWarning) {
                test("output should contain V2 migration warning") {
                    output shouldContainOnlyOnce expectedV2MigrationWarning
                }
            } else {
                test("output should NOT contain V2 migration warning") {
                    output shouldNotContain expectedV2MigrationWarning
                    output shouldNotContain expectedV2MigrationWarning.trim().lines().first()
                }
            }

            if (shouldContainV2EnabledMessage) {
                test("output should contain V2 enabled message") {
                    output shouldContainOnlyOnce expectedV2Message
                }
            } else {
                test("output should NOT contain V2 message") {
                    output shouldNotContain expectedV2Message
                    output shouldNotContain expectedV2Message.trim().lines().first()
                }
            }
        }


        private const val PLUGIN_MODE_FLAG =
            "org.jetbrains.dokka.experimental.gradle.pluginMode"

        private const val PLUGIN_MODE_NO_WARN_FLAG_PRETTY =
            "org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn"

        private val expectedV1Warning = /* language=text */ """
            |warning: Dokka Gradle plugin V1 is deprecated
            """.trimMargin()

        private val expectedV1Message = /* language=text */ """
            |Dokka Gradle plugin V1 is deprecated, and will be removed in Dokka version 2.2.0
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
