package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import java.io.File

public fun SingleFolderLocationService(root: String): SingleFolderLocationService = SingleFolderLocationService(File(root), "")
public class SingleFolderLocationService @Inject constructor(@Named("outputDir") val rootFile: File, val extension: String) : FileLocationService {
    override fun withExtension(newExtension: String): FileLocationService =
        SingleFolderLocationService(rootFile, newExtension)

    override fun location(qualifiedName: List<String>, hasMembers: Boolean): FileLocation {
        val filename = qualifiedName.map { identifierToFilename(it) }.joinToString("-")
        return FileLocation(File(rootFile, filename).appendExtension(extension))
    }

    override val root: Location
        get() = FileLocation(rootFile)
}