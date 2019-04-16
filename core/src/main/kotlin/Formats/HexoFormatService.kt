package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.impliedPlatformsName

open class HexoOutputBuilder(
    to: StringBuilder,
    location: Location,
    generator: NodeLocationAwareGenerator,
    languageService: LanguageService,
    extension: String,
    impliedPlatforms: List<String>
) : GFMOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms) {

    override fun appendNodes(nodes: Iterable<DocumentationNode>) {
        to.appendln("---")
        appendFrontMatter(nodes, to)
        to.appendln("---")
        to.appendln("")
        super.appendNodes(nodes)
    }

    protected open fun appendFrontMatter(nodes: Iterable<DocumentationNode>, to: StringBuilder) {
        to.appendln("title: ${getPageTitle(nodes)}")
        to.appendln("layout: api")
    }
}

open class HexoFormatService(
    generator: NodeLocationAwareGenerator,
    signatureGenerator: LanguageService,
    linkExtension: String,
    impliedPlatforms: List<String>
) : GFMFormatService(generator, signatureGenerator, linkExtension, impliedPlatforms) {

    @Inject
    constructor(
        generator: NodeLocationAwareGenerator,
        signatureGenerator: LanguageService,
        @Named(impliedPlatformsName) impliedPlatforms: List<String>
    ) : this(generator, signatureGenerator, "html", impliedPlatforms)

    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder =
        HexoOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms)
}