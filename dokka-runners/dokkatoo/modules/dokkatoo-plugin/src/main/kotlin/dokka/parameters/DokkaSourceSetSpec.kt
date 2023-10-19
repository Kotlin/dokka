package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetIdSpec.Companion.dokkaSourceSetIdSpec
import org.jetbrains.dokka.dokkatoo.internal.*
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*

/**
 * [Source set](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets) level configuration.
 *
 * Can be configured in the following way with Gradle Kotlin DSL:
 *
 * ```kotlin
 * // build.gradle.kts
 *
 * dokkatoo {
 *   dokkatooSourceSets {
 *     // configure individual source set by name
 *     named("customSourceSet") {
 *       suppress.set(true)
 *     }
 *
 *     // configure all source sets at once
 *     configureEach {
 *       reportUndocumented.set(true)
 *     }
 *   }
 * }
 * ```
 */
abstract class DokkaSourceSetSpec
@DokkatooInternalApi
@Inject
constructor(
  private val name: String,
  private val objects: ObjectFactory,
) :
  HasConfigurableVisibilityModifiers,
  Named,
  Serializable,
  ExtensionAware {

  @Internal // will be tracked by sourceSetId
  override fun getName(): String = name

  /**
   * An arbitrary string used to group source sets that originate from different Gradle subprojects.
   * This is primarily used by Kotlin Multiplatform projects, which can have multiple source sets
   * per subproject.
   *
   * The default is set from [DokkatooExtension.sourceSetScopeDefault][org.jetbrains.dokka.dokkatoo.DokkatooExtension.sourceSetScopeDefault]
   *
   * It's unlikely that this value needs to be changed.
   */
  @get:Internal // will be tracked by sourceSetId
  abstract val sourceSetScope: Property<String>

  /**
   * The identifier for this source set, across all Gradle subprojects.
   *
   * @see sourceSetScope
   * @see getName
   */
  @get:Input
  val sourceSetId: Provider<DokkaSourceSetIdSpec>
    get() = sourceSetScope.map { scope -> objects.dokkaSourceSetIdSpec(scope, getName()) }

  /**
   * Whether this source set should be skipped when generating documentation.
   *
   * Default is `false`.
   */
  @get:Input
  abstract val suppress: Property<Boolean>

  /**
   * Display name used to refer to the source set.
   *
   * The name will be used both externally (for example, source set name visible to documentation readers) and
   * internally (for example, for logging messages of [reportUndocumented]).
   *
   * By default, the value is deduced from information provided by the Kotlin Gradle plugin.
   */
  @get:Input
  abstract val displayName: Property<String>

  /**
   * List of Markdown files that contain
   * [module and package documentation](https://kotlinlang.org/docs/reference/dokka-module-and-package-docs.html).
   *
   * Contents of specified files will be parsed and embedded into documentation as module and package descriptions.
   *
   * Example of such a file:
   *
   * ```markdown
   * # Module kotlin-demo
   *
   * The module shows the Dokka usage.
   *
   * # Package org.jetbrains.kotlin.demo
   *
   * Contains assorted useful stuff.
   *
   * ## Level 2 heading
   *
   * Text after this heading is also part of documentation for `org.jetbrains.kotlin.demo`
   *
   * # Package org.jetbrains.kotlin.demo2
   *
   * Useful stuff in another package.
   * ```
   */
  @get:InputFiles
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val includes: ConfigurableFileCollection

  /**
   * Set of visibility modifiers that should be documented.
   *
   * This can be used if you want to document protected/internal/private declarations,
   * as well as if you want to exclude public declarations and only document internal API.
   *
   * Can be configured on per-package basis, see [DokkaPackageOptionsSpec.documentedVisibilities].
   *
   * Default is [VisibilityModifier.PUBLIC].
   */
  @get:Input
  abstract override val documentedVisibilities: SetProperty<VisibilityModifier>

  /**
   * Specifies source sets that current source set depends on.
   *
   * Among other things, this information is needed to resolve
   * [expect/actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html) declarations.
   *
   * By default, the values are deduced from information provided by the Kotlin Gradle plugin.
   */
  @get:Nested
  val dependentSourceSets: NamedDomainObjectContainer<DokkaSourceSetIdSpec> =
    extensions.adding("dependentSourceSets", objects.domainObjectContainer())

  /**
   * Classpath for analysis and interactive samples.
   *
   * Useful if some types that come from dependencies are not resolved/picked up automatically.
   * Property accepts both `.jar` and `.klib` files.
   *
   * By default, classpath is deduced from information provided by the Kotlin Gradle plugin.
   */
  @get:Classpath
  @get:Optional
  abstract val classpath: ConfigurableFileCollection

  /**
   * Source code roots to be analyzed and documented.
   * Accepts directories and individual `.kt` / `.java` files.
   *
   * By default, source roots are deduced from information provided by the Kotlin Gradle plugin.
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceRoots: ConfigurableFileCollection

  /**
   * List of directories or files that contain sample functions which are referenced via
   * [`@sample`](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier) KDoc tag.
   */
  @get:InputFiles
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val samples: ConfigurableFileCollection

  /**
   * Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
   * after they have been filtered by [documentedVisibilities].
   *
   * Can be overridden for a specific package by setting [DokkaPackageOptionsSpec.reportUndocumented].
   *
   * Default is `false`.
   */
  @get:Input
  abstract val reportUndocumented: Property<Boolean>

  /**
   * Specifies the location of the project source code on the Web. If provided, Dokka generates
   * "source" links for each declaration. See [DokkaSourceLinkSpec] for more details.
   *
   * Prefer using [sourceLink] action/closure for adding source links.
   *
   * @see sourceLink
   */
  @get:Nested
  abstract val sourceLinks: DomainObjectSet<DokkaSourceLinkSpec>

  /**
   * Allows to customize documentation generation options on a per-package basis.
   *
   * @see DokkaPackageOptionsSpec for details
   */
  @get:Nested
  abstract val perPackageOptions: DomainObjectSet<DokkaPackageOptionsSpec>

  /**
   * Allows linking to Dokka/Javadoc documentation of the project's dependencies.
   */
  @get:Nested
  val externalDocumentationLinks: NamedDomainObjectContainer<DokkaExternalDocumentationLinkSpec> =
    extensions.adding("externalDocumentationLinks", objects.domainObjectContainer())

  /**
   * Platform to be used for setting up code analysis and samples.
   *
   * The default value is deduced from information provided by the Kotlin Gradle plugin.
   */
  @get:Input
  abstract val analysisPlatform: Property<KotlinPlatform>

  /**
   * Whether to skip packages that contain no visible declarations after
   * various filters have been applied.
   *
   * For instance, if [skipDeprecated] is set to `true` and your package contains only
   * deprecated declarations, it will be considered to be empty.
   *
   * Default is `true`.
   */
  @get:Input
  abstract val skipEmptyPackages: Property<Boolean>

  /**
   * Whether to document declarations annotated with [Deprecated].
   *
   * Can be overridden on package level by setting [DokkaPackageOptionsSpec.skipDeprecated].
   *
   * Default is `false`.
   */
  @get:Input
  abstract val skipDeprecated: Property<Boolean>

  /**
   * Directories or individual files that should be suppressed, meaning declarations from them
   * will be not documented.
   *
   * Will be concatenated with generated files if [suppressGeneratedFiles] is set to `false`.
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val suppressedFiles: ConfigurableFileCollection

  /**
   * Whether to document/analyze generated files.
   *
   * Generated files are expected to be present under `{project}/{buildDir}/generated` directory.
   * If set to `true`, it effectively adds all files from that directory to [suppressedFiles], so
   * you can configure it manually.
   *
   * Default is `true`.
   */
  @get:Input
  abstract val suppressGeneratedFiles: Property<Boolean>

  /**
   * Whether to generate external documentation links that lead to API reference documentation for
   * Kotlin's standard library when declarations from it are used.
   *
   * Default is `true`, meaning links will be generated.
   *
   * @see externalDocumentationLinks
   */
  @get:Input
  abstract val enableKotlinStdLibDocumentationLink: Property<Boolean>

  /**
   * Whether to generate external documentation links to JDK's Javadocs when declarations from it
   * are used.
   *
   * The version of JDK Javadocs is determined by [jdkVersion] property.
   *
   * Default is `true`, meaning links will be generated.
   *
   * @see externalDocumentationLinks
   */
  @get:Input
  abstract val enableJdkDocumentationLink: Property<Boolean>

  /**
   * Whether to generate external documentation links for Android SDK API reference when
   * declarations from it are used.
   *
   * Only relevant in Android projects, ignored otherwise.
   *
   * Default is `false`, meaning links will not be generated.
   *
   * @see externalDocumentationLinks
   */
  @get:Input
  abstract val enableAndroidDocumentationLink: Property<Boolean>

  /**
   * [Kotlin language version](https://kotlinlang.org/docs/compatibility-modes.html)
   * used for setting up analysis and [`@sample`](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
   * environment.
   *
   * By default, the latest language version available to Dokka's embedded compiler will be used.
   */
  @get:Input
  @get:Optional
  abstract val languageVersion: Property<String?>

  /**
   * [Kotlin API version](https://kotlinlang.org/docs/compatibility-modes.html)
   * used for setting up analysis and [`@sample`](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
   * environment.
   *
   * By default, it will be deduced from [languageVersion].
   */
  @get:Input
  @get:Optional
  abstract val apiVersion: Property<String?>

  /**
   * JDK version to use when generating external documentation links for Java types.
   *
   * For instance, if you use [java.util.UUID] from JDK in some public declaration signature,
   * and this property is set to `8`, Dokka will generate an external documentation link
   * to [JDK 8 Javadocs](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html) for it.
   *
   * Default is JDK 8.
   */
  @get:Input
  abstract val jdkVersion: Property<Int>

  /**
   * Configure and add a new source link to [sourceLinks].
   *
   * @see DokkaSourceLinkSpec
   */
  fun sourceLink(action: Action<in DokkaSourceLinkSpec>) {
    sourceLinks.add(
      objects.newInstance(DokkaSourceLinkSpec::class).also {
        action.execute(it)
      }
    )
  }

  /**
   * Action for configuring package options, appending to [perPackageOptions].
   *
   * @see DokkaPackageOptionsSpec
   */
  fun perPackageOption(action: Action<in DokkaPackageOptionsSpec>) {
    perPackageOptions.add(
      objects.newInstance(DokkaPackageOptionsSpec::class).also {
        action.execute(it)
      }
    )
  }
}
