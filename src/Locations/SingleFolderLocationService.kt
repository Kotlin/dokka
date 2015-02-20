package org.jetbrains.dokka

import java.io.File

public fun SingleFolderLocationService(root: String): SingleFolderLocationService = SingleFolderLocationService(File(root), "")
public class SingleFolderLocationService(val root: File, val extension: String) : FileLocationService {
    override fun withExtension(newExtension: String): LocationService =
        SingleFolderLocationService(root, newExtension)

    override fun location(qualifiedName: List<String>, hasMembers: Boolean): FileLocation {
        val filename = qualifiedName.map { identifierToFilename(it) }.joinToString("-")
        return FileLocation(File(root, filename).appendExtension(extension))
    }
}