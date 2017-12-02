package org.jetbrains.dokka

import java.io.File

interface Location {
    val path: String get
    fun relativePathTo(other: Location, anchor: String? = null): String
}

/**
 * Represents locations in the documentation in the form of [path](File).
 *
 * Locations are provided by [LocationService.location] function.
 *
 * $file: [File] for this location
 * $path: [String] representing path of this location
 */
data class FileLocation(val file: File): Location {
    override val path : String
        get() = file.path

    override fun relativePathTo(other: Location, anchor: String?): String {
        if (other !is FileLocation) {
            throw IllegalArgumentException("$other is not a FileLocation")
        }
        if (file.path.substringBeforeLast(".") == other.file.path.substringBeforeLast(".") && anchor == null) {
            return "./${file.name}"
        }
        val ownerFolder = file.parentFile!!
        val relativePath = ownerFolder.toPath().relativize(other.file.toPath()).toString().replace(File.separatorChar, '/')
        return if (anchor == null) relativePath else relativePath + "#" + anchor
    }
}



fun relativePathToNode(qualifiedName: List<String>, hasMembers: Boolean): String {
    val parts = qualifiedName.map { identifierToFilename(it) }.filterNot { it.isEmpty() }
    return if (!hasMembers) {
        // leaf node, use file in owner's folder
        parts.joinToString("/")
    } else {
        parts.joinToString("/") + (if (parts.none()) "" else "/") + "index"
    }
}

fun relativePathToNode(node: DocumentationNode) = relativePathToNode(node.path.map { it.name }, node.members.any())


//
///**
// * Provides means of retrieving locations for [DocumentationNode](documentation nodes)
// *
// * `LocationService` determines where documentation for particular node should be generated
// *
// * * [FoldersLocationService] – represent packages and types as folders, members as files in those folders.
// * * [SingleFolderLocationService] – all documentation is generated into single folder using fully qualified names
// * for file names.
// */
//interface LocationService {
//    fun withExtension(newExtension: String) = this
//
//    fun location(node: DocumentationNode): Location = location(node.path.map { it.name }, node.members.any())
//
//    /**
//     * Calculates a location corresponding to the specified [qualifiedName].
//     * @param hasMembers if true, the node for which the location is calculated has member nodes.
//     */
//    fun location(qualifiedName: List<String>, hasMembers: Boolean): Location
//
//    val root: Location
//}
//
//
//interface FileLocationService: LocationService {
//    override fun withExtension(newExtension: String): FileLocationService = this
//
//    override fun location(node: DocumentationNode): FileLocation = location(node.path.map { it.name }, node.members.any())
//    override fun location(qualifiedName: List<String>, hasMembers: Boolean): FileLocation
//}

fun identifierToFilename(path: String): String {
    val escaped = path.replace('<', '-').replace('>', '-')
    val lowercase = escaped.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    return if (lowercase == "index") "--index--" else lowercase
}

///**
// * Returns relative location between two nodes. Used for relative links in documentation.
// */
//fun LocationService.relativePathToLocation(owner: DocumentationNode, node: DocumentationNode): String {
//    return location(owner).relativePathTo(location(node), null)
//}


fun NodeLocationAwareGenerator.relativePathToLocation(owner: DocumentationNode, node: DocumentationNode): String {
    return location(owner).relativePathTo(location(node), null)
}


fun NodeLocationAwareGenerator.relativeToRoot(from: Location): File {
    val file = File(from.path)
    return file.relativeTo(root)
}