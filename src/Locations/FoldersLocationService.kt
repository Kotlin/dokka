package org.jetbrains.dokka

import java.io.File

public fun FoldersLocationService(root: String): FoldersLocationService = FoldersLocationService(File(root))
public class FoldersLocationService(val root: File) : FileLocationService {
    override fun location(node: DocumentationNode): FileLocation {
        return FileLocation(File(root, relativePathToNode(node)))
    }
}

fun relativePathToNode(node: DocumentationNode): String {
    val parts = node.path.map { identifierToFilename(it.name) }.filterNot { it.isEmpty() }
    return if (node.members.none()) {
        // leaf node, use file in owner's folder
        parts.joinToString("/")
    } else {
        parts.joinToString("/") + (if (parts.none()) "" else "/") + "index"
    }
}
