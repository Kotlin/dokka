package org.jetbrains.dokka

import java.io.*

fun File.getRelativePath(name: File): File {
    val parent = parentFile

    if (parent == null)
        throw IOException("No common directory");

    val basePath = canonicalPath + File.separator;
    val targetPath = name.canonicalPath;

    if (targetPath.startsWith(basePath)) {
        return File(targetPath.substring(basePath.length()))
    } else {
        return File(".." + File.separator + parent.getRelativePath(name))
    }
}

fun File.appendExtension(extension: String) = if (extension.isEmpty()) this else File(path + "." + extension)
