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
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject

class TryK2MessagesTest : FunSpec({

    context("given multi-module project") {

        // Test with a multi-module project to verify that even though there
        // are multiple subprojects with Dokka only one message is logged.
        val project = initMultiModuleProject("TryK2MessagesTest")

        val k2Flags = listOf(null, true, false)
        val noWarns = listOf(null, true, false)
        val logLevels = listOf(WARN, LIFECYCLE)

        k2Flags.forEach { k2Enabled ->
            noWarns.forEach { noWarn ->
                logLevels.forEach { logLevel ->
                    context("when k2Enabled=$k2Enabled, pluginModeNoWarn=$noWarn, log level=$logLevel") {

                        val output = project.getOutput(
                            k2Enabled = k2Enabled,
                            k2NoWarn = noWarn,
                            logLevel = logLevel,
                        )

                        val shouldContainK1Warning = when (noWarn) {
                            true -> false
                            false -> k2Enabled == false
                            null -> k2Enabled == false
                        }
                        if (shouldContainK1Warning) {
                            test("output should contain K1 warning") {
                                output shouldContainOnlyOnce expectedK1AnalysisWarning
                            }
                        } else {
                            test("output should NOT contain K1 warning") {
                                output shouldNotContain expectedK1AnalysisWarning
                            }
                        }

                        val shouldContainK1Message = when (noWarn) {
                            true -> false
                            false -> k2Enabled == false && logLevel == LIFECYCLE
                            null -> k2Enabled == false && logLevel == LIFECYCLE
                        }
                        if (shouldContainK1Message) {
                            test("output should contain K1 message") {
                                output shouldContainOnlyOnce expectedK1AnalysisMessage
                            }
                        } else {
                            test("output should NOT contain V1 message") {
                                output shouldNotContain expectedK1AnalysisMessage
                                output shouldNotContain expectedK1AnalysisMessage.trim().lines().first()
                            }
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
            "$K2_ANALYSIS_ENABLED_FLAG.noWarn"

        private val expectedK1AnalysisWarning = /* language=text */ """
            |warning: Dokka K1 Analysis is enabled
            """.trimMargin()

        private val expectedK1AnalysisMessage = /* language=text */ """
            |Dokka K1 Analysis is deprecated. It can cause build failures or generate incorrect documentation.
            |We recommend using Dokka K2 Analysis, which supports new language features like context parameters.
            |
            |To start using Dokka K2 Analysis remove
            |    $K2_ANALYSIS_ENABLED_FLAG=false
            |in your project's `gradle.properties` file.
            |
            |We would appreciate your feedback!
            | - Please report any feedback or problems https://kotl.in/dokka-issues
            | - Chat with the community visit #dokka in https://kotlinlang.slack.com/ (To sign up visit https://kotl.in/slack)
            """.trimMargin().prependIndent()

        private fun GradleProjectTest.getOutput(
            k2Enabled: Boolean?,
            k2NoWarn: Boolean?,
            logLevel: LogLevel,
        ): String {
            val args = buildList {
                add(":help")
                add("--dry-run")
                k2Enabled?.let { add("-P$K2_ANALYSIS_ENABLED_FLAG=$it") }
                if (logLevel != LIFECYCLE) add("--${logLevel.name.lowercase()}")
                k2NoWarn?.let { add("-P$K2_ANALYSIS_NO_WARN_FLAG=$it") }
            }
            return runner
                .addArguments(args)
                .build().output
        }
    }
}
