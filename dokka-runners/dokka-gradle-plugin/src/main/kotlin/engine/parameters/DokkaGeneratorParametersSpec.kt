/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.dokka.gradle.internal.DokkaPluginParametersContainer
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.adding
import org.jetbrains.dokka.gradle.internal.domainObjectContainer
import javax.inject.Inject

/**
 * Parameters used to run Dokka Generator to produce either a
 * Dokka Publication or a Dokka Module.
 */
abstract class DokkaGeneratorParametersSpec
@DokkaInternalApi
@Inject
constructor(
    objects: ObjectFactory,
    /**
     * Configurations for Dokka Generator Plugins. Must be provided from
     * [org.jetbrains.dokka.gradle.engine.DokkaPublication.pluginsConfiguration].
     */
    @get:Nested
    val pluginsConfiguration: DokkaPluginParametersContainer,
) : ExtensionAware {

//  /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
//  @get:InputFiles
//  //@get:NormalizeLineEndings
//  @get:PathSensitive(PathSensitivity.RELATIVE)
//  @get:Optional
//  abstract val dokkaSubprojectParameters: ConfigurableFileCollection

    @get:Input
    abstract val failOnWarning: Property<Boolean>

    @get:Input
    abstract val finalizeCoroutines: Property<Boolean>

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    @get:Optional
    abstract val moduleVersion: Property<String>

    @get:Input
    abstract val offlineMode: Property<Boolean>

    @get:Input
    abstract val suppressObviousFunctions: Property<Boolean>

    @get:Input
    abstract val suppressInheritedMembers: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(RELATIVE)
    abstract val includes: ConfigurableFileCollection

    /**
     * Classpath that contains the Dokka Generator Plugins used to modify this publication.
     *
     * The plugins should be configured in [org.jetbrains.dokka.gradle.engine.DokkaPublication.pluginsConfiguration].
     */
    @get:InputFiles
    @get:Classpath
    abstract val pluginsClasspath: ConfigurableFileCollection

    /**
     * Source sets used to generate a Dokka Module.
     *
     * The values are not used directly in this task, but they are required to be registered as a
     * task input for up-to-date checks
     */
    @get:Nested
    val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
        extensions.adding("dokkaSourceSets", objects.domainObjectContainer())

    /** Dokka Modules directories, containing the output, module descriptor, and module includes. */
    @get:InputFiles
    @get:PathSensitive(RELATIVE)
    abstract val moduleOutputDirectories: ConfigurableFileCollection
}
