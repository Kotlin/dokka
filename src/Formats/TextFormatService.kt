package org.jetbrains.dokka

public class TextFormatService(val signatureGenerator: LanguageService) : FormatService {
    override val extension: String = "txt"
    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        for (node in nodes) {
            with (to) {
                appendln(signatureGenerator.render(node))
                appendln()
                appendln(node.content.summary)

                for ((label, section) in node.content.sections) {
                    appendln(label)
                }
            }
        }
    }
}