package org.jetbrains.dokka

import com.google.inject.Inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

public class FileGenerator @Inject constructor(val locationService: FileLocationService,
                           val formatService: FormatService) : Generator {

    @set:Inject(optional = true) var outlineService: OutlineFormatService? = null

    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val specificLocationService = locationService.withExtension(formatService.extension)

        for ((location, items) in nodes.groupBy { specificLocationService.location(it) }) {
            val file = location.file
            file.parentFile?.mkdirsOrFail()
            try {
                FileOutputStream(file).use {
                    OutputStreamWriter(it, Charsets.UTF_8).use {
                        it.write(formatService.format(location, items))
                    }
                }
            } catch (e: Throwable) {
                println(e)
            }
            buildPages(items.flatMap { it.members })
        }
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        val outlineService = this.outlineService ?: return
        for ((location, items) in nodes.groupBy { locationService.location(it) }) {
            val file = outlineService.getOutlineFileName(location)
            file.parentFile?.mkdirsOrFail()
            FileOutputStream(file).use {
                OutputStreamWriter(it, Charsets.UTF_8).use {
                    it.write(outlineService.formatOutline(location, items))
                }
            }
        }
    }
}

private fun File.mkdirsOrFail() {
    if (!mkdirs() && !exists()) {
        throw IOException("Failed to create directory $this")
    }
}