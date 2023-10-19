package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.DokkaConfiguration

/**
 * Denotes the
 * [visibility modifier](https://kotlinlang.org/docs/visibility-modifiers.html)
 * of a source code elements.
 *
 * @see org.jetbrains.dokka.DokkaConfiguration.Visibility
 */
enum class VisibilityModifier {
  /** `public` modifier for Java, default visibility for Kotlin */
  PUBLIC,

  /** `private` modifier for both Kotlin and Java */
  PRIVATE,

  /** `protected` modifier for both Kotlin and Java */
  PROTECTED,

  /** Kotlin-specific `internal` modifier */
  INTERNAL,

  /** Java-specific package-private visibility (no modifier) */
  PACKAGE,
  ;

  companion object {
    internal val entries: Set<VisibilityModifier> = values().toSet()

    // Not defined as a property to try and minimize the dependency on Dokka Core types
    internal val VisibilityModifier.dokkaType: DokkaConfiguration.Visibility
      get() = when (this) {
        PUBLIC    -> DokkaConfiguration.Visibility.PUBLIC
        PRIVATE   -> DokkaConfiguration.Visibility.PRIVATE
        PROTECTED -> DokkaConfiguration.Visibility.PROTECTED
        INTERNAL  -> DokkaConfiguration.Visibility.INTERNAL
        PACKAGE   -> DokkaConfiguration.Visibility.PACKAGE
      }
  }
}
