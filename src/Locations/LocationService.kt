package org.jetbrains.dokka

import java.io.File

public data class Location(val file: File) {
    public val path : String
        get() = file.path
}

public trait LocationService {
    fun location(node: DocumentationNode): Location
}


public fun escapeUri(path: String): String = path.replace('<', '-').replace('>', '-')

fun LocationService.relativeLocation(owner: DocumentationNode, node: DocumentationNode, extension: String): Location {
    return relativeLocation(location(owner), node, extension)
}

fun LocationService.relativeLocation(owner: Location, node: DocumentationNode, extension: String): Location {
    val ownerFolder = owner.file.getParentFile()!!
    val memberPath = location(node).file.appendExtension(extension)
    return Location(ownerFolder.getRelativePath(memberPath))
}
