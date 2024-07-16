package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor


/**
 * Generates a Dokka Module.
 *
 * Dokka Modules are 'incomplete', and must be combined into a single Dokka Publication
 * by [DokkatooGeneratePublicationTask].
 */
@CacheableTask
abstract class DokkatooGenerateModuleTask
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  workers: WorkerExecutor,
  archives: ArchiveOperations,
  private val fs: FileSystemOperations,
  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooGenerateTask(
  objects = objects,
  workers = workers,
  pluginsConfiguration = pluginsConfiguration,
  archives = archives,
) {

  @get:Input
  abstract val modulePath: Property<String>

  @TaskAction
  internal fun generateModule() {
    val outputDirectory = outputDirectory.get().asFile
    val moduleDescriptorJson = outputDirectory.resolve("module-descriptor.json")

    // clean output dir, so previous generations don't dirty this generation
    fs.delete { delete(outputDirectory) }
    outputDirectory.mkdirs()

    // generate descriptor, will be read by other subprojects
    val moduleDescriptor = generateModuleConfiguration(moduleDescriptorJson)
    val includesOutputDir = outputDirectory.resolve(moduleDescriptor.moduleIncludesDirName)
    val moduleOutputDir = outputDirectory.resolve(moduleDescriptor.moduleOutputDirName)

    // run Dokka Generator
    generateDocumentation(GeneratorMode.Module, moduleOutputDir)

    // gather includes, to be consumed by other subprojects
    fs.sync {
      into(includesOutputDir)
      from(generator.includes)
      from(generator.dokkaSourceSets.map { it.includes })
    }
  }

  private fun generateModuleConfiguration(
    moduleDescriptorJson: File,
  ): DokkaModuleDescriptionKxs {
    val moduleName = generator.moduleName.get()
    val modulePath = modulePath.get()

    val moduleDesc = DokkaModuleDescriptionKxs(
      name = moduleName,
      modulePath = modulePath,
    )

    val encodedModuleDesc =
      DokkatooBasePlugin.jsonMapper.encodeToString(
        DokkaModuleDescriptionKxs.serializer(),
        moduleDesc
      )

    logger.info("encodedModuleDesc: $encodedModuleDesc".lines().joinToString(" "))

    moduleDescriptorJson.apply {
      parentFile.mkdirs()
      writeText(encodedModuleDesc)
    }

    return moduleDesc
  }
}
