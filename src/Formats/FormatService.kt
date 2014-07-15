package org.jetbrains.dokka

public trait FormatService {
    val extension: String
    fun appendNodes(to: StringBuilder,
               nodes: Iterable<DocumentationNode>)
}

fun FormatService.format(nodes: Iterable<DocumentationNode>): String = StringBuilder { appendNodes(this, nodes) }.toString()