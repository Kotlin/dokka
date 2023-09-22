package org.jetbrains.dokka.dokkatoo

import org.jetbrains.dokka.dokkatoo.dokka.DokkaPublication
import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.dokkatoo.internal.*
import java.io.Serializable
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*

/**
 * Configure the behaviour of the [DokkatooBasePlugin].
 */
abstract class DokkatooExtension
@DokkatooInternalApi
constructor(
  objects: ObjectFactory,
) : ExtensionAware, Serializable {

  /** Directory into which [DokkaPublication]s will be produced */
  abstract val dokkatooPublicationDirectory: DirectoryProperty

  /** Directory into which Dokka Modules will be produced */
  abstract val dokkatooModuleDirectory: DirectoryProperty

  abstract val dokkatooConfigurationsDirectory: DirectoryProperty

  /** Default Dokkatoo cache directory */
  abstract val dokkatooCacheDirectory: DirectoryProperty

  abstract val moduleName: Property<String>
  abstract val moduleVersion: Property<String>
  abstract val modulePath: Property<String>

  /**
   * An arbitrary string used to group source sets that originate from different Gradle subprojects.
   *
   * This is primarily used by Kotlin Multiplatform projects, which can have multiple source sets
   * per subproject.
   *
   * Defaults to [the path of the subproject][org.gradle.api.Project.getPath].
   */
  abstract val sourceSetScopeDefault: Property<String>

  /**
   * The Konan home directory, which contains libraries for Kotlin/Native development.
   *
   * This is only required as a workaround to fetch the compile-time dependencies in Kotlin/Native
   * projects with a version below 2.0.
   */
  // This property should be removed when Dokkatoo only supports KGP 2 or higher.
  @DokkatooInternalApi
  abstract val konanHome: RegularFileProperty

  /**
   * Configuration for creating Dokka Publications.
   *
   * Each publication will generate one Dokka site based on the included Dokka Source Sets.
   *
   * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
   */
  val dokkatooPublications: NamedDomainObjectContainer<DokkaPublication> =
    extensions.adding(
      "dokkatooPublications",
      objects.domainObjectContainer { named -> objects.newInstance(named, pluginsConfiguration) }
    )

  /**
   * Dokka Source Sets describe the source code that should be included in a Dokka Publication.
   *
   * Dokka will not generate documentation unless there is at least there is at least one Dokka Source Set.
   *
   *  TODO make sure dokkatooSourceSets doc is up to date...
   *
   * Only source sets that are contained within _this project_ should be included here.
   * To merge source sets from other projects, use the Gradle dependencies block.
   *
   * ```kotlin
   * dependencies {
   *   // merge :other-project into this project's Dokka Configuration
   *   dokka(project(":other-project"))
   * }
   * ```
   *
   * Or, to include other Dokka Publications as a Dokka Module use
   *
   * ```kotlin
   * dependencies {
   *   // include :other-project as a module in this project's Dokka Configuration
   *   dokkaModule(project(":other-project"))
   * }
   * ```
   *
   * Dokka will merge Dokka Source Sets from other subprojects if...
   */
  val dokkatooSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
    extensions.adding("dokkatooSourceSets", objects.domainObjectContainer())

  /**
   * Dokka Plugin are used to configure the way Dokka generates a format.
   * Some plugins can be configured via parameters, and those parameters are stored in this
   * container.
   */
  val pluginsConfiguration: DokkaPluginParametersContainer =
    extensions.adding("pluginsConfiguration", objects.dokkaPluginParametersContainer())

  /**
   * Versions of dependencies that Dokkatoo will use to run Dokka Generator.
   *
   * These versions can be set to change the versions of dependencies that Dokkatoo uses defaults,
   * or can be read to align versions.
   */
  val versions: Versions = extensions.adding("versions", objects.newInstance())

  interface Versions : ExtensionAware {

    /** Default version used for Dokka dependencies */
    val jetbrainsDokka: Property<String>
    val jetbrainsMarkdown: Property<String>
    val freemarker: Property<String>
    val kotlinxHtml: Property<String>
    val kotlinxCoroutines: Property<String>

    companion object
  }
}
