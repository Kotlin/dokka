package org.jetbrains.dokka

import java.io.File

public fun FoldersLocationService(root: String): FoldersLocationService = FoldersLocationService(File(root))
public class FoldersLocationService(val root: File) : LocationService {
    override fun location(node: DocumentationNode): Location {
        val (pageNode, anchor) = getParentPage(node)
        val parts = pageNode.path.map { escapeUri(it.name) }
        val folder = if (node.members.none()) {
            // leaf node, use file in owner's folder
            parts.joinToString("/", limit = parts.size - 1, truncated = "") + "/" + parts.last()
        } else {
            parts.joinToString("/") + (if (parts.none()) "" else "/") + "index"
        }
        return Location(File(root, folder), anchor)
    }
}