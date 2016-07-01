package org.jetbrains.dokka

import com.google.inject.Inject

open class JekyllOutputBuilder(to: StringBuilder,
                               location: Location,
                               locationService: LocationService,
                               languageService: LanguageService,
                               extension: String)
    : MarkdownOutputBuilder(to, location, locationService, languageService, extension)
{
    override fun appendNodes(nodes: Iterable<DocumentationNode>) {
        to.appendln("---")
        appendFrontMatter(nodes, to)
        to.appendln("---")
        to.appendln("")
        super.appendNodes(nodes)
    }

    protected open fun appendFrontMatter(nodes: Iterable<DocumentationNode>, to: StringBuilder) {
        to.appendln("title: ${getPageTitle(nodes)}")
    }
}


open class JekyllFormatService(locationService: LocationService,
                               signatureGenerator: LanguageService,
                               linkExtension: String)
: MarkdownFormatService(locationService, signatureGenerator, linkExtension) {

    @Inject constructor(locationService: LocationService,
                        signatureGenerator: LanguageService): this(locationService, signatureGenerator, "md") {
    }

    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder =
        JekyllOutputBuilder(to, location, locationService, languageService, extension)

}
