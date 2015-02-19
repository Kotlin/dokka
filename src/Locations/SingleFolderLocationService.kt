package org.jetbrains.dokka

import java.io.File

public fun SingleFolderLocationService(root: String): SingleFolderLocationService = SingleFolderLocationService(File(root), "")
public class SingleFolderLocationService(val root: File, val extension: String) : FileLocationService {
    override fun withExtension(newExtension: String): LocationService =
        SingleFolderLocationService(root, newExtension)

    override fun location(node: DocumentationNode): FileLocation {
        val filename = node.path.map { identifierToFilename(it.name) }.joinToString("-")
        return FileLocation(File(root, filename).appendExtension(extension))
    }
}