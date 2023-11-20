/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.templating.AddToSearch
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

public data class SearchRecord(
    val name: String,
    val description: String? = null,
    val location: String,
    val searchKeys: List<String> = listOf(name)
) {
    public companion object
}

public open class SearchbarDataInstaller(
    public val context: DokkaContext
) : PageTransformer {

    public data class DRIWithSourceSets(val dri: DRI, val sourceSet: Set<DisplaySourceSet>)

    public data class SignatureWithId(val driWithSourceSets: DRIWithSourceSets, val displayableSignature: String) {
        public constructor(dri: DRI, page: ContentPage) : this( DRIWithSourceSets(dri, page.sourceSets()),
            getSymbolSignature(page, dri)?.let { flattenToText(it) } ?: page.name)

        val id: String
            get() = with(driWithSourceSets.dri) {
                listOfNotNull(
                    packageName?.takeIf { it.isNotBlank() },
                    classNames,
                    callable?.name
                ).joinToString(".")
            }
    }

    private val mapper = jacksonObjectMapper()

    public open fun generatePagesList(
        pages: List<SignatureWithId>,
        locationResolver: DriResolver
    ): List<SearchRecord> =
        pages.map { pageWithId ->
            createSearchRecord(
                name = pageWithId.displayableSignature,
                description = pageWithId.id,
                location = resolveLocation(locationResolver, pageWithId.driWithSourceSets).orEmpty(),
                searchKeys = listOf(
                    pageWithId.id.substringAfterLast("."),
                    pageWithId.displayableSignature,
                    pageWithId.id,
                )
            )
        }.sortedWith(compareBy({ it.name }, { it.description }))

    public open fun createSearchRecord(
        name: String,
        description: String?,
        location: String,
        searchKeys: List<String>
    ): SearchRecord =
        SearchRecord(name, description, location, searchKeys)

    public open fun processPage(page: PageNode): List<SignatureWithId> =
        when (page) {
            is ContentPage -> page.takeIf { page !is ModulePageNode && page !is PackagePageNode }?.dri
                ?.map { dri -> SignatureWithId(dri, page) }.orEmpty()
            else -> emptyList()
        }

    private fun resolveLocation(locationResolver: DriResolver, driWithSourceSets: DRIWithSourceSets): String? =
        locationResolver(driWithSourceSets.dri, driWithSourceSets.sourceSet).also { location ->
            if (location.isNullOrBlank()) context.logger.warn("Cannot resolve path for ${driWithSourceSets.dri}")
        }

    override fun invoke(input: RootPageNode): RootPageNode {
        val signatureWithIds = input.withDescendants().fold(emptyList<SignatureWithId>()) { pageList, page ->
            pageList + processPage(page)
        }
        val page = RendererSpecificResourcePage(
            name = "scripts/pages.json",
            children = emptyList(),
            strategy = RenderingStrategy.DriLocationResolvableWrite { resolver ->
                val content = signatureWithIds.run {
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
                .takeIf { node.dci.kind != ContentKind.Annotations && node.dci.kind != ContentKind.Source }
                .orEmpty()
            else -> emptyList()
        }

    val sourceSetRestriction =
        node.sourceSets.find { it.platform == Platform.common } ?: node.sourceSets.first()
    return getContentTextNodes(node, sourceSetRestriction).joinToString("") { it.text }
}
