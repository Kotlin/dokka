package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.dependencies.FormatDependenciesManager
import dev.adamko.dokkatoo.dokka.DokkaPublication
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.configuring
import dev.adamko.dokkatoo.tasks.DokkatooGenerateModuleTask
import dev.adamko.dokkatoo.tasks.DokkatooGeneratePublicationTask
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import dev.adamko.dokkatoo.tasks.TaskNames
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

/** Tasks for generating a Dokkatoo Publication in a specific format. */
@DokkatooInternalApi
class DokkatooFormatTasks(
  project: Project,
  private val publication: DokkaPublication,
  private val dokkatooExtension: DokkatooExtension,
  private val formatDependencies: FormatDependenciesManager,

  private val providers: ProviderFactory,
) {
  private val formatName: String get() = publication.formatName

  private val taskNames = TaskNames(formatName)

  private fun DokkatooGenerateTask.applyFormatSpecificConfiguration() {
    runtimeClasspath.from(
      formatDependencies.dokkaGeneratorClasspathResolver
    )
    generator.apply {
      publicationEnabled.convention(publication.enabled)

      failOnWarning.convention(publication.failOnWarning)
      finalizeCoroutines.convention(publication.finalizeCoroutines)
      includes.from(publication.includes)
      moduleName.convention(publication.moduleName)
      moduleVersion.convention(publication.moduleVersion)
      offlineMode.convention(publication.offlineMode)
      pluginsConfiguration.addAllLater(providers.provider { publication.pluginsConfiguration })
      pluginsClasspath.from(
        formatDependencies.dokkaPluginsIntransitiveClasspathResolver
      )
      suppressInheritedMembers.convention(publication.suppressInheritedMembers)
      suppressObviousFunctions.convention(publication.suppressObviousFunctions)
    }
  }

  val generatePublication: TaskProvider<DokkatooGeneratePublicationTask> =
    project.tasks.register<DokkatooGeneratePublicationTask>(
      taskNames.generatePublication,
      publication.pluginsConfiguration,
    ).configuring {
      description = "Executes the Dokka Generator, generating the $formatName publication"

      outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))

      applyFormatSpecificConfiguration()
    }

  val generateModule: TaskProvider<DokkatooGenerateModuleTask> =
    project.tasks.register<DokkatooGenerateModuleTask>(
      taskNames.generateModule,
      publication.pluginsConfiguration,
    ).configuring {
      description = "Executes the Dokka Generator, generating a $formatName module"

      outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))

      applyFormatSpecificConfiguration()
    }

  @Suppress("DEPRECATION", "unused")
  @Deprecated("DokkatooPrepareModuleDescriptorTask was not compatible with relocatable Gradle Build Cache and has been replaced with a dark Gradle devilry. All references to DokkatooPrepareModuleDescriptorTask must be removed.")
  val prepareModuleDescriptor: TaskProvider<dev.adamko.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask> =
    project.tasks.register<dev.adamko.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask>(
      taskNames.prepareModuleDescriptor
    ) {
      description = "[Deprecated ⚠️] Prepares the Dokka Module Descriptor for $formatName"
    }
}
