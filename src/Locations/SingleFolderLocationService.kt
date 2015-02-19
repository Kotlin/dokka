package org.jetbrains.dokka

import java.io.File

public fun SingleFolderLocationService(root: String): SingleFolderLocationService = SingleFolderLocationService(File(root))
public class SingleFolderLocationService(val root: File) : FileLocationService {
    override fun location(node: DocumentationNode): FileLocation {
        val filename = node.path.map { identifierToFilename(it.name) }.joinToString("-")
        return FileLocation(File(root, filename))
    }
}