package org.jetbrains.dokka

import com.google.inject.Inject

open class JekyllFormatService
    @Inject constructor(locationService: LocationService,
                        signatureGenerator: LanguageService,
                        linkExtension: String = "md")
: MarkdownFormatService(locationService, signatureGenerator, linkExtension) {

    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        to.appendln("---")
        appendFrontMatter(nodes, to)
        to.appendln("---")
        to.appendln("")
        super.appendNodes(location, to, nodes)
    }

    protected open fun appendFrontMatter(nodes: Iterable<DocumentationNode>, to: StringBuilder) {
        to.appendln("title: ${getPageTitle(nodes)}")
    }
}