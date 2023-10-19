package org.jetbrains.dokka.dokkatoo.dokka.plugins

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.addAll
import org.jetbrains.dokka.dokkatoo.internal.addAllIfNotNull
import org.jetbrains.dokka.dokkatoo.internal.putIfNotNull
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE


/**
 * Configuration for
 * [Dokka's Versioning plugin](https://github.com/Kotlin/dokka/tree/master/plugins/versioning#readme).
 *
 * The versioning plugin provides the ability to host documentation for multiple versions of your
 * library/application with seamless switching between them. This, in turn, provides a better
 * experience for your users.
 *
 * Note: The versioning plugin only works with Dokka's HTML format.
 */
abstract class DokkaVersioningPluginParameters
@DokkatooInternalApi
@Inject
constructor(
  name: String,
) : DokkaPluginParametersBaseSpec(
  name,
  DOKKA_VERSIONING_PLUGIN_FQN,
) {

  /**
   * The version of your application/library that documentation is going to be generated for.
   * This will be the version shown in the dropdown menu.
   */
  @get:Input
  @get:Optional
  abstract val version: Property<String>

  /**
   * An optional list of strings that represents the order that versions should appear in the
   * dropdown menu.
   *
   * Must match [version] string exactly. The first item in the list is at the top of the dropdown.
   */
  @get:Input
  @get:Optional
  abstract val versionsOrdering: ListProperty<String>

  /**
   * An optional path to a parent folder that contains other documentation versions.
   * It requires a specific directory structure.
   *
   * For more information, see
   * [Directory structure](https://github.com/Kotlin/dokka/blob/master/plugins/versioning/README.md#directory-structure).
   */
  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val olderVersionsDir: DirectoryProperty

  /**
   * An optional list of paths to other documentation versions. It must point to Dokka's outputs
   * directly. This is useful if different versions can't all be in the same directory.
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val olderVersions: ConfigurableFileCollection

  /**
   * An optional boolean value indicating whether to render the navigation dropdown on all pages.
   *
   * Set to `true` by default.
   */
  @get:Input
  @get:Optional
  abstract val renderVersionsNavigationOnAllPages: Property<Boolean>

  override fun jsonEncode(): String =
    buildJsonObject {
      putIfNotNull("version", version.orNull)
      putJsonArray("versionsOrdering") { addAllIfNotNull(versionsOrdering.orNull) }
      putIfNotNull("olderVersionsDir", olderVersionsDir.orNull?.asFile)
      putJsonArray("olderVersions") {
        addAll(olderVersions.files)
      }
      putIfNotNull("renderVersionsNavigationOnAllPages", renderVersionsNavigationOnAllPages.orNull)
    }.toString()

  companion object {
    const val DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME = "versioning"
    const val DOKKA_VERSIONING_PLUGIN_FQN = "org.jetbrains.dokka.versioning.VersioningPlugin"
  }
}
