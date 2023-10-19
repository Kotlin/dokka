package org.jetbrains.dokka.dokkatoo.formats

import org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin
import org.jetbrains.dokka.dokkatoo.DokkatooExtension
import org.jetbrains.dokka.dokkatoo.adapters.DokkatooAndroidAdapter
import org.jetbrains.dokka.dokkatoo.adapters.DokkatooJavaAdapter
import org.jetbrains.dokka.dokkatoo.adapters.DokkatooKotlinAdapter
import org.jetbrains.dokka.dokkatoo.internal.*
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*

/**
 * Base Gradle Plugin for setting up a Dokka Publication for a specific format.
 *
 * [DokkatooBasePlugin] must be applied for this plugin (or any subclass) to have an effect.
 *
 * Anyone can use this class as a basis for a generating a Dokka Publication in a custom format.
 */
abstract class DokkatooFormatPlugin(
  val formatName: String,
) : Plugin<Project> {

  @get:Inject
  @DokkatooInternalApi
  protected abstract val objects: ObjectFactory
  @get:Inject
  @DokkatooInternalApi
  protected abstract val providers: ProviderFactory
  @get:Inject
  @DokkatooInternalApi
  protected abstract val files: FileSystemOperations


  override fun apply(target: Project) {

    // apply DokkatooBasePlugin
    target.pluginManager.apply(DokkatooBasePlugin::class)

    // apply the plugin that will autoconfigure Dokkatoo to use the sources of a Kotlin project
    target.pluginManager.apply(type = DokkatooKotlinAdapter::class)
    target.pluginManager.apply(type = DokkatooJavaAdapter::class)
    target.pluginManager.apply(type = DokkatooAndroidAdapter::class)

    target.plugins.withType<DokkatooBasePlugin>().configureEach {
      val dokkatooExtension = target.extensions.getByType(DokkatooExtension::class)

      val publication = dokkatooExtension.dokkatooPublications.create(formatName)

      val dokkatooConsumer =
        target.configurations.named(DokkatooBasePlugin.dependencyContainerNames.dokkatoo)

      val dependencyContainers = DokkatooFormatDependencyContainers(
        formatName = formatName,
        dokkatooConsumer = dokkatooConsumer,
        project = target,
      )

      val dokkatooTasks = DokkatooFormatTasks(
        project = target,
        publication = publication,
        dokkatooExtension = dokkatooExtension,
        dependencyContainers = dependencyContainers,
        providers = providers,
      )

      dependencyContainers.dokkaModuleOutgoing.configure {
        outgoing {
          artifact(dokkatooTasks.prepareModuleDescriptor.flatMap { it.dokkaModuleDescriptorJson })
        }
        outgoing {
          artifact(dokkatooTasks.generateModule.flatMap { it.outputDirectory }) {
            type = "directory"
          }
        }
      }

      // TODO DokkaCollect replacement - share raw files without first generating a Dokka Module
      //dependencyCollections.dokkaParametersOutgoing.configure {
      //  outgoing {
      //    artifact(dokkatooTasks.prepareParametersTask.flatMap { it.dokkaConfigurationJson })
      //  }
      //}

      val context = DokkatooFormatPluginContext(
        project = target,
        dokkatooExtension = dokkatooExtension,
        dokkatooTasks = dokkatooTasks,
        formatName = formatName,
      )

      context.configure()

      if (context.addDefaultDokkaDependencies) {
        with(context) {
          addDefaultDokkaDependencies()
        }
      }
    }
  }


  /** Format specific configuration - to be implemented by subclasses */
  open fun DokkatooFormatPluginContext.configure() {}


  @DokkatooInternalApi
  class DokkatooFormatPluginContext(
    val project: Project,
    val dokkatooExtension: DokkatooExtension,
    val dokkatooTasks: DokkatooFormatTasks,
    formatName: String,
  ) {
    private val dependencyContainerNames = DokkatooBasePlugin.DependencyContainerNames(formatName)

    var addDefaultDokkaDependencies = true

    /** Create a [Dependency] for a Dokka module */
    fun DependencyHandler.dokka(module: String): Provider<Dependency> =
      dokkatooExtension.versions.jetbrainsDokka.map { version -> create("org.jetbrains.dokka:$module:$version") }

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>): Unit =
      addProvider(dependencyContainerNames.dokkaPluginsClasspath, dependency)

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: String) {
      add(dependencyContainerNames.dokkaPluginsClasspath, dependency)
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) {
      addProvider(dependencyContainerNames.dokkaGeneratorClasspath, dependency)
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: String) {
      add(dependencyContainerNames.dokkaGeneratorClasspath, dependency)
    }
  }


  private fun DokkatooFormatPluginContext.addDefaultDokkaDependencies() {
    project.dependencies {
      /** lazily create a [Dependency] with the provided [version] */
      infix fun String.version(version: Property<String>): Provider<Dependency> =
        version.map { v -> create("$this:$v") }

      with(dokkatooExtension.versions) {
        dokkaPlugin(dokka("analysis-kotlin-descriptors"))
        dokkaPlugin(dokka("templating-plugin"))
        dokkaPlugin(dokka("dokka-base"))
//        dokkaPlugin(dokka("all-modules-page-plugin"))

        dokkaPlugin("org.jetbrains.kotlinx:kotlinx-html" version kotlinxHtml)
        dokkaPlugin("org.freemarker:freemarker" version freemarker)

        dokkaGenerator(dokka("dokka-core"))
        // TODO why does org.jetbrains:markdown need a -jvm suffix?
        dokkaGenerator("org.jetbrains:markdown-jvm" version jetbrainsMarkdown)
        dokkaGenerator("org.jetbrains.kotlinx:kotlinx-coroutines-core" version kotlinxCoroutines)
      }
    }
  }

}
