package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import java.io.File

public fun SingleFolderLocationService(root: String): SingleFolderLocationService = SingleFolderLocationService(File(root), "")
public class SingleFolderLocationService @Inject constructor(@Named("outputDir") val root: File, val extension: String) : FileLocationService {
    override fun withExtension(newExtension: String): FileLocationService =
        SingleFolderLocationService(root, newExtension)

    override fun location(qualifiedName: List<String>, hasMembers: Boolean): FileLocation {
        val filename = qualifiedName.map { identifierToFilename(it) }.joinToString("-")
        return FileLocation(File(root, filename).appendExtension(extension))
    }
}