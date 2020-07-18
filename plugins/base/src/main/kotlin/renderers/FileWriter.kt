package org.jetbrains.dokka.base.renderers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.*

class FileWriter(val context: DokkaContext): OutputWriter {
    private val createdFiles: MutableSet<String> = mutableSetOf()
    private val jarUriPrefix = "jar:file:"
    private val root = context.configuration.outputDir

    override suspend fun write(path: String, text: String, ext: String) {
        if (createdFiles.contains(path)) {
            context.logger.error("An attempt to write ${root}/$path several times!")
            return
        }
        createdFiles.add(path)

        try {
            val dir = Paths.get(root.absolutePath, path.dropLastWhile { it != '/' }).toFile()
            withContext(Dispatchers.IO) {
                dir.mkdirsOrFail()
                Files.write(Paths.get(root.absolutePath, "$path$ext"), text.lines())
            }
        } catch (e: Throwable) {
            context.logger.error("Failed to write $this. ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun writeResources(pathFrom: String, pathTo: String) =
        if (javaClass.getResource(pathFrom).toURI().toString().startsWith(jarUriPrefix)) {
            copyFromJar(pathFrom, pathTo)
        } else {
            copyFromDirectory(pathFrom, pathTo)
        }


    private suspend fun copyFromDirectory(pathFrom: String, pathTo: String) {
        val dest = Paths.get(root.path, pathTo).toFile()
        val uri = javaClass.getResource(pathFrom).toURI()
        withContext(Dispatchers.IO) {
            File(uri).copyRecursively(dest, true)
        }
    }

    private suspend fun copyFromJar(pathFrom: String, pathTo: String) {
        val rebase = fun(path: String) =
            "$pathTo/${path.removePrefix(pathFrom)}"
        val dest = Paths.get(root.path, pathTo).toFile()
        dest.mkdirsOrFail()
        val uri = javaClass.getResource(pathFrom).toURI()
        val fs = getFileSystemForURI(uri)
        val path = fs.getPath(pathFrom)
        for (file in Files.walk(path).iterator()) {
            if (Files.isDirectory(file)) {
                val dirPath = file.toAbsolutePath().toString()
                withContext(Dispatchers.IO) {
                    Paths.get(root.path, rebase(dirPath)).toFile().mkdirsOrFail()
                }
            } else {
                val filePath = file.toAbsolutePath().toString()
                withContext(Dispatchers.IO) {
                    Paths.get(root.path, rebase(filePath)).toFile().writeBytes(
                        this@FileWriter.javaClass.getResourceAsStream(filePath).readBytes()
                    )
                }
            }
        }
    }

    private fun File.mkdirsOrFail() {
        if (!mkdirs() && !exists()) {
            throw IOException("Failed to create directory $this")
        }
    }

    private fun getFileSystemForURI(uri: URI): FileSystem =
        try {
            FileSystems.newFileSystem(uri, emptyMap<String, Any>())
        } catch (e: FileSystemAlreadyExistsException) {
            FileSystems.getFileSystem(uri)
        }
}
