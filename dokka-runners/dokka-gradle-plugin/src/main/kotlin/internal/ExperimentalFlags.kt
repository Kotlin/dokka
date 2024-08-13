/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

internal data class ExperimentalFlags(
    val dokkatooEnabled: Boolean,
    val dokkatooEnabledQuietly: Boolean,
) {

    companion object {
        @Suppress("ConstPropertyName")
        private const val GradlePluginMode = "org.jetbrains.dokka.experimental.gradlePlugin"

        fun Project.dokkaMode(): ExperimentalFlags {

            val flags = providers
                .gradleProperty(GradlePluginMode)
                .forUseAtConfigurationTimeCompat()
                .orElse(
                    project.provider { project.extra.properties[GradlePluginMode]?.toString() ?: "" }
                )
                .orNull
                ?.split(",")
                .orEmpty()
                .map { it.lowercase() }
                .toSet()

            val dokkatooEnabledQuietly = "dokkatoo!" in flags
            val dokkatooEnabled = dokkatooEnabledQuietly || "dokkatoo" in flags

            return ExperimentalFlags(
                dokkatooEnabledQuietly = dokkatooEnabledQuietly,
                dokkatooEnabled = dokkatooEnabled,
            )
        }
    }
}
