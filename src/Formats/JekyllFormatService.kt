package org.jetbrains.dokka

public class JekyllFormatService(locationService: LocationService, signatureGenerator: SignatureGenerator)
: MarkdownFormatService(locationService, signatureGenerator) {

    override fun link(from: DocumentationNode, to: DocumentationNode): FormatLink = link(from, to, "html")

    override fun appendNodes(to: StringBuilder,
                        nodes: Iterable<DocumentationNode>) {
        to.appendln("---")
        to.appendln("layout: api")
        to.appendln("title: ${nodes.first().name}")
        to.appendln("---")
        super<MarkdownFormatService>.appendNodes(to, nodes)
    }
}