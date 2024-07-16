package dev.adamko.dokkatoo.internal

internal inline fun <T, R> Set<T>.mapToSet(transform: (T) -> R): Set<R> =
  mapTo(mutableSetOf(), transform)

internal inline fun <T, R : Any> Set<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> =
  mapNotNullTo(mutableSetOf(), transform)
