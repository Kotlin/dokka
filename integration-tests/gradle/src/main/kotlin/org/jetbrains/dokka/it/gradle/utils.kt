package org.jetbrains.dokka.it.gradle

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

fun Path.copy(to: Path) {
    Files.walkFileTree(this, CopyFileVisitor(this, to))
}

class CopyFileVisitor(private var sourcePath: Path, private val targetPath: Path) : SimpleFileVisitor<Path>() {
    @Throws(IOException::class)
    override fun preVisitDirectory(
        dir: Path,
        attrs: BasicFileAttributes
    ): FileVisitResult {
        Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)))
        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun visitFile(
        file: Path,
        attrs: BasicFileAttributes
    ): FileVisitResult {
        Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING)
        return FileVisitResult.CONTINUE
    }
}
