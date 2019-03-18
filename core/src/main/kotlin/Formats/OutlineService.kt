package org.jetbrains.dokka

import java.io.File

/**
 * Service for building the outline of the package contents.
 */
interface OutlineFormatService {
    fun getOutlineFileName(location: Location): File

    fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder)
    fun appendOutlineLevel(to: StringBuilder, body: () -> Unit)

    /** Appends formatted outline to [StringBuilder](to) using specified [location] */
    fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        for (node in nodes) {
            appendOutlineHeader(location, node, to)
            if (node.members.any()) {
                val sortedMembers = node.members.sortedBy { it.name.toLowerCase() }
                appendOutlineLevel(to) {
                    appendOutline(location, to, sortedMembers)
                }
            }
        }
    }

    fun formatOutline(location: Location, nodes: Iterable<DocumentationNode>): String =
            StringBuilder().apply { appendOutline(location, this, nodes) }.toString()
}
