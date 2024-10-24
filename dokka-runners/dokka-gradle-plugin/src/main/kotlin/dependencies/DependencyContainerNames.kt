/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dependencies

import org.gradle.api.artifacts.Configuration
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.DokkaBasePlugin.Companion.DOKKA_CONFIGURATION_NAME
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.HasFormatName
import org.jetbrains.dokka.gradle.internal.INTERNAL_CONF_NAME_TAG

/**
 * Names of the Gradle [Configuration]s used by the [Dokka Plugin][DokkaBasePlugin].
 *
 * Beware the confusing terminology:
 * - [Gradle Configurations][org.gradle.api.artifacts.Configuration] - share files between subprojects. Each has a name.
 * - [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration] - parameters for executing the Dokka Generator
 */
@InternalDokkaGradlePluginApi
class DependencyContainerNames(override val formatName: String) : HasFormatName() {

    /**
     * Base declarable configuration, for aggregating Dokka Modules.
     */
    val dokka = DOKKA_CONFIGURATION_NAME.appendFormat()

    /**
     * ### Dokka Plugins
     *
     * Includes transitive dependencies, so this can be passed to the Dokka Generator Worker classpath.
     *
     * Will be used in user's build scripts to declare additional format-specific Dokka Plugins.
     */
    val pluginsClasspath = "${dokka}Plugin"

    /**
     * ### Dokka Plugins (excluding transitive dependencies)
     *
     * Will be used to create Dokka Generator Parameters
     *
     * Extends [pluginsClasspath]
     *
     * Internal Dokka Gradle Plugin usage only.
     */
    val pluginsClasspathIntransitiveResolver =
        "${pluginsClasspath}IntransitiveResolver${INTERNAL_CONF_NAME_TAG}"

    /**
     * ### Dokka Generator Classpath
     *
     * Classpath used to execute the Dokka Generator.
     *
     * Extends [pluginsClasspath], so Dokka plugins and their dependencies are included.
     */
    val generatorClasspath = "${dokka}GeneratorRuntime"

    /** Resolver for [generatorClasspath] - internal Dokka usage only. */
    val generatorClasspathResolver = "${generatorClasspath}Resolver${INTERNAL_CONF_NAME_TAG}"

    /**
     * ### Dokka Publication Plugins Classpath
     *
     * Dokka Plugins used specifically for creating a finished Dokka publication.
     */
    val publicationPluginClasspath = "${dokka}PublicationPlugin"

    /** Resolver for [publicationPluginClasspath] - internal Dokka usage only. */
    val publicationPluginClasspathResolver = "${publicationPluginClasspath}Resolver${INTERNAL_CONF_NAME_TAG}"

    /**
     * ### Dokka Publication Plugins API Only Classpath
     *
     * Dokka can aggregate multiple 'partial' Modules into a single Publication.
     * Aggregation sometimes requires specific plugins.
     * Dokka Plugins that are required by Dokka Generator *consumers* of a Module to create an aggregated Publication.
     */
    val publicationPluginClasspathApiOnly = "${publicationPluginClasspath}ApiOnly${INTERNAL_CONF_NAME_TAG}"

    /** Consumable [publicationPluginClasspathApiOnly] - internal Dokka usage only. */
    val publicationPluginClasspathApiOnlyConsumable =
        "${publicationPluginClasspath}ApiOnlyConsumable${INTERNAL_CONF_NAME_TAG}"

}
