package org.jetbrains.dokka.base.renderers.html

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.table
import kotlinx.html.tbody
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer


object SearchPageInstaller : PageTransformer {
    override fun invoke(input: RootPageNode) = input.modified(children = input.children + searchPage)

    private val searchPage = RendererSpecificResourcePage(
        name = "Search",
        children = emptyList(),
        strategy = RenderingStrategy<HtmlRenderer> {
            buildHtml(it, listOf("styles/style.css", "scripts/pages.js", "scripts/search.js")) {
                h1 {
                    id = "searchTitle"
                    text("Search results for ")
                }
                table {
                    tbody {
                        id = "searchTable"
                    }
                }
            }
        })
}

object NavigationPageInstaller : PageTransformer {
    private val mapper = jacksonObjectMapper()

    private data class NavigationNodeView(
        val name: String,
        val label: String = name,
        val searchKey: String = name,
        @get:JsonSerialize(using = ToStringSerializer::class) val dri: DRI,
        val location: String
    ) {
        companion object {
            fun from(node: NavigationNode, location: String): NavigationNodeView =
                NavigationNodeView(name = node.name, dri = node.dri, location = location)
        }
    }

    override fun invoke(input: RootPageNode): RootPageNode {
        val nodes = input.children.filterIsInstance<ContentPage>().single()
            .let(NavigationPageInstaller::visit)

        val page = RendererSpecificResourcePage(
            name = "scripts/navigation-pane.json",
            children = emptyList(),
            strategy = RenderingStrategy.LocationResolvableWrite { resolver ->
                val flattened = flattenNavigationNodes(listOf(nodes))
                val view = flattened.map { NavigationNodeView.from(it, resolver(it.dri, it.sourceSets)) }
                mapper.writeValueAsString(view)
            })

        return input.modified(
            children = input.children + page + NavigationPage(nodes)
        )
    }

    private fun visit(page: ContentPage): NavigationNode =
        NavigationNode(
            name = page.name,
            dri = page.dri.first(),
            sourceSets = page.sourceSets(),
            children = page.navigableChildren()
        )

    private fun ContentPage.navigableChildren(): List<NavigationNode> =
        when {
            this !is ClasslikePageNode ->
                children.filterIsInstance<ContentPage>().map { visit(it) }
            documentable is DEnum ->
                children.filter { it is ContentPage && it.documentable is DEnumEntry }.map { visit(it as ContentPage) }
            else -> emptyList()
        }.sortedBy { it.name.toLowerCase() }

    private tailrec fun flattenNavigationNodes(nodes: List<NavigationNode>, acc: List<NavigationNode> = emptyList()): List<NavigationNode> {
        if(nodes.isEmpty()) return acc
        return flattenNavigationNodes(nodes.flatMap { it.children }, nodes.map { it.copy(children = emptyList()) } + acc)
    }
}

object ResourceInstaller : PageTransformer {
    override fun invoke(input: RootPageNode) = input.modified(children = input.children + resourcePages)

    private val resourcePages = listOf("styles", "scripts", "images").map {
        RendererSpecificResourcePage(it, emptyList(), RenderingStrategy.Copy("/dokka/$it"))
    }
}

object StyleAndScriptsAppender : PageTransformer {
    override fun invoke(input: RootPageNode) = input.transformContentPagesTree {
        it.modified(
            embeddedResources = it.embeddedResources + listOf(
                "styles/style.css",
                "scripts/navigationLoader.js",
                "scripts/platformContentHandler.js",
                "scripts/sourceset_dependencies.js",
                "scripts/clipboard.js",
                "styles/jetbrains-mono.css"
            )
        )
    }
}

class SourcesetDependencyAppender(val context: DokkaContext) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {
        val dependenciesMap = context.configuration.sourceSets.map {
            it.sourceSetID to it.dependentSourceSets
        }.toMap()

        fun createDependenciesJson(): String = "sourceset_dependencies = '{${
        dependenciesMap.entries.joinToString(", ") {
            "\"${it.key}\": [${it.value.joinToString(",") {
                "\"$it\""
            }}]"
        }
        }}'"

        val deps = RendererSpecificResourcePage(
            name = "scripts/sourceset_dependencies.js",
            children = emptyList(),
            strategy = RenderingStrategy.Write(createDependenciesJson())
        )

        return input.modified(
            children = input.children + deps
        )
    }
}


