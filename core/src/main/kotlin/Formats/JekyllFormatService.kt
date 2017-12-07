package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName

open class JekyllOutputBuilder(to: StringBuilder,
                               location: Location,
                               generator: NodeLocationAwareGenerator,
                               languageService: LanguageService,
                               extension: String,
                               impliedPlatforms: List<String>)
    : MarkdownOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms) {
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


open class JekyllFormatService(
        generator: NodeLocationAwareGenerator,
        signatureGenerator: LanguageService,
        linkExtension: String,
        impliedPlatforms: List<String>
) : MarkdownFormatService(generator, signatureGenerator, linkExtension, impliedPlatforms) {

    @Inject constructor(
            generator: NodeLocationAwareGenerator,
            signatureGenerator: LanguageService,
            @Named(impliedPlatformsName) impliedPlatforms: List<String>
    ) : this(generator, signatureGenerator, "html", impliedPlatforms)

    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder =
            JekyllOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms)

}
