package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator

internal fun unquotedValue(value: String): String = value.removeSurrounding("\"")
