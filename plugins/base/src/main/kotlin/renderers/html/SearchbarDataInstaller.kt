package org.jetbrains.dokka.base.renderers.html

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.templating.AddToSearch
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

typealias PageId = String

data class SearchRecord(
    val name: String,
    val description: String? = null,
    val location: String,
    val searchKeys: List<String> = listOf(name)
) {
    companion object {}
}

open class SearchbarDataInstaller(val context: DokkaContext) : PageTransformer {
    data class PageWithId(val dri: DRI, val page: ContentPage) {
        val displayableSignature = getSymbolSignature(page, dri)?.let { flattenToText(it) } ?: page.name
        val id: String
            get() = listOfNotNull(
                dri.packageName,
                dri.classNames,
                dri.callable?.name
            ).joinToString(".")
    }

    private val mapper = jacksonObjectMapper()

    open fun generatePagesList(pages: List<PageWithId>, locationResolver: PageResolver): List<SearchRecord> =
        pages.map { pageWithId ->
            createSearchRecord(
                name = pageWithId.displayableSignature,
                description = pageWithId.id,
                location = resolveLocation(locationResolver, pageWithId.page).orEmpty(),
                searchKeys = listOf(
                    pageWithId.id.substringAfterLast("."),
                    pageWithId.displayableSignature,
                    pageWithId.id,
                )
            )
        }.sortedWith(compareBy({ it.name }, { it.description }))

    open fun createSearchRecord(
        name: String,
        description: String?,
        location: String,
        searchKeys: List<String>
    ): SearchRecord =
        SearchRecord(name, description, location, searchKeys)

    open fun processPage(page: PageNode): List<PageWithId> =
        when (page) {
            is ContentPage -> page.takeIf { page !is ModulePageNode && page !is PackagePageNode }?.dri
                ?.map { dri -> PageWithId(dri, page) }.orEmpty()
            else -> emptyList()
        }

    private fun resolveLocation(locationResolver: PageResolver, page: ContentPage): String? =
        locationResolver(page, null).also { location ->
            if (location.isNullOrBlank()) context.logger.warn("Cannot resolve path for ${page.dri}")
        }

    override fun invoke(input: RootPageNode): RootPageNode {
        val page = RendererSpecificResourcePage(
            name = "scripts/pages.json",
            children = emptyList(),
            strategy = RenderingStrategy.PageLocationResolvableWrite { resolver ->
                val content = input.withDescendants().fold(emptyList<PageWithId>()) { pageList, page ->
                    pageList + processPage(page)
                }.run {
                    generatePagesList(this, resolver)
                }

                if (context.configuration.delayTemplateSubstitution) {
                    mapper.writeValueAsString(AddToSearch(context.configuration.moduleName, content))
                } else {
                    mapper.writeValueAsString(content)
                }
            })

        return input.modified(children = input.children + page)
    }
}

private fun getSymbolSignature(page: ContentPage, dri: DRI) =
    page.content.dfs { it.dci.kind == ContentKind.Symbol && it.dci.dri.contains(dri) }

private fun flattenToText(node: ContentNode): String {
    fun getContentTextNodes(node: ContentNode, sourceSetRestriction: DisplaySourceSet): List<ContentText> =
        when (node) {
            is ContentText -> listOf(node)
            is ContentComposite -> node.children
                .filter { sourceSetRestriction in it.sourceSets }
                .flatMap { getContentTextNodes(it, sourceSetRestriction) }
                .takeIf { node.dci.kind != ContentKind.Annotations }
                .orEmpty()
            else -> emptyList()
        }

    val sourceSetRestriction =
        node.sourceSets.find { it.platform == Platform.common } ?: node.sourceSets.first()
    return getContentTextNodes(node, sourceSetRestriction).joinToString("") { it.text }
}