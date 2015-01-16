package org.jetbrains.dokka

import java.io.File
import org.jetbrains.dokka.DocumentationNode.Kind

/**
 * Represents locations in the documentation in the form of [path](File).
 *
 * Locations are provided by [LocationService.location] function.
 *
 * $file: [File] for this location
 * $path: [String] representing path of this location
 */
public data class Location(val file: File, val anchor: String? = null) {
    public val path : String
        get() = file.path

    public val pathWithAnchor: String
        get() = if (anchor == null) path else "$path#$anchor"
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
    /**
     * Calculates location for particular node in output structure
     */
    fun location(node: DocumentationNode): Location

    fun getParentPage(node: DocumentationNode): Pair<DocumentationNode, String?> {
        if (node.kind == Kind.Parameter) {
            return node.owner!! to node.name
        }
        return node to null
    }
}


public fun escapeUri(path: String): String = path.replace('<', '-').replace('>', '-')

/**
 * Returns relative location between two nodes. Used for relative links in documentation.
 */
fun LocationService.relativeLocation(owner: DocumentationNode, node: DocumentationNode, extension: String): Location {
    return relativeLocation(location(owner), node, extension)
}

/**
 * Returns relative location between base location and a node. Used for relative links in documentation.
 */
fun LocationService.relativeLocation(owner: Location, node: DocumentationNode, extension: String): Location {
    val ownerFolder = owner.file.getParentFile()!!
    val targetLocation = location(node)
    val memberPath = targetLocation.file.appendExtension(extension)
    return Location(ownerFolder.getRelativePath(memberPath), targetLocation.anchor)
}
