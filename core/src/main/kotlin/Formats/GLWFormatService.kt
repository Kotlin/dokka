package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName

/**
 * Special provider for GitLab-Wiki
 * it is for now fully compatible with Github MD except that link extensions should be empty
 */
open class GLWFormatService (
    generator: NodeLocationAwareGenerator,
    signatureGenerator: LanguageService,
    linkExtension: String,
    impliedPlatforms: List<String>
): MarkdownFormatService(generator, signatureGenerator, linkExtension, impliedPlatforms){
    @Inject
    constructor(
        generator: NodeLocationAwareGenerator,
        signatureGenerator: LanguageService,
        @Named(impliedPlatformsName) impliedPlatforms: List<String>
    ) : this(generator, signatureGenerator, "md", impliedPlatforms)

    override fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder =
        GFMOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms, true)
}