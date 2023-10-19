package org.jetbrains.dokka.dokkatoo.formats

import org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin
import org.jetbrains.dokka.dokkatoo.DokkatooExtension
import org.jetbrains.dokka.dokkatoo.dokka.DokkaPublication
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.LocalProjectOnlyFilter
import org.jetbrains.dokka.dokkatoo.internal.configuring
import org.jetbrains.dokka.dokkatoo.tasks.DokkatooGenerateTask
import org.jetbrains.dokka.dokkatoo.tasks.DokkatooPrepareModuleDescriptorTask
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*

/** Tasks for generating a Dokkatoo Publication in a specific format. */
@DokkatooInternalApi
class DokkatooFormatTasks(
  project: Project,
  private val publication: DokkaPublication,
  private val dokkatooExtension: DokkatooExtension,
  private val dependencyContainers: DokkatooFormatDependencyContainers,

  private val providers: ProviderFactory,
) {
  private val formatName: String get() = publication.formatName

  private val taskNames = DokkatooBasePlugin.TaskNames(formatName)

  private fun DokkatooGenerateTask.applyFormatSpecificConfiguration() {
    runtimeClasspath.from(
      dependencyContainers.dokkaGeneratorClasspath.map { classpath ->
        classpath.incoming.artifacts.artifactFiles
      }
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
        dependencyContainers.dokkaPluginsIntransitiveClasspath.map { classpath ->
          classpath.incoming.artifacts.artifactFiles
        }
      )
      suppressInheritedMembers.convention(publication.suppressInheritedMembers)
      suppressObviousFunctions.convention(publication.suppressObviousFunctions)
    }
  }

  val generatePublication = project.tasks.register<DokkatooGenerateTask>(
    taskNames.generatePublication,
    publication.pluginsConfiguration,
  ).configuring task@{
    description = "Executes the Dokka Generator, generating the $formatName publication"
    generationType.set(DokkatooGenerateTask.GenerationType.PUBLICATION)

    outputDirectory.convention(dokkatooExtension.dokkatooPublicationDirectory.dir(formatName))

    generator.apply {
      // depend on Dokka Module Descriptors from other subprojects
      dokkaModuleFiles.from(
        dependencyContainers.dokkaModuleConsumer.map { modules ->
          modules.incoming
            .artifactView { componentFilter(LocalProjectOnlyFilter) }
            .artifacts.artifactFiles
        }
      )
    }

    applyFormatSpecificConfiguration()
  }

  val generateModule = project.tasks.register<DokkatooGenerateTask>(
    taskNames.generateModule,
    publication.pluginsConfiguration,
  ).configuring task@{
    description = "Executes the Dokka Generator, generating a $formatName module"
    generationType.set(DokkatooGenerateTask.GenerationType.MODULE)

    outputDirectory.convention(dokkatooExtension.dokkatooModuleDirectory.dir(formatName))

    applyFormatSpecificConfiguration()
  }

  val prepareModuleDescriptor = project.tasks.register<DokkatooPrepareModuleDescriptorTask>(
    taskNames.prepareModuleDescriptor
  ) task@{
    description = "Prepares the Dokka Module Descriptor for $formatName"
    includes.from(publication.includes)
    dokkaModuleDescriptorJson.convention(
      dokkatooExtension.dokkatooConfigurationsDirectory.file("$formatName/module_descriptor.json")
    )
    moduleDirectory.set(generateModule.flatMap { it.outputDirectory })

//      dokkaSourceSets.addAllLater(providers.provider { dokkatooExtension.dokkatooSourceSets })
//      dokkaSourceSets.configureEach {
//        sourceSetScope.convention(this@task.path)
//      }
  }
}
