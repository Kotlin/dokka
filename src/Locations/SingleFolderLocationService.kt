package org.jetbrains.dokka

import java.io.File
import org.jetbrains.dokka.DocumentationNode.Kind

public fun SingleFolderLocationService(root: String): SingleFolderLocationService = SingleFolderLocationService(File(root))
public class SingleFolderLocationService(val root: File) : LocationService {
    override fun location(node: DocumentationNode): Location {
        val (pageNode, anchor) = getParentPage(node)
        val filename = pageNode.path.map { escapeUri(it.name) }.joinToString("-")
        return Location(File(root, filename), anchor)
    }
}