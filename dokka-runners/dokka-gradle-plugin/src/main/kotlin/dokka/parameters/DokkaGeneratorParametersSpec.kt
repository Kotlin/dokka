package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkaPluginParametersContainer
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.adding
import dev.adamko.dokkatoo.internal.domainObjectContainer
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE

/**
 * Parameters used to run Dokka Generator to produce either a
 * Dokka Publication or a Dokka Module.
 */
abstract class DokkaGeneratorParametersSpec
@DokkatooInternalApi
@Inject
constructor(
  objects: ObjectFactory,
  /**
   * Configurations for Dokka Generator Plugins. Must be provided from
   * [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
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
   * The plugins should be configured in [dev.adamko.dokkatoo.dokka.DokkaPublication.pluginsConfiguration].
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
  @get:Internal
  @Deprecated("DokkatooPrepareModuleDescriptorTask was not compatible with relocatable Gradle Build Cache and has been replaced with a dark Gradle devilry. All references to DokkatooPrepareModuleDescriptorTask must be removed.")
  @Suppress("unused")
  abstract val dokkaModuleFiles: ConfigurableFileCollection

  /** Dokka Modules directories, containing the output, module descriptor, and module includes. */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  abstract val moduleOutputDirectories: ConfigurableFileCollection
}
