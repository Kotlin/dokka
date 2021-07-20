package org.jetbrains.dokka.sitemap.transformers.pages

import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.sitemap.SitemapConfiguration
import org.jetbrains.dokka.sitemap.SitemapPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

/**
 * TODO: Solutions for ordering problem:
 * 1) Adding a post processing to dokka that would not modify pages tree by convention.
 *    This will be useful for webhelp if they provide a jar or even for jekyll to compile sources
 * 2) Adding a separate stage after renderer specific processors
 * 3) Add `doLast` to plugins or adding a Noop Processor that would be hard-coded to be last and if you want to have your thing at the end you write that it is before Noop
 * 4) Force Page transformers to declare what they do via a type, so that we can ensure that we can run something after all content pages are in place
 *
 * Specification: [sitemap docs](https://developers.google.com/search/docs/advanced/sitemaps/build-sitemap?hl=en&visit_id=637613321436192601-2699040246&rd=1#text)
 */
class SitemapPageTransformer(private val context: DokkaContext) : PageTransformer {
    private val maybeConfiguration = configuration<SitemapPlugin, SitemapConfiguration>(context)

    override fun invoke(input: RootPageNode): RootPageNode {
        val baseUrl: String = maybeConfiguration?.baseUrl ?: run {
            context.logger.warn(
                "Failed to find configured value for baseUrl. Sitemap plugin will generate only relative paths from root page, " +
                        "that need to be appended manually to site's url"
            )
            ""
        }
        val page = RendererSpecificResourcePage(
            name = fileName,
            children = emptyList(),
            strategy = RenderingStrategy.DriLocationResolvableWrite { resolver ->
                createTextSitemap(input, resolver, baseUrl).joinToString(separator = "\n")
                    .prefixedWithModuleNameInMultiModule()
            })

        return input.modified(children = input.children + page)
    }

    private fun createTextSitemap(
        rootPageNode: RootPageNode,
        locationResolver: DriResolver,
        baseUrl: String,
    ): Set<String> {
        val moduleTemplate = templateSymbol.takeIf { context.configuration.delayTemplateSubstitution }
        val baseUrlWithoutSlash = baseUrl.removeSuffix("/").takeIf { it.isNotEmpty() }
        return rootPageNode.withDescendants().fold(mutableSetOf()) { siteMap, page ->
            (page as? ContentPage)?.let {
                listOfNotNull(
                    baseUrlWithoutSlash,
                    moduleTemplate,
                    locationResolver(it.dri.first(), it.content.sourceSets),
                ).joinToString("/")
            }?.let { resolvedLocation -> siteMap.add(resolvedLocation) }
            siteMap
        }
    }

    private val fileName: String
        get() =
            if (context.configuration.delayTemplateSubstitution) SitemapConfiguration.defaultRelativeOutputLocation
            else maybeConfiguration?.relativeOutputLocation ?: SitemapConfiguration.defaultRelativeOutputLocation

    private fun String.prefixedWithModuleNameInMultiModule(): String =
        if (context.configuration.delayTemplateSubstitution) "${context.configuration.moduleName}\n$this"
        else this

    companion object {
        val templateSymbol = "###"
    }
}