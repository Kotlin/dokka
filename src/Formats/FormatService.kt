package org.jetbrains.dokka

public trait FormatService {
    val extension: String
    fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>)
    fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>)
}

fun FormatService.format(location: Location, nodes: Iterable<DocumentationNode>): String = StringBuilder { appendNodes(location, this, nodes) }.toString()
fun FormatService.formatOutline(location: Location, nodes: Iterable<DocumentationNode>): String = StringBuilder { appendOutline(location, this, nodes) }.toString()