package org.jetbrains.dokka

import java.io.File

data class Location(val file: File)

public trait LocationService {
    fun location(node: DocumentationNode): Location
}


public fun escapeUri(path: String): String = path.replace('<', '_').replace('>', '_')

fun LocationService.relativeLocation(node: DocumentationNode, link: DocumentationNode, extension: String): File {
    val ownerFolder = location(node).file.getParentFile()!!
    val memberPath = location(link).file.appendExtension(extension)
    return ownerFolder.getRelativePath(memberPath)
}
