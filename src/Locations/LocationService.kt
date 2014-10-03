package org.jetbrains.dokka

import java.io.File

public data class Location(val file: File) {
    public val path : String
        get() = file.path
}

public trait LocationService {
    fun location(node: DocumentationNode): Location
}


public fun escapeUri(path: String): String = path.replace('<', '_').replace('>', '_')

fun LocationService.relativeLocation(node: DocumentationNode, link: DocumentationNode, extension: String): Location {
    val ownerFolder = location(node).file.getParentFile()!!
    val memberPath = location(link).file.appendExtension(extension)
    return Location(ownerFolder.getRelativePath(memberPath))
}

fun LocationService.relativeLocation(location: Location, link: DocumentationNode, extension: String): Location {
    val ownerFolder = location.file.getParentFile()!!
    val memberPath = location(link).file.appendExtension(extension)
    return Location(ownerFolder.getRelativePath(memberPath))
}
