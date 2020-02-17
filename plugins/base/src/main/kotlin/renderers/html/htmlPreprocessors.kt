package org.jetbrains.dokka.renderers.html

import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.table
import kotlinx.html.tbody
import org.jetbrains.dokka.base.renderers.platforms
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.pages.PageNodeTransformer

object RootCreator : PageNodeTransformer {
    override fun invoke(input: RootPageNode) =
        RendererSpecificRootPage("", listOf(input), RenderingStrategy.DoNothing)
}

object SearchPageInstaller : PageNodeTransformer {
    override fun invoke(input: RootPageNode) = input.modified(children = input.children + searchPage)

    private val searchPage = RendererSpecificResourcePage(
        name = "Search",
        children = emptyList(),
        strategy = RenderingStrategy<HtmlRenderer> {
            buildHtml(it, listOf("styles/style.css", "scripts/pages.js")) {
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

object NavigationPageInstaller : PageNodeTransformer {
    override fun invoke(input: RootPageNode) = input.modified(
        children = input.children + NavigationPage(
            input.children.filterIsInstance<ContentPage>().single().let(::visit)
        )
    )

    private fun visit(page: ContentPage): NavigationNode = NavigationNode(
        page.name,
        page.dri.first(),
        page.platforms(),
        page.children.filterIsInstance<ContentPage>().map { visit(it) })
}

object ResourceInstaller : PageNodeTransformer {
    override fun invoke(input: RootPageNode) = input.modified(children = input.children + resourcePages)

    private val resourcePages = listOf("styles", "scripts", "images").map {
        RendererSpecificResourcePage(it, emptyList(), RenderingStrategy.Copy("/dokka/$it"))
    }
}

object StyleAndScriptsAppender : PageNodeTransformer {
    override fun invoke(input: RootPageNode) = input.transformContentPagesTree {
        it.modified(
            embeddedResources = it.embeddedResources + listOf(
                "styles/style.css",
                "scripts/navigationLoader.js"
            )
        )
    }
}
