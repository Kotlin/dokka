/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dokka.parameters

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
    Public,

    /** `private` modifier for both Kotlin and Java */
    Private,

    /** `protected` modifier for both Kotlin and Java */
    Protected,

    /** Kotlin-specific `internal` modifier */
    Internal,

    /** Java-specific package-private visibility (no modifier) */
    Package,
    ;

    companion object {
        // replace with `entries` when Kotlin lang level is 1.9
        internal val values: Set<VisibilityModifier> = values().toSet()

        // Not defined as a property to try and minimize the dependency on Dokka Core types
        internal val VisibilityModifier.dokkaType: DokkaConfiguration.Visibility
            get() = when (this) {
                Public -> DokkaConfiguration.Visibility.PUBLIC
                Private -> DokkaConfiguration.Visibility.PRIVATE
                Protected -> DokkaConfiguration.Visibility.PROTECTED
                Internal -> DokkaConfiguration.Visibility.INTERNAL
                Package -> DokkaConfiguration.Visibility.PACKAGE
            }
    }
}
