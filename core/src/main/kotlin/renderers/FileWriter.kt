package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import java.io.File
import java.io.IOException
import java.nio.file.Paths

class FileWriter(val root: String, val extension: String){
    private val createdFiles: MutableSet<String> = mutableSetOf()

    fun write(path: String, text: String, ext: String = extension){
        if (createdFiles.contains(path)) {
            println("ERROR. An attempt to write $root/$path several times!")
            return
        }
        createdFiles.add(path)

        try {
//            println("Writing $root/$path$ext")
            val dir = Paths.get(root, path.dropLastWhile { it != '/' }).toFile()
            dir.mkdirsOrFail()
            Paths.get(root, "$path$ext").toFile().writeText(text)
        } catch (e : Throwable) {
            println("Failed to write $this. ${e.message}")
            e.printStackTrace()
        }
    }

    private fun File.mkdirsOrFail() {
        if (!mkdirs() && !exists()) {
            throw IOException("Failed to create directory $this")
        }
    }
}