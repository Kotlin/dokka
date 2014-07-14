package org.jetbrains.dokka

public class TextFormatService(val signatureGenerator: SignatureGenerator) : FormatService {
    override val extension: String = "txt"
    override fun format(nodes: Iterable<DocumentationNode>, to: StringBuilder) {
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
}