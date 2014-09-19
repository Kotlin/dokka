package org.jetbrains.dokka

import java.io.FileOutputStream
import java.io.OutputStreamWriter

public class FileGenerator(val signatureGenerator: LanguageService,
                           val locationService: LocationService,
                           val formatService: FormatService) {

    public fun buildPage(node: DocumentationNode): Unit = buildPages(listOf(node))
    public fun buildOutline(node: DocumentationNode): Unit = buildOutlines(listOf(node))

    public fun buildPages(nodes: Iterable<DocumentationNode>) {
        for ((location, items) in nodes.groupBy { locationService.location(it) }) {
            val file = location.file.appendExtension(formatService.extension)
            file.getParentFile()?.mkdirs()
            FileOutputStream(file).use {
                OutputStreamWriter(it, defaultCharset).use {
                    it.write(formatService.format(items))
                }
            }
            buildPages(items.flatMap { it.members })
        }
    }

    public fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        for ((location, items) in nodes.groupBy { locationService.location(it) }) {
            val file = location.file.appendExtension("yml") // TODO: hardcoded
            file.getParentFile()?.mkdirs()
            FileOutputStream(file).use {
                OutputStreamWriter(it, defaultCharset).use {
                    it.write(formatService.formatOutline(items))
                }
            }
        }
    }
}