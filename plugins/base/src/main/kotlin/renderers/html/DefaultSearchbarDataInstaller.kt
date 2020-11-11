package org.jetbrains.dokka.base.renderers.html

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import java.util.concurrent.ConcurrentHashMap

open class DefaultSearchbarDataInstaller : SearchbarDataInstaller {
    private val mapper = jacksonObjectMapper()

    private val pageList = ConcurrentHashMap<String, Pair<String, String>>()

    open override fun generatePagesList(): Json {
        val pages = pageList.entries
            .filter { it.key.isNotEmpty() }
            .sortedWith(compareBy({ it.key }, { it.value.first }, { it.value.second }))
            .groupBy { it.key.substringAfterLast(".") }
            .entries
            .flatMap { entry ->
                entry.value.map { subentry ->
                    val name = subentry.value.first
                    createSearchRecord(
                        name = name,
                        description = subentry.key,
                        location = subentry.value.second,
                        searchKeys = listOf(entry.key, name)
                    )
                }
            }
        return mapper.writeValueAsString(pages)
    }

    open override fun createSearchRecord(
        name: String,
        description: String?,
        location: String,
        searchKeys: List<String>
    ): SearchRecord =
        SearchRecord(name, description, location, searchKeys)


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

    open override fun processPage(page: ContentPage, link: String) {
        val signature = getSymbolSignature(page)
        val textNodes = signature?.let { flattenToText(it) }
        val documentable = page.documentable
        if (documentable != null) {
            listOf(
                documentable.dri.packageName,
                documentable.dri.classNames,
                documentable.dri.callable?.name
            ).filter { !it.isNullOrEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(".")
                ?.let { id -> pageList.put(id, Pair(textNodes ?: page.name, link)) }
        }
    }
}