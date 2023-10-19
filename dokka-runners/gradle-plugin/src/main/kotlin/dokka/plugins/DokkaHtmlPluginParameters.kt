package org.jetbrains.dokka.dokkatoo.dokka.plugins

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.addAll
import org.jetbrains.dokka.dokkatoo.internal.putIfNotNull
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE


/**
 * Configuration for Dokka's base HTML format
 *
 * [More information is available in the Dokka docs.](https://kotlinlang.org/docs/dokka-html.html#configuration)
 */
abstract class DokkaHtmlPluginParameters
@DokkatooInternalApi
@Inject
constructor(
  name: String
) : DokkaPluginParametersBaseSpec(
  name,
  DOKKA_HTML_PLUGIN_FQN,
) {

  /**
   * List of paths for image assets to be bundled with documentation.
   * The image assets can have any file extension.
   *
   * For more information, see
   * [Customizing assets](https://kotlinlang.org/docs/dokka-html.html#customize-assets).
   *
   * Be aware that files will be copied as-is to a specific directory inside the assembled Dokka
   * publication. This means that any relative paths must be written in such a way that they will
   * work _after_ the files are moved into the publication.
   *
   * It's best to try and mirror Dokka's directory structure in the source files, which can help
   * IDE inspections.
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val customAssets: ConfigurableFileCollection

  /**
   * List of paths for `.css` stylesheets to be bundled with documentation and used for rendering.
   *
   * For more information, see
   * [Customizing assets](https://kotlinlang.org/docs/dokka-html.html#customize-assets).
   *
   * Be aware that files will be copied as-is to a specific directory inside the assembled Dokka
   * publication. This means that any relative paths must be written in such a way that they will
   * work _after_ the files are moved into the publication.
   *
   * It's best to try and mirror Dokka's directory structure in the source files, which can help
   * IDE inspections.
   */
  @get:InputFiles
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val customStyleSheets: ConfigurableFileCollection

  /**
   * This is a boolean option. If set to `true`, Dokka renders properties/functions and inherited
   * properties/inherited functions separately.
   *
   * This is disabled by default.
   */
  @get:Input
  @get:Optional
  abstract val separateInheritedMembers: Property<Boolean>

  /**
   * This is a boolean option. If set to `true`, Dokka merges declarations that are not declared as
   * [expect/actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html), but have the
   * same fully qualified name. This can be useful for legacy codebases.
   *
   * This is disabled by default.
   */
  @get:Input
  @get:Optional
  abstract val mergeImplicitExpectActualDeclarations: Property<Boolean>

  /** The text displayed in the footer. */
  @get:Input
  @get:Optional
  abstract val footerMessage: Property<String>

  /**
   * Path to the directory containing custom HTML templates.
   *
   * For more information, see [Templates](https://kotlinlang.org/docs/dokka-html.html#templates).
   */
  @get:InputDirectory
  @get:PathSensitive(RELATIVE)
  @get:Optional
  abstract val templatesDir: DirectoryProperty

  override fun jsonEncode(): String =
    buildJsonObject {
      putJsonArray("customAssets") {
        addAll(customAssets.files)
      }
      putJsonArray("customStyleSheets") {
        addAll(customStyleSheets.files)
      }
      putIfNotNull("separateInheritedMembers", separateInheritedMembers.orNull)
      putIfNotNull(
        "mergeImplicitExpectActualDeclarations",
        mergeImplicitExpectActualDeclarations.orNull
      )
      putIfNotNull("footerMessage", footerMessage.orNull)
      putIfNotNull("footerMessage", footerMessage.orNull)
      putIfNotNull(
        "templatesDir",
        templatesDir.orNull?.asFile?.canonicalFile?.invariantSeparatorsPath
      )
    }.toString()

  companion object {
    const val DOKKA_HTML_PARAMETERS_NAME = "html"
    const val DOKKA_HTML_PLUGIN_FQN = "org.jetbrains.dokka.base.DokkaBase"
  }
}
