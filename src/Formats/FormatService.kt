package org.jetbrains.dokka

public trait FormatService {
    val extension: String
    fun format(nodes: Iterable<DocumentationNode>, to: StringBuilder)
}

fun FormatService.format(node: Iterable<DocumentationNode>): String = StringBuilder { format(node, this) }.toString()