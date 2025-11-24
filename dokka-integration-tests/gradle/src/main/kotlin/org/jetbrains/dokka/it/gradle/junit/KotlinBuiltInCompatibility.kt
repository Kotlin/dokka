/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.junit

/**
 * Indicates whether an Android test project can run with or without Kotlin built-in.
 */
enum class KotlinBuiltInCompatibility {
    /**
     * The tested project cannot run without Kotlin built-in.
     * Filter out AGP versions that don't support Kotlin built-in.
     */
    Required,

    /**
     * The tested project can run with or without Kotlin built-in.
     * No AGP versions will be filtered.
     *
     * (Currently no test projects work the same with and without Kotlin built-in,
     * this option is here just to be the default if no option is provided.)
     */
    Supported,

    /**
     * The tested project cannot run with Kotlin built-in.
     * Filter out AGP versions that use Kotlin built-in.
     */
    Incompatible,
}
