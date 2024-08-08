/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.dokka.gradle.formats.DokkaHtmlPlugin
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.Companion.pluginFeaturesService
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
        val pluginFeaturesService = target.pluginFeaturesService
        if (pluginFeaturesService.enableV2Plugin) {
            with(target.pluginManager) {
                apply(type = DokkaBasePlugin::class)
                // auto-apply the HTML plugin
                apply(type = DokkaHtmlPlugin::class)
            }
        } else {
            target.pluginManager.apply(ClassicDokkaPlugin::class)
        }
    }

    companion object
}
