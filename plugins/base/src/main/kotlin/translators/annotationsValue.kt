package org.jetbrains.dokka.base.translators

internal fun unquotedValue(value: String): String = value.removeSurrounding("\"")