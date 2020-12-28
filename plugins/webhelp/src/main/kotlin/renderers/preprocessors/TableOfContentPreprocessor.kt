package org.jetbrains.dokka.webhelp.renderers.preprocessors

import kotlinx.html.FlowContent
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.html.NavigationDataProvider
import org.jetbrains.dokka.base.renderers.html.NavigationNode
import org.jetbrains.dokka.pages.DriResolver
import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.webhelp.renderers.tags.productProfile
import org.jetbrains.dokka.webhelp.renderers.tags.tocElement

class TableOfContentPreprocessor : PageTransformer, NavigationDataProvider() {
    private fun FlowContent.renderChildren(navigationNode: NavigationNode, resolver: DriResolver) {
        navigationNode.children.forEach { child ->
            tocElement(id = resolver(child.dri, child.sourceSets)!!) {
                renderChildren(child, resolver)
            }
        }
    }

    override fun invoke(input: RootPageNode): RootPageNode {
        val page = RendererSpecificResourcePage(
            name = "t.tree",
            children = emptyList(),
            strategy = RenderingStrategy.DriLocationResolvableWrite { resolver ->
                val root = navigableChildren(input)
                createHTML().productProfile(
                    id = "t",
                    name = root.name,
                    startPage = resolver(root.dri, root.sourceSets)!!
                ) {
                    renderChildren(root, resolver)
                }.let { content ->
                    """<?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE product-profile
                            SYSTEM "https://resources.jetbrains.com/stardust/product-profile.dtd">
                        $content
                    """.trimIndent()
                }
            }
        )

        return input.modified(children = input.children + page)
    }
}