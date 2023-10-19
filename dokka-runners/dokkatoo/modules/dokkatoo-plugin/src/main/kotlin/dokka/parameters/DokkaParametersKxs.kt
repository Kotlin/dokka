@file:UseSerializers(
  FileAsPathStringSerializer::class,
)

package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import java.nio.file.Paths
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaModuleDescriptionImpl


// Implementations of DokkaConfiguration interfaces that can be serialized to files.
// Serialization is required because Gradle tasks can only pass data to one-another via files.


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
  /**
   * Location of the Dokka Module directory for a subproject.
   *
   * @see DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory
   */
  val sourceOutputDirectory: File,
  /** @see DokkaConfiguration.DokkaModuleDescription.includes */
  val includes: Set<File>,
  /** @see [org.gradle.api.Project.getPath] */
  val modulePath: String,
) {
  internal fun convert() =
    DokkaModuleDescriptionImpl(
      name = name,
      relativePathToOutputDirectory = File(modulePath.removePrefix(":").replace(':', '/')),
      includes = includes,
      sourceOutputDirectory = sourceOutputDirectory,
    )
}


/**
 * Serialize a [File] as an absolute, canonical file path, with
 * [invariant path separators][invariantSeparatorsPath]
 */
private object FileAsPathStringSerializer : KSerializer<File> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("java.io.File", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): File =
    Paths.get(decoder.decodeString()).toFile()

  override fun serialize(encoder: Encoder, value: File): Unit =
    encoder.encodeString(value.invariantSeparatorsPath)
}
