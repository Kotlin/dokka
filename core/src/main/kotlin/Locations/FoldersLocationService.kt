package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import java.io.File
//
//class FoldersLocationService @Inject constructor(@Named("outputDir") val rootFile: File, val extension: String) : FileLocationService {
//    constructor(root: String): this(File(root), "")
//
//    override val root: Location
//        get() = FileLocation(rootFile)
//
//    override fun withExtension(newExtension: String): FileLocationService {
//        return if (extension.isEmpty()) FoldersLocationService(rootFile, newExtension) else this
//    }
//
//    override fun location(qualifiedName: List<String>, hasMembers: Boolean): FileLocation {
//        return FileLocation(File(rootFile, relativePathToNode(qualifiedName, hasMembers)).appendExtension(extension))
//    }
//}
