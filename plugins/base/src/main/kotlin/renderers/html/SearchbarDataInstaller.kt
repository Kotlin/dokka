package org.jetbrains.dokka.base.renderers.html

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.templating.AddToSearch
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
    data class PageWithId(val id: PageId, val page: ContentPage) {
        val displayableSignature = getSymbolSignature(page)?.let { flattenToText(it) } ?: page.name
    }

    private val mapper = jacksonObjectMapper()

    open fun generatePagesList(pages: Map<PageId, PageWithId>, locationResolver: PageResolver): List<SearchRecord> =
        pages.entries
            .filter { it.key.isNotEmpty() }
            .sortedWith(compareBy({ it.key }, { it.value.displayableSignature }))
            .groupBy { it.key.substringAfterLast(".") }
            .entries
            .flatMap { entry ->
                entry.value.map { subentry ->
                    createSearchRecord(
                        name = subentry.value.displayableSignature,
                        description = subentry.key,
                        location = resolveLocation(locationResolver, subentry.value.page).orEmpty(),
                        searchKeys = listOf(entry.key, subentry.value.displayableSignature)
                    )
                }
            }

    open fun createSearchRecord(
        name: String,
        description: String?,
        location: String,
        searchKeys: List<String>
    ): SearchRecord =
        SearchRecord(name, description, location, searchKeys)

    open fun processPage(page: PageNode): PageWithId? =
        when (page) {
            is ContentPage -> page.takeIf { page !is ModulePageNode && page !is PackagePageNode }?.documentable
                ?.let { documentable ->
                    listOfNotNull(
                        documentable.dri.packageName,
                        documentable.dri.classNames,
                        documentable.dri.callable?.name
                    ).takeIf { it.isNotEmpty() }?.joinToString(".")
                }?.let { id ->
                    PageWithId(id, page)
                }
            else -> null
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
                val content = input.withDescendants().fold(emptyMap<PageId, PageWithId>()) { pageList, page ->
                    processPage(page)?.let { pageList + Pair(it.id, it) } ?: pageList
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

private fun getSymbolSignature(page: ContentPage) = page.content.dfs { it.dci.kind == ContentKind.Symbol }

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