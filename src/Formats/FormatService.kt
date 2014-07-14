package org.jetbrains.dokka

public trait FormatService {
    val extension: String
    fun format(node: DocumentationNode, to: StringBuilder)
}

fun FormatService.format(node: DocumentationNode): String = StringBuilder { format(node, this) }.toString()