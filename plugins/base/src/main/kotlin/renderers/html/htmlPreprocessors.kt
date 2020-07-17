package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.table
import kotlinx.html.tbody
import org.jetbrains.dokka.base.renderers.sourceSets
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
    override fun invoke(input: RootPageNode) = input.modified(
        children = input.children + NavigationPage(
            input.children.filterIsInstance<ContentPage>().single()
                .let(NavigationPageInstaller::visit)
        )
    )

    private fun visit(page: ContentPage): NavigationNode =
        NavigationNode(
            page.name,
            page.dri.first(),
            page.sourceSets(),
            page.navigableChildren()
        )

    private fun ContentPage.navigableChildren(): List<NavigationNode> {
        if (this !is ClasslikePageNode) {
            return children.filterIsInstance<ContentPage>()
                .map { visit(it) }
        } else if (documentable is DEnum) {
            return children.filter { it is ContentPage && it.documentable is DEnumEntry }
                .map { visit(it as ContentPage) }
        }

        return emptyList()
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
