package buildsrc.utils


/** Title case the first char of a string */
internal fun String.uppercaseFirstChar(): String = mapFirstChar(Character::toTitleCase)


/** Lowercase the first char of a string */
internal fun String.lowercaseFirstChar(): String = mapFirstChar(Character::toLowerCase)


private inline fun String.mapFirstChar(
  transform: (Char) -> Char
): String = if (isNotEmpty()) transform(this[0]) + substring(1) else this


/**
 * Exclude all non-alphanumeric characters and converts the result into a camelCase string.
 */
internal fun String.toAlphaNumericCamelCase(): String =
  map { if (it.isLetterOrDigit()) it else ' ' }
    .joinToString("")
    .split(" ")
    .filter { it.isNotBlank() }
    .joinToString("") { it.uppercaseFirstChar() }
    .lowercaseFirstChar()
