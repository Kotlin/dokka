/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import kotlin.RequiresOptIn.Level.WARNING
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*


/**
 * Functionality annotated with this API is intended only for use by the
 * Dokka Gradle plugin, but it has been given
 * [`public` visibility](https://kotlinlang.org/docs/visibility-modifiers.html)
 * for technical reasons.
 *
 * Anyone is welcome to
 * [opt in](https://kotlinlang.org/docs/opt-in-requirements.html#opt-in-to-using-api)
 * to use this API, but be aware that it might change unexpectedly and without warning or migration
 * hints.
 *
 * If you find yourself needing to opt in, then please report your use-case on
 * [the Dokka issue tracker](https://github.com/Kotlin/dokka/issues).
 */
@RequiresOptIn(
    message = "Internal Dokka Gradle API - may change at any time without notice",
    level = WARNING,
)
@Retention(BINARY)
@Target(
    CLASS,
    FUNCTION,
    CONSTRUCTOR,
    PROPERTY,
    PROPERTY_GETTER,
)
@MustBeDocumented
annotation class InternalDokkaGradlePluginApi
