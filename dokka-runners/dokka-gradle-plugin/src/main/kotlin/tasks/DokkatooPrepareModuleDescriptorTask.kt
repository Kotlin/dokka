/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import javax.inject.Inject

/**
 * Deprecated:
 *
 * The `module-descriptor.json` that this task produced was not compatible with relocatable
 * build-cache.
 *
 * Generation of the Module Descriptor JSON was moved into [DokkatooGenerateModuleTask].
 * This task now does nothing and should not be used.
 *
 * @see org.jetbrains.dokka.gradle.tasks.DokkatooGenerateModuleTask
 */
@Deprecated(
    "The module-descriptor.json that this task produced was not compatible with relocatable build-cache. " +
            "Module Descriptor JSON generation was moved into DokkatooGenerateModuleTask. " +
            "This task now does nothing and should not be used."
)
@UntrackedTask(because = "DokkatooPrepareModuleDescriptorTask has been deprecated and should no longer be used - see KDoc")
@Suppress("unused")
abstract class DokkatooPrepareModuleDescriptorTask
@DokkaInternalApi
@Inject
constructor() : DokkatooTask() {

    @get:Internal
    abstract val dokkaModuleDescriptorJson: RegularFileProperty

    @get:Internal
    abstract val moduleName: Property<String>

    @get:Internal
    abstract val modulePath: Property<String>

    @get:Internal
    abstract val moduleDirectory: DirectoryProperty

    @get:Internal
    abstract val includes: ConfigurableFileCollection

    @TaskAction
    internal fun generateModuleConfiguration() {
        logger.warn("$path has been deprecated and should no longer be used.")
    }
}
