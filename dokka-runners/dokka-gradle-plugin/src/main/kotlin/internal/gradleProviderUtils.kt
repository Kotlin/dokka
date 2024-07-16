package dev.adamko.dokkatoo.internal

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
