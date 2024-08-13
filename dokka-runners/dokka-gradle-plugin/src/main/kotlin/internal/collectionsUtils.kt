/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

internal inline fun <T, R> Set<T>.mapToSet(transform: (T) -> R): Set<R> =
    mapTo(mutableSetOf(), transform)

internal inline fun <T, R : Any> Set<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> =
    mapNotNullTo(mutableSetOf(), transform)
