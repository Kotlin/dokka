package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.DokkaConfiguration

/**
 * Properties that describe a Dokka Module.
 *
 * These values are passed into Dokka Generator, which will aggregate all provided Modules into a
 * single publication.
 */
@DokkatooInternalApi
abstract class DokkaModuleDescriptionSpec
@DokkatooInternalApi
@Inject constructor(
  @get:Input
  val moduleName: String,
) : Named {

  /**
   * @see DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
   */
  @get:Input
  abstract val sourceOutputDirectory: RegularFileProperty

  /**
   * @see DokkaConfiguration.DokkaModuleDescription.includes
   */
  @get:Input
  abstract val includes: ConfigurableFileCollection

  /**
   * File path of the subproject that determines where the Dokka Module will be placed within an
   * assembled Dokka Publication.
   *
   * This must be a relative path, and will be appended to the root Dokka Publication directory.
   *
   * The Gradle project path will also be accepted ([org.gradle.api.Project.getPath]), and the
   * colons `:` will be replaced with file separators `/`.
   */
  @get:Input
  abstract val projectPath: Property<String>
}
