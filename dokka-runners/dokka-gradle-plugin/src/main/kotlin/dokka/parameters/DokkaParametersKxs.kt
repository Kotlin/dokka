package dev.adamko.dokkatoo.dokka.parameters

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import kotlinx.serialization.Serializable
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.DokkaConfiguration

/**
 * Any subproject can be merged into a single Dokka Publication. To do this, first it must create
 * a Dokka Module. A [DokkaModuleDescriptionKxs] describes a config file for the Dokka Module that
 * describes its content. This config file will be used by any aggregating project to produce
 * a Dokka Publication with multiple modules.
 *
 * Note: this class implements [java.io.Serializable] because it is used as a
 * [Gradle Property][org.gradle.api.provider.Property], and Gradle must be able to fingerprint
 * property values classes using Java Serialization.
 *
 * All other configuration data classes also implement [java.io.Serializable] via their parent interfaces.
 */
@Serializable
@DokkatooInternalApi
data class DokkaModuleDescriptionKxs(
  /** @see DokkaConfiguration.DokkaModuleDescription.name */
  val name: String,
  /** @see [org.gradle.api.Project.getPath] */
  val modulePath: String,
  /** name of the sibling directory that contains the module output */
  val moduleOutputDirName: String = "module",
  /** name of the sibling directory that contains the module includes */
  val moduleIncludesDirName: String = "includes",
)
