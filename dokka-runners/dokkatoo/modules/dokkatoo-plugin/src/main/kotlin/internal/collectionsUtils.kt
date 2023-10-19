package org.jetbrains.dokka.dokkatoo.internal

internal fun <T, R> Set<T>.mapToSet(transform: (T) -> R): Set<R> =
  mapTo(mutableSetOf(), transform)

internal fun <T, R : Any> Set<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> =
  mapNotNullTo(mutableSetOf(), transform)
