package org.jetbrains.dokka

import java.io.*

fun File.getRelativePath(name: File): File {
    val parent = getParentFile()

    if (parent == null)
        throw IOException("No common directory");

    val basePath = getCanonicalPath() + File.separator;
    val targetPath = name.getCanonicalPath();

    if (targetPath.startsWith(basePath)) {
        return File(targetPath.substring(basePath.length()))
    } else {
        return File(".." + File.separator + parent.getRelativePath(name))
    }
}

fun File.appendExtension(extension: String) = if (extension.isEmpty()) this else File(getPath() + "." + extension)
