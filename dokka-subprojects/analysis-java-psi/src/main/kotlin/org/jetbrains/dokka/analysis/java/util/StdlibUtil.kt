/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
internal fun Char.uppercaseChar(): Char = Character.toUpperCase(this)

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
internal fun Char.lowercaseChar(): Char = Character.toLowerCase(this)

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
internal fun String.lowercase(): String = this.toLowerCase(Locale.ROOT)

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
internal fun String.uppercase(): String = this.toUpperCase(Locale.ROOT)

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
internal fun String.replaceFirstChar(transform: (Char) -> Char): String {
    return if (isNotEmpty()) transform(this[0]) + substring(1) else this
}
