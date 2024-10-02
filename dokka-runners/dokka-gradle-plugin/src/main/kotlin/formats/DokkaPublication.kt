/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.formats

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.DokkaPluginParametersContainer
import org.jetbrains.dokka.gradle.internal.adding
import java.io.Serializable
import javax.inject.Inject

/**
 * A [DokkaPublication] controls the output running the Dokka Generator.
 *
 * The output format (for example, HTML) is determined by the Dokka Plugins that are present.
 *
 * Each Dokka Publication has its own set of Gradle tasks and [org.gradle.api.artifacts.Configuration]s.
 */
abstract class DokkaPublication
@DokkaInternalApi
@Inject
constructor(
    /**
     * The identifier of the generated output format.
     *
     * For example, `html` or `javadoc`.
     *
     * The value is case-sensitive.
     */
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

    /** @see formatName */
    override fun getName(): String = formatName

    /**
     * Controls whether Dokka should generate documentation using this publication.
     */
    abstract val enabled: Property<Boolean>

    /** @see org.jetbrains.dokka.gradle.DokkaExtension.moduleName */
    abstract val moduleName: Property<String>

    /** @see org.jetbrains.dokka.gradle.DokkaExtension.moduleVersion */
    abstract val moduleVersion: Property<String>

    /** Output directory for the finished Dokka publication. */
    abstract val outputDirectory: DirectoryProperty

    /**
     * Whether to resolve remote files/links over the network.
     *
     * This includes package-lists used for generating external documentation links.
     * For example, to make classes from the standard library clickable in the generated output.
     *
     * Setting this to `true` can significantly speed up build times in certain cases,
     * but can also worsen documentation quality and user experience.
     * For example, by not resolving class/member links from your dependencies, including the standard library.
     *
     * Note: You can cache files locally and provide them to Dokka as local paths.
     * See [DokkaSourceSetSpec.externalDocumentationLinks].
     */
    abstract val offlineMode: Property<Boolean>

    /**
     * Whether to fail documentation generation if Dokka has emitted a warning or an error.
     * The process waits until all errors and warnings have been emitted first.
     *
     * This setting works well with [DokkaSourceSetSpec.reportUndocumented].
     */
    abstract val failOnWarning: Property<Boolean>

    /**
     * Whether to suppress obvious functions.
     *
     * A function is considered to be obvious if it is:
     *
     * - Inherited from [kotlin.Any], [kotlin.Enum], [java.lang.Object] or [java.lang.Enum],
     *   such as `equals()`, `hashCode()`, `toString()` functions.
     * - Synthetic (generated by the compiler) and does not have any documentation,
     *   such as `data class` `componentN()` or `copy()` functions.
     */
    abstract val suppressObviousFunctions: Property<Boolean>

    /**
     * Whether to suppress inherited members that aren't explicitly overridden in a given class.
     *
     * Note: This can suppress functions such as `equals()`, `hashCode()`, or `toString()`,
     * but cannot suppress synthetic functions such as `data class` `componentN()` or `copy()`.
     * Instead, use [suppressObviousFunctions].
     */
    abstract val suppressInheritedMembers: Property<Boolean>

    /**
     * A list of Markdown files that contain module and package documentation.
     *
     * The contents of the specified files are parsed and embedded into documentation as module and package descriptions.
     *
     * The format of the Markdown files is very specific and must be exactly correct to be readable by Dokka.
     *
     * See the
     * [Dokka Gradle example](https://github.com/Kotlin/dokka/tree/master/examples/gradle-v2/basic-gradle-example)
     * for an example of what it looks like and how to use it.
     */
    abstract val includes: ConfigurableFileCollection

    /**
     * A shared cache directory used by DokkaGenerator.
     *
     * (Aside: This property is not used by any official Dokka plugins.
     * It has been retained in case third-party Dokka plugins use it.)
     */
    abstract val cacheRoot: DirectoryProperty

    /**
     * Controls whether [org.jetbrains.dokka.DokkaGenerator] will forcibly stop coroutines.
     *
     * This is an internal Dokka Gradle plugin property.
     * If you find you need to set this property, please report your use-case https://kotl.in/dokka-issues.
     */
    abstract val finalizeCoroutines: Property<Boolean>

    /** Output directory for the partial Dokka module. */
    internal abstract val moduleOutputDirectory: DirectoryProperty
}
