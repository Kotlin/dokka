/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dependencies

import org.gradle.api.artifacts.Configuration
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.DokkaBasePlugin.Companion.DOKKA_CONFIGURATION_NAME
import org.jetbrains.dokka.gradle.DokkaBasePlugin.Companion.DOKKA_GENERATOR_PLUGINS_CONFIGURATION_NAME
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.HasFormatName

/**
 * Names of the Gradle [Configuration]s used by the [Dokkatoo Plugin][DokkaBasePlugin].
 *
 * Beware the confusing terminology:
 * - [Gradle Configurations][org.gradle.api.artifacts.Configuration] - share files between subprojects. Each has a name.
 * - [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration] - parameters for executing the Dokka Generator
 */
@DokkaInternalApi
class DependencyContainerNames(override val formatName: String) : HasFormatName() {

    val dokkatoo = DOKKA_CONFIGURATION_NAME.appendFormat()
    val dokkatooResolver = "${dokkatoo}Resolver"

    /**
     * ### Dokka Plugins
     *
     * Includes transitive dependencies, so this can be passed to the Dokka Generator Worker classpath.
     *
     * Will be used in user's build scripts to declare additional format-specific Dokka Plugins.
     */
    val pluginsClasspath = DOKKA_GENERATOR_PLUGINS_CONFIGURATION_NAME.appendFormat()

    /**
     * ### Dokka Plugins (excluding transitive dependencies)
     *
     * Will be used to create Dokka Generator Parameters
     *
     * Extends [pluginsClasspath]
     *
     * Internal Dokkatoo usage only.
     */
    val pluginsClasspathIntransitiveResolver =
        "${dokkatoo}PluginsClasspathIntransitiveResolver"

    /**
     * ### Dokka Generator Classpath
     *
     * Classpath used to execute the Dokka Generator.
     *
     * Extends [pluginsClasspath], so Dokka plugins and their dependencies are included.
     */
    val generatorClasspath = "${dokkatoo}GeneratorClasspath"

    /** Resolver for [generatorClasspath] - internal Dokkatoo usage only. */
    val generatorClasspathResolver = "${dokkatoo}GeneratorClasspathResolver"

    val publicationPluginClasspath = "${dokkatoo}PublicationPluginClasspath"
    val publicationPluginClasspathApiOnly = "${publicationPluginClasspath}ApiOnly"
    val publicationPluginClasspathResolver = "${publicationPluginClasspath}Resolver"
    val publicationPluginClasspathApiOnlyConsumable =
        "${publicationPluginClasspathApiOnly}Consumable"
}
