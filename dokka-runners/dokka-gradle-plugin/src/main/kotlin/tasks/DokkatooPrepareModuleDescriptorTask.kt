package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/**
 * Deprecated:
 *
 * The `module-descriptor.json` that this task produced was not compatible with relocatable
 * build-cache.
 *
 * Generation of the Module Descriptor JSON was moved into [DokkatooGenerateModuleTask].
 * This task now does nothing and should not be used.
 *
 * @see dev.adamko.dokkatoo.tasks.DokkatooGenerateModuleTask
 */
@Deprecated(
  "The module-descriptor.json that this task produced was not compatible with relocatable build-cache. " +
      "Module Descriptor JSON generation was moved into DokkatooGenerateModuleTask. " +
      "This task now does nothing and should not be used."
)
@UntrackedTask(because = "DokkatooPrepareModuleDescriptorTask has been deprecated and should no longer be used - see KDoc")
@Suppress("unused")
abstract class DokkatooPrepareModuleDescriptorTask
@DokkatooInternalApi
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
