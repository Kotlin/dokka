package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.pageId
import org.jetbrains.dokka.base.templating.AddToNavigationCommand
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.pages.DriResolver
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RendererSpecificPage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.plugability.DokkaContext

class NavigationPage(val root: NavigationNode, val moduleName: String, val context: DokkaContext, val resolver: DriResolver) {
    private fun <R> TagConsumer<R>.visit(node: NavigationNode) =
        if (context.configuration.delayTemplateSubstitution) {
            templateCommand(AddToNavigationCommand(moduleName)) {
                visit(node, "${moduleName}-nav-submenu")
            }
        } else {
            visit(node, "${moduleName}-nav-submenu")
        }

    private fun <R> TagConsumer<R>.visit(node: NavigationNode, navId: String): R =
        div("sideMenuPart") {
            id = navId
            attributes["pageId"] = "${moduleName}::${node.pageId}"
            div("overview") {
                if (node.children.isNotEmpty()) {
                    span("navButton") {
                        onClick = """document.getElementById("$navId").classList.toggle("hidden");"""
                        span("navButtonContent")
                    }
                }
                a {
                    href = resolver.invoke(node.dri, node.sourceSets).orEmpty()
                    buildBreakableText(node.name)
                }
            }
            node.children.withIndex().forEach { (n, p) -> visit(p, "$navId-$n") }
        }

    fun invoke(): String = createHTML(prettyPrint = false).visit(root)
}

data class NavigationNode(
    val name: String,
    val dri: DRI,
    val sourceSets: Set<DisplaySourceSet>,
    override val children: List<NavigationNode>
) : WithChildren<NavigationNode>
