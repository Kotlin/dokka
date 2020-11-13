package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.pageId
import org.jetbrains.dokka.base.templating.AddToNavigationCommand
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RendererSpecificPage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.plugability.DokkaContext

class NavigationPage(val root: NavigationNode, val moduleName: String, val context: DokkaContext) :
    RendererSpecificPage {
    override val name = "navigation"

    override val children = emptyList<PageNode>()

    override fun modified(name: String, children: List<PageNode>) = this

    override val strategy = RenderingStrategy<HtmlRenderer> {
        createHTML().visit(root, this)
    }

    private fun <R> TagConsumer<R>.visit(node: NavigationNode, renderer: HtmlRenderer): R = with(renderer) {
        if (context.configuration.delayTemplateSubstitution) {
            templateCommand(AddToNavigationCommand(moduleName)) {
                visit(node, "${moduleName}-nav-submenu", renderer)
            }
        } else {
            visit(node, "${moduleName}-nav-submenu", renderer)
        }
    }

    private fun <R> TagConsumer<R>.visit(node: NavigationNode, navId: String, renderer: HtmlRenderer): R =
        with(renderer) {
            div("sideMenuPart") {
                id = navId
                attributes["pageId"] = "${moduleName}::${node.pageId}"
                div("overview") {
                    buildLink(node.dri, node.sourceSets.toList()) { buildBreakableText(node.name) }
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
    override val children: List<NavigationNode>
) : WithChildren<NavigationNode>

fun NavigationPage.transform(block: (NavigationNode) -> NavigationNode) =
    NavigationPage(root.transform(block), moduleName, context)

fun NavigationNode.transform(block: (NavigationNode) -> NavigationNode) =
    run(block).let { NavigationNode(it.name, it.dri, it.sourceSets, it.children.map(block)) }
