package org.jetbrains.dokka.dokkatoo.formats

import org.jetbrains.dokka.dokkatoo.DokkatooBasePlugin
import org.jetbrains.dokka.dokkatoo.distributions.DokkatooConfigurationAttributes
import org.jetbrains.dokka.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_BASE_ATTRIBUTE
import org.jetbrains.dokka.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKATOO_CATEGORY_ATTRIBUTE
import org.jetbrains.dokka.dokkatoo.distributions.DokkatooConfigurationAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.asConsumer
import org.jetbrains.dokka.dokkatoo.internal.asProvider
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.*

/**
 * The Dokka-specific Gradle [Configuration]s used to produce and consume files from external sources
 * (example: Maven Central), or between subprojects.
 *
 * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
 * [DokkaConfiguration][org.jetbrains.dokka.DokkaConfiguration]
 * is used to configure Dokka behaviour.)
 */
@DokkatooInternalApi
class DokkatooFormatDependencyContainers(
  private val formatName: String,
  dokkatooConsumer: NamedDomainObjectProvider<Configuration>,
  project: Project,
) {

  private val objects: ObjectFactory = project.objects

  private val dependencyContainerNames = DokkatooBasePlugin.DependencyContainerNames(formatName)

  private val dokkatooAttributes: DokkatooConfigurationAttributes = objects.newInstance()

  private fun AttributeContainer.dokkaCategory(category: DokkatooConfigurationAttributes.DokkatooCategoryAttribute) {
    attribute(DOKKATOO_BASE_ATTRIBUTE, dokkatooAttributes.dokkatooBaseUsage)
    attribute(DOKKA_FORMAT_ATTRIBUTE, objects.named(formatName))
    attribute(DOKKATOO_CATEGORY_ATTRIBUTE, category)
  }

  private fun AttributeContainer.jvmJar() {
    attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
    attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
    attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
  }

  //<editor-fold desc="Dokka Module files">
  /** Fetch Dokka Module files from other subprojects */
  val dokkaModuleConsumer: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(dependencyContainerNames.dokkatooModuleFilesConsumer) {
      description = "Fetch Dokka Module files for $formatName from other subprojects"
      asConsumer()
      extendsFrom(dokkatooConsumer.get())
      attributes {
        dokkaCategory(dokkatooAttributes.dokkaModuleFiles)
      }
    }
  /** Provide Dokka Module files to other subprojects */
  val dokkaModuleOutgoing: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(dependencyContainerNames.dokkatooModuleFilesProvider) {
      description = "Provide Dokka Module files for $formatName to other subprojects"
      asProvider()
      // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
      extendsFrom(dokkaModuleConsumer.get())
      attributes {
        dokkaCategory(dokkatooAttributes.dokkaModuleFiles)
      }
    }
  //</editor-fold>

  //<editor-fold desc="Dokka Generator Plugins">
  /**
   * Dokka plugins.
   *
   * Users can add plugins to this dependency.
   *
   * Should not contain runtime dependencies.
   */
  val dokkaPluginsClasspath: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(dependencyContainerNames.dokkaPluginsClasspath) {
      description = "Dokka Plugins classpath for $formatName"
      asConsumer()
      attributes {
        jvmJar()
        dokkaCategory(dokkatooAttributes.dokkaPluginsClasspath)
      }
    }

  /**
   * Dokka Plugins, without transitive dependencies.
   *
   * It extends [dokkaPluginsClasspath], so do not add dependencies to this configuration -
   * the dependencies are computed automatically.
   */
  val dokkaPluginsIntransitiveClasspath: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(dependencyContainerNames.dokkaPluginsIntransitiveClasspath) {
      description =
        "Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
      asConsumer()
      extendsFrom(dokkaPluginsClasspath.get())
      isTransitive = false
      attributes {
        jvmJar()
        dokkaCategory(dokkatooAttributes.dokkaPluginsClasspath)
      }
    }
  //</editor-fold>

  //<editor-fold desc="Dokka Generator Classpath">
  /**
   * Runtime classpath used to execute Dokka Worker.
   *
   * This configuration is not exposed to other subprojects.
   *
   * Extends [dokkaPluginsClasspath].
   *
   * @see org.jetbrains.dokka.dokkatoo.workers.DokkaGeneratorWorker
   * @see org.jetbrains.dokka.dokkatoo.tasks.DokkatooGenerateTask
   */
  val dokkaGeneratorClasspath: NamedDomainObjectProvider<Configuration> =
    project.configurations.register(dependencyContainerNames.dokkaGeneratorClasspath) {
      description =
        "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
      asConsumer()

      // extend from plugins classpath, so Dokka Worker can run the plugins
      extendsFrom(dokkaPluginsClasspath.get())

      isTransitive = true
      attributes {
        jvmJar()
        dokkaCategory(dokkatooAttributes.dokkaGeneratorClasspath)
      }
    }
  //</editor-fold>
}
