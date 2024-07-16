package dev.adamko.dokkatoo.internal


/**
 * Title case the first char of a string.
 *
 * (Custom implementation because [uppercase] is deprecated, and Dokkatoo should try and be as
 * stable as possible.)
 */
internal fun String.uppercaseFirstChar(): String =
  if (isNotEmpty()) Character.toTitleCase(this[0]) + substring(1) else this
