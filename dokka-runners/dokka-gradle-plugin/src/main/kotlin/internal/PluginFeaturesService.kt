/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.registerIfAbsent

/**
 * Internal utility service for managing Dokka Plugin features and warnings.
 *
 * Using a [BuildService] is most useful for only logging a single warning for the whole project,
 * regardless of how many subprojects have applied DGP.
 */
internal abstract class PluginFeaturesService : BuildService<PluginFeaturesService.Params> {

    interface Params : BuildServiceParameters {
        /**
         * Whether DGP should use V2 [org.jetbrains.dokka.gradle.DokkaBasePlugin].
         *
         * Otherwise, fallback to V1 [org.jetbrains.dokka.gradle.DokkaClassicPlugin].
         */
        val enableV2Plugin: Property<Boolean>

        /** If `true`, suppress [PluginFeaturesService.logV2Message]. */
        val suppressV2PluginMessage: Property<Boolean>
    }

    internal val enableV2Plugin: Boolean get() = parameters.enableV2Plugin.get()
    private val suppressV2PluginMessage: Boolean get() = parameters.suppressV2PluginMessage.get()

    /** Used to only log the V1 message once per project, regardless of how many subprojects there are. */
    private var v1WarningLogged: Boolean = false

    /** Used to only log the V2 message once per project, regardless of how many subprojects there are. */
    private var v2MessageLogged: Boolean = false

    fun logV1Warning() {
        if (!v1WarningLogged) {
            logger.warn(
                """
                |⚠ Warning: Dokka Gradle Plugin V1 mode is enabled
                |
                |  V1 mode is deprecated, and will be removed in Dokka version 2.0.20.
                |
                |  Please migrate Dokka Gradle Plugin to V2. This will require updating your project.
                |  To get started check out the Dokka Gradle Plugin Migration guide
                |      https://kotl.in/dokka-gradle-migration
                |
                |  Once you have prepared your project, enable V2 by adding
                |      $V2_PLUGIN_ENABLED_FLAG=true
                |  to your project's `gradle.properties`
                |
                |  Please report any feedback or problems to Dokka GitHub Issues
                |      https://github.com/Kotlin/dokka/issues/
                """.trimMargin().surroundWithBorder()
            )
            v1WarningLogged = true
        }
    }

    fun logV2Message() {
        if (!suppressV2PluginMessage && !v2MessageLogged) {
            logger.lifecycle(
                """
                |Dokka Gradle Plugin V2 is enabled ♡
                |
                |  We would appreciate your feedback!
                |  Please report any feedback or problems to Dokka GitHub Issues
                |      https://github.com/Kotlin/dokka/issues/
                |
                |  If you need help or advice, check out the migration guide
                |      https://kotl.in/dokka-gradle-migration
                |
                |  You can suppress this message by adding
                |      $V2_PLUGIN_MESSAGE_SUPPRESSED_FLAG=true
                |  to your project's `gradle.properties`
                """.trimMargin().surroundWithBorder()
            )
            v2MessageLogged = true
        }
    }

    companion object {
        private val logger = Logging.getLogger(PluginFeaturesService::class.java)

        /**
         * Register a new [PluginFeaturesService], or get an existing instance.
         */
        internal val Project.pluginFeaturesService: PluginFeaturesService
            get() = gradle.sharedServices
                .registerIfAbsent("PluginFeaturesService.internal", PluginFeaturesService::class) {
                    parameters {
                        enableV2Plugin.set(getFlag(V2_PLUGIN_ENABLED_FLAG))
                        suppressV2PluginMessage.set(getFlag(V2_PLUGIN_MESSAGE_SUPPRESSED_FLAG))
                    }
                }.get()

        internal const val V2_PLUGIN_ENABLED_FLAG =
            "org.jetbrains.dokka.experimental.gradlePlugin.enableV2"

        internal const val V2_PLUGIN_MESSAGE_SUPPRESSED_FLAG =
            "org.jetbrains.dokka.experimental.gradlePlugin.suppressV2Message"

        private fun Project.getFlag(flag: String): Boolean =
            providers
                .gradleProperty(flag)
                .forUseAtConfigurationTimeCompat()
                .orElse(
                    // Note: Enabling/disabling features via extra-properties is only intended for unit tests.
                    // (Because org.gradle.testfixtures.ProjectBuilder doesn't support mocking Gradle properties.)
                    project
                        .provider { project.extra.properties[flag]?.toString() ?: "" }
                        .forUseAtConfigurationTimeCompat()
                )
                .orNull
                .toBoolean()


        /**
         * Draw a pretty ascii border around some text.
         * This helps with logging a multiline message, so it is easier to view.
         */
        private fun String.surroundWithBorder(): String {
            val lines = lines()
            val maxLength = lines.maxOf { it.length }
            val horizontalBorder = "─".repeat(maxLength)

            return buildString {
                appendLine("┌─$horizontalBorder─┐")
                lines.forEach { line ->
                    val paddedLine = line.padEnd(maxLength, padChar = ' ')
                    appendLine("│ $paddedLine │")
                }
                appendLine("└─$horizontalBorder─┘")
            }
        }
    }
}
