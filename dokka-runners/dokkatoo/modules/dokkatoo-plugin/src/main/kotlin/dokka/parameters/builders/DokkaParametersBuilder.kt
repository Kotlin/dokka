package org.jetbrains.dokka.dokkatoo.dokka.parameters.builders

import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaGeneratorParametersSpec
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaModuleDescriptionKxs
import org.jetbrains.dokka.dokkatoo.dokka.plugins.DokkaPluginParametersBaseSpec
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.mapNotNullToSet
import java.io.File
import org.gradle.api.logging.Logging
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.PluginConfigurationImpl

/**
 * Convert the Gradle-focused [DokkaGeneratorParametersSpec] into a [DokkaSourceSetImpl] instance,
 * which will be passed to Dokka Generator.
 *
 * The conversion is defined in a separate class to try and prevent classes from Dokka Generator
 * leaking into the public API.
 */
@DokkatooInternalApi
internal object DokkaParametersBuilder {

  fun build(
    spec: DokkaGeneratorParametersSpec,
    delayTemplateSubstitution: Boolean,
    modules: List<DokkaModuleDescriptionKxs>,
    outputDirectory: File,
    cacheDirectory: File? = null,
  ): DokkaConfiguration {
    val moduleName = spec.moduleName.get()
    val moduleVersion = spec.moduleVersion.orNull?.takeIf { it != "unspecified" }
    val offlineMode = spec.offlineMode.get()
    val sourceSets = DokkaSourceSetBuilder.buildAll(spec.dokkaSourceSets)
    val failOnWarning = spec.failOnWarning.get()
    val suppressObviousFunctions = spec.suppressObviousFunctions.get()
    val suppressInheritedMembers = spec.suppressInheritedMembers.get()
    val finalizeCoroutines = spec.finalizeCoroutines.get()
    val pluginsConfiguration = spec.pluginsConfiguration.toSet()

    val pluginsClasspath = spec.pluginsClasspath.files.toList()
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
      modules = modules.map(DokkaModuleDescriptionKxs::convert),
//      modules = modules.map {
//        it.convert(
//          moduleDescriptionFiles.get(it.name)
//            ?: error("missing module description files for ${it.name}")
//        )
//      },
      failOnWarning = failOnWarning,
      delayTemplateSubstitution = delayTemplateSubstitution,
      suppressObviousFunctions = suppressObviousFunctions,
      includes = includes,
      suppressInheritedMembers = suppressInheritedMembers,
      finalizeCoroutines = finalizeCoroutines,
    )
  }

  private fun build(spec: DokkaPluginParametersBaseSpec): PluginConfigurationImpl {
    return PluginConfigurationImpl(
      fqPluginName = spec.pluginFqn,
      serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
      values = spec.jsonEncode(),
    )
  }
}
