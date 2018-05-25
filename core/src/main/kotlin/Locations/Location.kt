package org.jetbrains.dokka

import java.io.File

interface Location {
    val path: String get
    fun relativePathTo(other: Location, anchor: String? = null): String
}

/**
 * Represents locations in the documentation in the form of [path](File).
 *
 * $file: [File] for this location
 * $path: [String] representing path of this location
 */
data class FileLocation(val file: File) : Location {
    override val path: String
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

fun identifierToFilename(path: String): String {
    val escaped = path.replace('<', '-').replace('>', '-')
    val lowercase = escaped.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    return if (lowercase == "index") "--index--" else lowercase
}

fun NodeLocationAwareGenerator.relativePathToLocation(owner: DocumentationNode, node: DocumentationNode): String {
    return location(owner).relativePathTo(location(node), null)
}

fun NodeLocationAwareGenerator.relativePathToRoot(from: Location): File {
    val file = File(from.path).parentFile
    return root.relativeTo(file)
}

fun File.toUnixString() = toString().replace(File.separatorChar, '/')
