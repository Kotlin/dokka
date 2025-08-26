/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
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

        val logLevels = listOf(WARN, LIFECYCLE)
        val dgpFlags = listOf(null, "V1Enabled", "V2EnabledWithHelpers", "V2Enabled")
        val noWarns = listOf(null, true, false)

        dgpFlags.forEach { dgpFlag ->
            noWarns.forEach { noWarn ->
                logLevels.forEach { logLevel ->
                    context("when dgpFlag=$dgpFlag, pluginModeNoWarn=$noWarn, log level=$logLevel") {

                        val output = project.getOutput(
                            pluginMode = dgpFlag,
                            pluginModeNoWarn = noWarn,
                            logLevel = logLevel,
                        )

                        val shouldContainV1Warning = when (noWarn) {
                            true -> false
                            false -> dgpFlag == "V1Enabled"
                            null -> dgpFlag == "V1Enabled"
                        }
                        if (shouldContainV1Warning) {
                            test("output should contain V1 warning") {
                                output shouldContainOnlyOnce expectedV1Warning
                            }
                        } else {
                            test("output should NOT contain V1 warning") {
                                output shouldNotContain expectedV1Warning
                            }
                        }

                        val shouldContainV1Message = when (noWarn) {
                            true -> false
                            false -> dgpFlag == "V1Enabled" && logLevel == LIFECYCLE
                            null -> dgpFlag == "V1Enabled" && logLevel == LIFECYCLE
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
    }
}
