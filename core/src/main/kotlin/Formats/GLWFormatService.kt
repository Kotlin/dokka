package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName



/**
 * Formatter for GitLab Markdown #403
 * based on GitHub markdown
 */
open class GLMOutputBuilder(
    to: StringBuilder,
    location: Location,
    generator: NodeLocationAwareGenerator,
    languageService: LanguageService,
    extension: String,
    impliedPlatforms: List<String>,
    /**
     * GitLab has two markdown usage - core and wiki
     * in wikies where is alternate *.md links processing
     */
    val wikiMode: Boolean = false
) : GFMOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms){


    override fun normalizeHref(href: String): String {
        fun isExternalHref(href:String) = href.contains(":/")
        var result = href
        /**
         * in wiki mode we should adapt non global links to othe markdown files from
         * form `xxxx.md` to form `./xxxx`
         */
        if(wikiMode && !isExternalHref(href)){
            result = href.replace("""(\.$extension*)?$""".toRegex(),"")
            if(!result.startsWith("./")&& !result.startsWith("../")){
                result = "./$result"
            }
        }
        return result
    }
}

/**
 * Special provider for GitLab Markdown (non-wiki-mode)
 */
open class GLMFormatService (
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
        GLMOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms, false)
}

/**
 * Special provider for GitLab Markdown (wiki-mode)
 * it is for now fully compatible with Github MD except that link extensions should be empty
 */
open class GLMWFormatService (
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
        GLMOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms, true)
}