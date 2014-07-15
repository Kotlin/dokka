package org.jetbrains.dokka

public class TextFormatService(val signatureGenerator: LanguageService) : FormatService {
    override val extension: String = "txt"
    override fun appendNodes(to: StringBuilder,
                        nodes: Iterable<DocumentationNode>) {
        for (node in nodes) {
            with (to) {
                appendln(signatureGenerator.render(node))
                appendln()
                appendln(node.doc.summary)
                for (n in node.doc.summary.indices)
                    append("=")

                for (section in node.doc.sections) {
                    appendln(section.label)
                    appendln(section.text)
                }
            }
        }
    }

    override fun appendOutline(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
    }
}