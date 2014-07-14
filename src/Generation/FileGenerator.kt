package org.jetbrains.dokka

import java.io.File

public class FileGenerator(val signatureGenerator: SignatureGenerator,
                           val locationService: LocationService,
                           val formatService: FormatService) {

    public fun generate(node: DocumentationNode) {
        val location = locationService.location(node)
        val file = location.file.appendExtension(formatService.extension)
        file.getParentFile()?.mkdirs()
        file.writeText(formatService.format(node), defaultCharset)
        val members = node.members.sortBy { it.name }
        for (member in members)
            generate(member)
    }
}