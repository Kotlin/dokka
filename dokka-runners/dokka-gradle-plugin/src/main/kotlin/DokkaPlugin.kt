/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.apply
import org.jetbrains.dokka.gradle.formats.DokkaHtmlPlugin
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.ExperimentalFlags.Companion.dokkaMode
import javax.inject.Inject
import org.jetbrains.dokka.gradle.DokkaClassicPlugin as ClassicDokkaPlugin

/**
 * Dokka Gradle Plugin.
 *
 * Creates all necessary defaults to generate API source code documentation for HTML.
 */
abstract class DokkaPlugin
@Inject
@DokkaInternalApi
constructor() : Plugin<Project> {

    override fun apply(target: Project) {
        val dokkaMode = target.dokkaMode()
        if (dokkaMode.dokkatooEnabled) {
            if (!dokkaMode.dokkatooEnabledQuietly) {
                logger.lifecycle("[${target.displayName}] Dokka Gradle Plugin: Dokkatoo mode enabled")
            }
            with(target.pluginManager) {
                apply(type = DokkaBasePlugin::class)

                // auto-apply the custom format plugins
                apply(type = DokkaHtmlPlugin::class)
            }
        } else {
            logger.lifecycle("[${target.path}] Using Dokka Classic - please migrate, see \$linkToDGPMigrationDocs")
            target.pluginManager.apply(ClassicDokkaPlugin::class)
        }
    }

    companion object {
        private val logger = Logging.getLogger(DokkaPlugin::class.java)
    }
}
