package org.jetbrains.dokka

import java.io.File

public trait Location {
    val path: String get
    fun relativePathTo(other: Location): String
}

/**
 * Represents locations in the documentation in the form of [path](File).
 *
 * Locations are provided by [LocationService.location] function.
 *
 * $file: [File] for this location
 * $path: [String] representing path of this location
 */
public data class FileLocation(val file: File): Location {
    override val path : String
        get() = file.path

    override fun relativePathTo(other: Location): String {
        if (other !is FileLocation) {
            throw IllegalArgumentException("$other is not a FileLocation")
        }
        val ownerFolder = file.getParentFile()!!
        return ownerFolder.getRelativePath(other.file).path
    }
}

/**
 * Provides means of retrieving locations for [DocumentationNode](documentation nodes)
 *
 * `LocationService` determines where documentation for particular node should be generated
 *
 * * [FoldersLocationService] – represent packages and types as folders, members as files in those folders.
 * * [SingleFolderLocationService] – all documentation is generated into single folder using fully qualified names
 * for file names.
 */
public trait LocationService {
    fun withExtension(newExtension: String) = this

    /**
     * Calculates location for particular node in output structure
     */
    fun location(node: DocumentationNode): Location
}


public trait FileLocationService: LocationService {
    override fun location(node: DocumentationNode): FileLocation
}


public fun identifierToFilename(path: String): String {
    val escaped = path.replace('<', '-').replace('>', '-')
    val lowercase = escaped.replaceAll("[A-Z]") { matchResult -> "-" + matchResult.group().toLowerCase() }
    return if (lowercase == "index") "--index--" else lowercase
}

/**
 * Returns relative location between two nodes. Used for relative links in documentation.
 */
fun LocationService.relativePathToLocation(owner: DocumentationNode, node: DocumentationNode): String {
    return location(owner).relativePathTo(location(node))
}
