/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.provider.Provider

/** Invert a boolean [Provider] */
internal operator fun Provider<Boolean>.not(): Provider<Boolean> =
    map { !it }

internal infix fun Provider<Boolean>.or(right: Provider<Boolean>): Provider<Boolean> =
    zip(right) { l, r -> l || r }

internal infix fun Provider<Boolean>.and(right: Provider<Boolean>): Provider<Boolean> =
    zip(right) { l, r -> l && r }

internal infix fun Provider<Boolean>.and(right: Boolean): Provider<Boolean> =
    map { left -> left && right }

internal infix fun Boolean.and(right: Provider<Boolean>): Provider<Boolean> =
    right.map { r -> this && r }

internal infix fun Boolean.or(right: Provider<Boolean>): Provider<Boolean> =
    right.map { r -> this || r }

internal infix fun Provider<Boolean>.or(right: Boolean): Provider<Boolean> =
    map { l -> l || right }

internal fun <T> Provider<T>.forUseAtConfigurationTimeCompat(): Provider<T> =
    if (CurrentGradleVersion < "7.0") {
        @Suppress("DEPRECATION")
        forUseAtConfigurationTime()
    } else {
        this
    }
