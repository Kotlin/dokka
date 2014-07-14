package org.jetbrains.dokka

import java.io.*

fun File.getRelativePath(name: File): File {
    val parent = getParentFile()

    if (parent == null)
        throw IOException("No common directory");

    val basePath = getCanonicalPath();
    val targetPath = name.getCanonicalPath();

    if (targetPath.startsWith(basePath)) {
        return File(targetPath.substring(basePath.length() + 1))
    } else {
        return File(".." + File.separator + parent.getRelativePath(name))
    }
}

fun File.appendExtension(extension: String) = File(getPath() + "." + extension)
