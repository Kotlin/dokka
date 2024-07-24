/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.apply
import org.jetbrains.dokka.gradle.formats.DokkatooGfmPlugin
import org.jetbrains.dokka.gradle.formats.DokkatooHtmlPlugin
import org.jetbrains.dokka.gradle.formats.DokkatooJavadocPlugin
import org.jetbrains.dokka.gradle.formats.DokkatooJekyllPlugin
import org.jetbrains.dokka.gradle.internal.CurrentGradleVersion
import org.jetbrains.dokka.gradle.internal.DokkatooInternalApi
import org.jetbrains.dokka.gradle.internal.compareTo
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import javax.inject.Inject
import org.jetbrains.dokka.gradle.DokkaPlugin as ClassicDokkaPlugin

/**
 * Dokkatoo Gradle Plugin.
 *
 * Creates all necessary defaults to generate documentation for HTML, Jekyll, Markdown, and Javadoc formats.
 */
abstract class DokkatooPlugin
@Inject
@DokkatooInternalApi
constructor(
    private val providers: ProviderFactory,
) : Plugin<Project> {

    override fun apply(target: Project) {
        if (isDokkatooEnabled(target)) {
            with(target.pluginManager) {
                apply(type = DokkatooBasePlugin::class)

                // auto-apply the custom format plugins
                apply(type = DokkatooGfmPlugin::class)
                apply(type = DokkatooHtmlPlugin::class)
                apply(type = DokkatooJavadocPlugin::class)
                apply(type = DokkatooJekyllPlugin::class)
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
