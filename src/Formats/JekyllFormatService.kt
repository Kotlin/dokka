package org.jetbrains.dokka

public open class JekyllFormatService(locationService: LocationService,
                                 signatureGenerator: LanguageService)
: MarkdownFormatService(locationService, signatureGenerator) {

    override fun link(from: DocumentationNode, to: DocumentationNode): FormatLink = link(from, to, "html")

    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        to.appendln("---")
        to.appendln("layout: api")
        to.appendln("title: ${nodes.first().name}")
        to.appendln("---")
        super<MarkdownFormatService>.appendNodes(location, to, nodes)
    }
}