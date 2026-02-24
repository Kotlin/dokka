/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinAsJavaPlugin.utils

import org.junit.jupiter.api.Tag

/**
 * Tests that pass with PSI-based Java analysis but fail with symbol-based Java analysis.
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
@Tag("onlyJavaPsi")
annotation class OnlyJavaPsi(val reason: String = "")
