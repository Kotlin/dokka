package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import java.io.Serializable
import java.net.URI
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.intellij.lang.annotations.Language

/**
 * Configuration builder that allows creating links leading to externally hosted
 * documentation of your dependencies.
 *
 * For instance, if you are using types from `kotlinx.serialization`, by default
 * they will be unclickable in your documentation, as if unresolved. However,
 * since API reference for `kotlinx.serialization` is also built by Dokka and is
 * [published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/),
 * you can configure external documentation links for it, allowing Dokka to generate
 * documentation links for used types, making them clickable and appear resolved.
 *
 * Example in Gradle Kotlin DSL:
 *
 * ```kotlin
 * externalDocumentationLink {
 *  url.set(URI("https://kotlinlang.org/api/kotlinx.serialization/"))
 *  packageListUrl.set(
 *    rootProject.projectDir.resolve("serialization.package.list").toURI()
 *  )
 * }
 * ```
 */
abstract class DokkaExternalDocumentationLinkSpec
@DokkatooInternalApi
@Inject
constructor(
  private val name: String
) : Serializable, Named {

  /**
   * Root URL of documentation to link with.
   *
   * Dokka will do its best to automatically find `package-list` for the given URL, and link
   * declarations together.
   *
   * It automatic resolution fails or if you want to use locally cached files instead,
   * consider providing [packageListUrl].
   *
   * Example:
   *
   * ```kotlin
   * java.net.URI("https://kotlinlang.org/api/kotlinx.serialization/")
   * ```
   */
  @get:Input
  abstract val url: Property<URI>

  /**
   * Set the value of [url].
   *
   * @param[value] will be converted to a [URI]
   */
  fun url(@Language("http-url-reference") value: String): Unit =
    url.set(URI(value))

  /**
   * Set the value of [url].
   *
   * @param[value] will be converted to a [URI]
   */
  fun url(value: Provider<String>): Unit =
    url.set(value.map(::URI))

  /**
   * Specifies the exact location of a `package-list` instead of relying on Dokka
   * automatically resolving it. Can also be a locally cached file to avoid network calls.
   *
   * Example:
   *
   * ```kotlin
   * rootProject.projectDir.resolve("serialization.package.list").toURL()
   * ```
   */
  @get:Input
  abstract val packageListUrl: Property<URI>

  /**
   * Set the value of [packageListUrl].
   *
   * @param[value] will be converted to a [URI]
   */
  fun packageListUrl(@Language("http-url-reference") value: String): Unit =
    packageListUrl.set(URI(value))

  /**
   * Set the value of [packageListUrl].
   *
   * @param[value] will be converted to a [URI]
   */
  fun packageListUrl(value: Provider<String>): Unit =
    packageListUrl.set(value.map(::URI))

  /**
   * If enabled this link will be passed to the Dokka Generator.
   *
   * Defaults to `true`.
   *
   * @see org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetSpec.enableKotlinStdLibDocumentationLink
   * @see org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetSpec.enableJdkDocumentationLink
   * @see org.jetbrains.dokka.dokkatoo.dokka.parameters.DokkaSourceSetSpec.enableAndroidDocumentationLink
   */
  @get:Input
  abstract val enabled: Property<Boolean>

  @Internal
  override fun getName(): String = name
}
