package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName

open class JekyllOutputBuilder(to: StringBuilder,
                               location: Location,
                               locationService: LocationService,
                               languageService: LanguageService,
                               extension: String,
                               impliedPlatforms: List<String>)
    : MarkdownOutputBuilder(to, location, locationService, languageService, extension, impliedPlatforms)
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

    override fun appendLink(href: String, body: () -> Unit) {
        super.appendLink(href.removeSuffix(".md") + "/", body)
    }
}


open class JekyllFormatService(locationService: LocationService,
                               signatureGenerator: LanguageService,
                               linkExtension: String,
                               impliedPlatforms: List<String>)
: MarkdownFormatService(locationService, signatureGenerator, linkExtension, impliedPlatforms) {

    @Inject constructor(locationService: LocationService,
                        signatureGenerator: LanguageService,
                        @Named(impliedPlatformsName) impliedPlatforms: List<String>): this(locationService, signatureGenerator, "html", impliedPlatforms)

    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder =
        JekyllOutputBuilder(to, location, locationService, languageService, extension, impliedPlatforms)

}
