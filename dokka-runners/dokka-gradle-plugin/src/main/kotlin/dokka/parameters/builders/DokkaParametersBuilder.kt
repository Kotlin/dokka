package dev.adamko.dokkatoo.dokka.parameters.builders

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import dev.adamko.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import dev.adamko.dokkatoo.dokka.plugins.DokkaPluginParametersBaseSpec
import dev.adamko.dokkatoo.formats.DokkatooHtmlPlugin.Companion.extractDokkaPluginMarkers
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.dokka.*

/**
 * Convert the Gradle-focused [DokkaGeneratorParametersSpec] into a [DokkaSourceSetImpl] instance,
 * which will be passed to Dokka Generator.
 *
 * The conversion is defined in a separate class to try and prevent classes from Dokka Generator
 * leaking into the public API.
 */
@DokkatooInternalApi
internal class DokkaParametersBuilder(
  private val archives: ArchiveOperations,
) {

  private val logger: Logger = Logging.getLogger(DokkaParametersBuilder::class.java)

  fun build(
    spec: DokkaGeneratorParametersSpec,
    delayTemplateSubstitution: Boolean,
    outputDirectory: File,
    cacheDirectory: File? = null,
    moduleDescriptorDirs: Iterable<File>,
  ): DokkaConfiguration {
    val moduleName = spec.moduleName.get()
    val moduleVersion = spec.moduleVersion.orNull?.takeIf { it != Project.DEFAULT_VERSION }
    val offlineMode = spec.offlineMode.get()
    val sourceSets = DokkaSourceSetBuilder.buildAll(spec.dokkaSourceSets)
    val failOnWarning = spec.failOnWarning.get()
    val suppressObviousFunctions = spec.suppressObviousFunctions.get()
    val suppressInheritedMembers = spec.suppressInheritedMembers.get()
    val finalizeCoroutines = spec.finalizeCoroutines.get()
    val pluginsConfiguration = spec.pluginsConfiguration.toSet()

    val pluginsClasspath = buildPluginsClasspath(
      plugins = spec.pluginsClasspath,
    )

    val includes = spec.includes.files

    return DokkaConfigurationImpl(
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      outputDir = outputDirectory,
      cacheRoot = cacheDirectory,
      offlineMode = offlineMode,
      sourceSets = sourceSets,
      pluginsClasspath = pluginsClasspath,
      pluginsConfiguration = pluginsConfiguration.map(::build),
      modules = buildModuleDescriptors(moduleDescriptorDirs),
      failOnWarning = failOnWarning,
      delayTemplateSubstitution = delayTemplateSubstitution,
      suppressObviousFunctions = suppressObviousFunctions,
      includes = includes,
      suppressInheritedMembers = suppressInheritedMembers,
      finalizeCoroutines = finalizeCoroutines,
    )
  }

  private fun buildPluginsClasspath(
    plugins: FileCollection,
  ): List<File> {
    // only include dependencies with Dokka Plugin markers
    return plugins
      .filter { file ->
        val pluginIds = extractDokkaPluginMarkers(archives, file)
        pluginIds.isNotEmpty()
      }
      .toList()
  }

  private fun buildModuleDescriptors(
    moduleDescriptorDirs: Iterable<File>
  ): List<DokkaModuleDescriptionImpl> {
    return moduleDescriptorDirs
      .map { moduleDir ->
        val moduleDescriptorJson = moduleDir.resolve("module-descriptor.json")
        if (!moduleDir.exists()) {
          error("missing module-descriptor.json in consolidated Dokka module $moduleDir")
        }

        val moduleDescriptor: DokkaModuleDescriptionKxs =
          DokkatooBasePlugin.jsonMapper.decodeFromString(
            DokkaModuleDescriptionKxs.serializer(),
            moduleDescriptorJson.readText(),
          )

        val moduleOutputDirectory = moduleDir.resolve(moduleDescriptor.moduleOutputDirName)
        if (!moduleOutputDirectory.exists()) {
          error("missing module output directory in consolidated Dokka module $moduleDir")
        }

        val moduleIncludes = moduleDir.resolve(moduleDescriptor.moduleIncludesDirName)
          .takeIf(File::exists)
          ?.walk()
          ?.drop(1)
          ?.toSet()
          ?: emptySet()  // 'include' files are optional


        // `relativeOutputDir` is the path where the Dokka Module should be located within the final
        // Dokka Publication.
        // Convert a project path to a relative path
        // e.g. `:x:y:z:my-cool-subproject` -> `x/y/z/my-cool-subproject`.
        // The path has to be unique per module - using the project path is a useful way to achieve this.
        val relativeOutputDir =
          File(moduleDescriptor.modulePath.removePrefix(":").replace(':', '/'))

        val md = DokkaModuleDescriptionImpl(
          name = moduleDescriptor.name,
          relativePathToOutputDirectory = relativeOutputDir,
          includes = moduleIncludes,
          sourceOutputDirectory = moduleOutputDirectory,
        )

        logger.info("[${this::class}] converted $moduleDir to $md")

        md
      }
      // Sort so the output is stable.
      // `relativePathToOutputDirectory` is better than `name` since it's guaranteed to be unique
      // across all modules (otherwise they'd be generated into the same directory), and even
      // though it's a file - it's a _relative_ file, so the ordering should be stable across
      // machines (which is important for relocatable Build Cache).
      .sortedBy { it.relativePathToOutputDirectory }
  }

  private fun build(spec: DokkaPluginParametersBaseSpec): PluginConfigurationImpl {
    return PluginConfigurationImpl(
      fqPluginName = spec.pluginFqn,
      serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
      values = spec.jsonEncode(),
    )
  }
}
