package org.jetbrains.dokka

import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.LinkedHashMap

public class FileGenerator(val signatureGenerator: SignatureGenerator,
                           val locationService: LocationService,
                           val formatService: FormatService) {

    public fun generate(node: DocumentationNode): Unit = generate(listOf(node))

    public fun generate(nodes: Iterable<DocumentationNode>) {
        for ((location, items) in nodes.groupByTo(LinkedHashMap()) { locationService.location(it) }) {
            val file = location.file.appendExtension(formatService.extension)
            file.getParentFile()?.mkdirs()
            FileOutputStream(file).use {
                OutputStreamWriter(it, defaultCharset).use {
                    it.write(formatService.format(items))
                }
            }
            generate(items.flatMap { it.members })
        }
    }
}