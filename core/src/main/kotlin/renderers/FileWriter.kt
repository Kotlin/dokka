package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import java.io.IOException
import java.nio.file.Paths

class FileWriter(val context: DokkaContext, override val extension: String): OutputWriter {
    private val createdFiles: MutableSet<String> = mutableSetOf()

    override fun write(path: String, text: String, ext: String) {
        if (createdFiles.contains(path)) {
            context.logger.error("An attempt to write ${context.configuration.outputDir}/$path several times!")
            return
        }
        createdFiles.add(path)

        try {
//            println("Writing $root/$path$ext")
            val dir = Paths.get(context.configuration.outputDir, path.dropLastWhile { it != '/' }).toFile()
            dir.mkdirsOrFail()
            Paths.get(context.configuration.outputDir, "$path$ext").toFile().writeText(text)
        } catch (e : Throwable) {
            context.logger.error("Failed to write $this. ${e.message}")
            e.printStackTrace()
        }
    }

    private fun File.mkdirsOrFail() {
        if (!mkdirs() && !exists()) {
            throw IOException("Failed to create directory $this")
        }
    }
}