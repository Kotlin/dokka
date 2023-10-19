package org.jetbrains.dokka.dokkatoo.utils


fun String.splitToPair(delimiter: String): Pair<String, String> =
  substringBefore(delimiter) to substringAfter(delimiter)


/** Title case the first char of a string */
fun String.uppercaseFirstChar(): String = mapFirstChar(Character::toTitleCase)


private inline fun String.mapFirstChar(
  transform: (Char) -> Char
): String = if (isNotEmpty()) transform(this[0]) + substring(1) else this


/** Split a string into lines, sort the lines, and re-join them (using [separator]). */
fun String.sortLines(separator: String = "\n") =
  lines()
    .sorted()
    .joinToString(separator)
