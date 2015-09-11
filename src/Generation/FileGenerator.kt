package org.jetbrains.dokka

import java.io.FileOutputStream
import java.io.OutputStreamWriter

public class FileGenerator(val signatureGenerator: LanguageService,
                           val locationService: FileLocationService,
                           val formatService: FormatService,
                           val outlineService: OutlineFormatService?) {

    public fun buildPage(node: DocumentationNode): Unit = buildPages(listOf(node))
    public fun buildOutline(node: DocumentationNode): Unit = buildOutlines(listOf(node))

    public fun buildPages(nodes: Iterable<DocumentationNode>) {
        for ((location, items) in nodes.groupBy { locationService.location(it) }) {
            val file = location.file
            file.parentFile?.mkdirs()
            FileOutputStream(file).use {
                OutputStreamWriter(it, Charsets.UTF_8).use {
                    it.write(formatService.format(location, items))
                }
            }
            buildPages(items.flatMap { it.members })
        }
    }

    public fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        if (outlineService == null) {
            return
        }
        for ((location, items) in nodes.groupBy { locationService.location(it) }) {
            val file = outlineService.getOutlineFileName(location)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use {
                OutputStreamWriter(it, Charsets.UTF_8).use {
                    it.write(outlineService.formatOutline(location, items))
                }
            }
        }
    }
}