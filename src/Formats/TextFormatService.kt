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

                for ((label,section) in node.doc.sections) {
                    appendln(label)
                }
            }
        }
    }

    override fun appendOutline(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
    }
}