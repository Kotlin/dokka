package org.jetbrains.dokka.analysis.java.util

import java.util.*

// TODO [beresnev] copy-paste

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
internal fun <T, R : Any> Iterable<T>.firstNotNullOfOrNull(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) {
            return result
        }
    }
    return null
}

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
fun Char.uppercaseChar(): Char = Character.toUpperCase(this)

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
fun Char.lowercaseChar(): Char = Character.toLowerCase(this)

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
fun String.lowercase(): String = this.toLowerCase(Locale.ROOT)

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
fun String.uppercase(): String = this.toUpperCase(Locale.ROOT)

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
fun String.replaceFirstChar(transform: (Char) -> Char): String {
    return if (isNotEmpty()) transform(this[0]) + substring(1) else this
}
