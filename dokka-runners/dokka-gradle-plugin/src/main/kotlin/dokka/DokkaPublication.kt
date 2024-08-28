/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dokka

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.DokkaPluginParametersContainer
import org.jetbrains.dokka.gradle.internal.adding
import java.io.Serializable
import javax.inject.Inject

/**
 * A [DokkaPublication] describes a single Dokka output.
 *
 * Each Publication has its own set of Gradle tasks and [org.gradle.api.artifacts.Configuration]s.
 *
 * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
 * By default, Dokka will create publications for HTML, Jekyll, and GitHub Flavoured Markdown.
 */
abstract class DokkaPublication
@DokkaInternalApi
@Inject
constructor(
    val formatName: String,

    /**
     * Configurations for Dokka Generator Plugins. Must be provided from
     * [org.jetbrains.dokka.gradle.DokkaExtension.pluginsConfiguration].
     */
    pluginsConfiguration: DokkaPluginParametersContainer,
) : Named, Serializable, ExtensionAware {

    /** Configurations for Dokka Generator Plugins. */
    val pluginsConfiguration: DokkaPluginParametersContainer =
        extensions.adding("pluginsConfiguration", pluginsConfiguration)

    override fun getName(): String = formatName

    abstract val enabled: Property<Boolean>

    abstract val moduleName: Property<String>

    abstract val moduleVersion: Property<String>

    /** Output directory for the finished Dokka publication. */
    abstract val outputDirectory: DirectoryProperty

    abstract val cacheRoot: DirectoryProperty

    abstract val offlineMode: Property<Boolean>

    abstract val failOnWarning: Property<Boolean>

    abstract val suppressObviousFunctions: Property<Boolean>

    abstract val includes: ConfigurableFileCollection

    abstract val suppressInheritedMembers: Property<Boolean>

    // TODO probably not needed any more, since Dokka Generator now runs in an isolated JVM process
    abstract val finalizeCoroutines: Property<Boolean>

    /** Output directory for the partial Dokka module. */
    internal abstract val moduleOutputDirectory: DirectoryProperty
}
