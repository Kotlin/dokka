package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RendererSpecificPage
import org.jetbrains.dokka.pages.RenderingStrategy

class NavigationPage(val root: NavigationNode) : RendererSpecificPage {
    override val name = "navigation"

    override val children = emptyList<PageNode>()

    override fun modified(name: String, children: List<PageNode>) = this

    override val strategy = RenderingStrategy<HtmlRenderer> {
        createHTML().visit(root, "nav-submenu", this)
    }

    private fun <R> TagConsumer<R>.visit(node: NavigationNode, navId: String, renderer: HtmlRenderer): R =
        with(renderer) {
            div("sideMenuPart") {
                id = navId
                attributes["pageId"] = node.dri.toString()
                div("overview") {
                    buildLink(node.dri, node.sourceSets.toList()) { buildBreakableDotSeparatedHtml(node.name) }
                    if (node.children.isNotEmpty()) {
                        span("navButton pull-right") {
                            onClick = """document.getElementById("$navId").classList.toggle("hidden");"""
                            span("navButtonContent")
                        }
                    }
                }
                node.children.withIndex().forEach { (n, p) -> visit(p, "$navId-$n", renderer) }
            }
        }
}

data class NavigationNode(
    val name: String,
    val dri: DRI,
    val sourceSets: Set<DisplaySourceSet>,
    val children: List<NavigationNode>
)

fun NavigationPage.transform(block: (NavigationNode) -> NavigationNode) = NavigationPage(root.transform(block))

fun NavigationNode.transform(block: (NavigationNode) -> NavigationNode) =
    run(block).let { NavigationNode(it.name, it.dri, it.sourceSets, it.children.map(block)) }
