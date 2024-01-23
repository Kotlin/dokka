/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test

import org.junit.jupiter.api.Tag

// COPY OF dokka-subprojects/plugin-base/src/test/kotlin/utils/TagsAnnotations.kt

/**
 * Run a test only for descriptors, not symbols.
 *
 * In theory, these tests can be fixed
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyDescriptors")
annotation class OnlyDescriptors(val reason: String = "")


/**
 * Run a test only for symbols (aka K2), not descriptors (K1).
 *
 * After remove K1 in dokka, this annotation should be also removed without consequences
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlySymbols")
annotation class OnlySymbols(val reason: String = "")

/**
 * Run a test only for descriptors, not symbols.
 *
 * These tests cannot be fixed until Analysis API does not support MPP
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyDescriptorsMPP")
annotation class OnlyDescriptorsMPP(val reason: String = "")
