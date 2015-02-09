package org.jetbrains.dokka

import java.io.File

public fun SingleFolderLocationService(root: String): SingleFolderLocationService = SingleFolderLocationService(File(root))
public class SingleFolderLocationService(val root: File) : LocationService {
    override fun location(node: DocumentationNode): Location {
        val filename = node.path.map { identifierToFilename(it.name) }.joinToString("-")
        return Location(File(root, filename))
    }
}