package dev.adamko.dokkatoo.formats

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.DokkatooExtension
import dev.adamko.dokkatoo.adapters.DokkatooAndroidAdapter
import dev.adamko.dokkatoo.adapters.DokkatooJavaAdapter
import dev.adamko.dokkatoo.adapters.DokkatooKotlinAdapter
import dev.adamko.dokkatoo.dependencies.DependencyContainerNames
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooClasspathAttribute
import dev.adamko.dokkatoo.dependencies.DokkatooAttribute.Companion.DokkatooFormatAttribute
import dev.adamko.dokkatoo.dependencies.FormatDependenciesManager
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logging
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
  @get:Inject
  @DokkatooInternalApi
  protected abstract val layout: ProjectLayout


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

      val formatDependencies = FormatDependenciesManager(
        project = target,
        baseDependencyManager = dokkatooExtension.baseDependencyManager,
        formatName = formatName,
        objects = objects,
      )

      val dokkatooTasks = DokkatooFormatTasks(
        project = target,
        publication = publication,
        dokkatooExtension = dokkatooExtension,
        formatDependencies = formatDependencies,
        providers = providers,
      )

      formatDependencies.moduleOutputDirectories
        .outgoing
        .get()
        .outgoing
        .artifact(dokkatooTasks.generateModule.map { it.outputDirectory }) {
          builtBy(dokkatooTasks.generateModule)
          type = "dokka-module-directory"
        }

      dokkatooTasks.generatePublication.configure {
        generator.moduleOutputDirectories.from(
          formatDependencies.moduleOutputDirectories.incomingArtifactFiles
        )
        generator.pluginsClasspath.from(
          formatDependencies.dokkaPublicationPluginClasspathResolver
        )
      }

      val context = DokkatooFormatPluginContext(
        project = target,
        dokkatooExtension = dokkatooExtension,
        dokkatooTasks = dokkatooTasks,
        formatDependencies = formatDependencies,
        formatName = formatName,
      )

      context.configure()

      if (context.addDefaultDokkaDependencies) {
        with(context) {
          addDefaultDokkaDependencies()
        }
      }

      if (context.enableVersionAlignment) {
        //region version alignment
        listOf(
          formatDependencies.dokkaPluginsIntransitiveClasspathResolver,
          formatDependencies.dokkaGeneratorClasspathResolver,
        ).forEach { dependenciesContainer: NamedDomainObjectProvider<Configuration> ->
          // Add a version if one is missing, which will allow defining a org.jetbrains.dokka
          // dependency without a version.
          // (It would be nice to do this with a virtual-platform, but Gradle is bugged:
          // https://github.com/gradle/gradle/issues/27435)
          dependenciesContainer.configure {
            resolutionStrategy.eachDependency {
              if (requested.group == "org.jetbrains.dokka" && requested.version.isNullOrBlank()) {
                logger.info("adding version of dokka dependency '$requested'")
                useVersion(dokkatooExtension.versions.jetbrainsDokka.get())
              }
            }
          }
        }
        //endregion
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
    val formatDependencies: FormatDependenciesManager,
    formatName: String,
  ) {
    private val dependencyContainerNames: DependencyContainerNames =
      DependencyContainerNames(formatName)

    var addDefaultDokkaDependencies: Boolean = true
    var enableVersionAlignment: Boolean = true

    /** Create a [Dependency] for a Dokka module */
    fun DependencyHandler.dokka(module: String): Provider<Dependency> =
      dokkatooExtension.versions.jetbrainsDokka.map { version -> create("org.jetbrains.dokka:$module:$version") }

    private fun AttributeContainer.dokkaPluginsClasspath() {
      attribute(DokkatooFormatAttribute, formatDependencies.formatAttributes.format.name)
      attribute(DokkatooClasspathAttribute, formatDependencies.baseAttributes.dokkaPlugins.name)
    }

    private fun AttributeContainer.dokkaGeneratorClasspath() {
      attribute(DokkatooFormatAttribute, formatDependencies.formatAttributes.format.name)
      attribute(DokkatooClasspathAttribute, formatDependencies.baseAttributes.dokkaGenerator.name)
    }

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>): Unit =
      addProvider(
        dependencyContainerNames.pluginsClasspath,
        dependency,
        Action<ExternalModuleDependency> {
          attributes { dokkaPluginsClasspath() }
        })

    /** Add a dependency to the Dokka plugins classpath */
    fun DependencyHandler.dokkaPlugin(dependency: String) {
      add(dependencyContainerNames.pluginsClasspath, dependency) {
        attributes { dokkaPluginsClasspath() }
      }
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) {
      addProvider(dependencyContainerNames.generatorClasspath, dependency,
        Action<ExternalModuleDependency> {
          attributes { dokkaGeneratorClasspath() }
        })
    }

    /** Add a dependency to the Dokka Generator classpath */
    fun DependencyHandler.dokkaGenerator(dependency: String) {
      add(dependencyContainerNames.generatorClasspath, dependency) {
        attributes { dokkaGeneratorClasspath() }
      }
    }
  }


  private fun DokkatooFormatPluginContext.addDefaultDokkaDependencies() {
    project.dependencies {
      /** lazily create a [Dependency] with the provided [version] */
      infix fun String.version(version: Property<String>): Provider<Dependency> =
        version.map { v -> create("$this:$v") }

      with(dokkatooExtension.versions) {
        dokkaPlugin(dokka("templating-plugin"))
        dokkaPlugin(dokka("dokka-base"))

        dokkaGenerator(dokka("analysis-kotlin-descriptors"))
        dokkaGenerator(dokka("dokka-core"))
        dokkaGenerator("org.freemarker:freemarker" version freemarker)
        dokkaGenerator("org.jetbrains:markdown" version jetbrainsMarkdown)
        dokkaGenerator("org.jetbrains.kotlinx:kotlinx-coroutines-core" version kotlinxCoroutines)
        dokkaGenerator("org.jetbrains.kotlinx:kotlinx-html" version kotlinxHtml)
      }
    }
  }

  companion object {
    private val logger = Logging.getLogger(DokkatooFormatPlugin::class.java)
  }
}
