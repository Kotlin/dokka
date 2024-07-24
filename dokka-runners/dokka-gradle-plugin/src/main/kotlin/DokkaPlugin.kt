/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.apply
import org.jetbrains.dokka.gradle.formats.DokkaGfmPlugin
import org.jetbrains.dokka.gradle.formats.DokkaHtmlPlugin
import org.jetbrains.dokka.gradle.formats.DokkaJavadocPlugin
import org.jetbrains.dokka.gradle.formats.DokkaJekyllPlugin
import org.jetbrains.dokka.gradle.internal.CurrentGradleVersion
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.compareTo
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import javax.inject.Inject
import org.jetbrains.dokka.gradle.DokkaClassicPlugin as ClassicDokkaPlugin

/**
 * Dokkatoo Gradle Plugin.
 *
 * Creates all necessary defaults to generate documentation for HTML, Jekyll, Markdown, and Javadoc formats.
 */
abstract class DokkaPlugin
@Inject
@DokkaInternalApi
constructor(
    private val providers: ProviderFactory,
) : Plugin<Project> {

    override fun apply(target: Project) {
        if (isDokkatooEnabled(target)) {
            with(target.pluginManager) {
                apply(type = DokkaBasePlugin::class)

                // auto-apply the custom format plugins
                apply(type = DokkaGfmPlugin::class)
                apply(type = DokkaHtmlPlugin::class)
                apply(type = DokkaJavadocPlugin::class)
                apply(type = DokkaJekyllPlugin::class)
            }
        } else {
            target.pluginManager.apply(ClassicDokkaPlugin::class)
        }
    }

    private fun isDokkatooEnabled(project: Project): Boolean {
        val enabledViaProperty =
            providers.gradleProperty("enableDokkatoo").run {
                if (CurrentGradleVersion < "7.0") {
                    @Suppress("DEPRECATION")
                    forUseAtConfigurationTime()
                } else {
                    this
                }
            }

        return enabledViaProperty.orNull.toBoolean()
                || (
                project.extraProperties.has("enableDokkatoo")
                        &&
                        project.extraProperties.get("enableDokkatoo")?.toString().toBoolean()
                )
    }
}
