package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.dokkatoo.internal.DokkaPluginParametersContainer
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.adding
import org.jetbrains.dokka.dokkatoo.internal.domainObjectContainer
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.work.NormalizeLineEndings

/**
 * Parameters used to run Dokka Generator to produce either a Publication or a Module.
 *
 *
 */
abstract class DokkaGeneratorParametersSpec
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [org.jetbrains.dokka.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  @get:Nested
  val pluginsConfiguration: DokkaPluginParametersContainer,
) : ExtensionAware {

//  /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
//  @get:InputFiles
//  //@get:NormalizeLineEndings
//  @get:PathSensitive(PathSensitivity.RELATIVE)
//  @get:Optional
//  abstract val dokkaSubprojectParameters: ConfigurableFileCollection

  @get:Input
  abstract val failOnWarning: Property<Boolean>

  @get:Input
  abstract val finalizeCoroutines: Property<Boolean>

  @get:Input
  abstract val moduleName: Property<String>

  @get:Input
  @get:Optional
  abstract val moduleVersion: Property<String>

  @get:Input
  abstract val offlineMode: Property<Boolean>

  @get:Input
  abstract val suppressObviousFunctions: Property<Boolean>

  @get:Input
  abstract val suppressInheritedMembers: Property<Boolean>

  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val includes: ConfigurableFileCollection

  /**
   * Classpath that contains the Dokka Generator Plugins used to modify this publication.
   *
   * The plugins should be configured in [org.jetbrains.dokka.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
   */
  @get:InputFiles
  @get:Classpath
  abstract val pluginsClasspath: ConfigurableFileCollection

  /**
   * Source sets used to generate a Dokka Module.
   *
   * The values are not used directly in this task, but they are required to be registered as a
   * task input for up-to-date checks
   */
  @get:Nested
  val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
    extensions.adding("dokkaSourceSets", objects.domainObjectContainer())

  /** Dokka Module files from other subprojects. */
  @get:InputFiles
  @get:NormalizeLineEndings
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val dokkaModuleFiles: ConfigurableFileCollection
}
