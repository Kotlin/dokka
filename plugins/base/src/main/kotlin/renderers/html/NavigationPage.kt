package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.pageId
import org.jetbrains.dokka.base.templating.AddToNavigationCommand
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.pages.*
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
                    if (node.children.isNotEmpty()) {
                        span("navButton") {
                            onClick = """document.getElementById("$navId").classList.toggle("hidden");"""
                            span("navButtonContent")
                        }
                    }
                    buildLink(node.dri, node.sourceSets.toList()) {
                        span("nav-link-text") {
                            if (node.icon != null) {
                                span(node.icon.style())
                            }
                            buildBreakableText(node.name)
                        }
                    }
                }
                node.children.withIndex().forEach { (n, p) -> visit(p, "$navId-$n", renderer) }
            }
        }

    private fun NavigationNodeIcon.style(): String = when(this) {
        NavigationNodeIcon.CLASS -> "nav-icon class"
        NavigationNodeIcon.ABSTRACT_CLASS -> "nav-icon abstract-class"
        NavigationNodeIcon.ENUM_CLASS -> "nav-icon enum-class"
        NavigationNodeIcon.ANNOTATION_CLASS -> "nav-icon annotation-class"
        NavigationNodeIcon.FUNCTION -> "nav-icon function"
        NavigationNodeIcon.INTERFACE -> "nav-icon interface"
        NavigationNodeIcon.EXCEPTION -> "nav-icon exception-class"
        NavigationNodeIcon.OBJECT -> "nav-icon object"
        NavigationNodeIcon.VAL -> "nav-icon val"
        NavigationNodeIcon.VAR -> "nav-icon var"
    }
}

data class NavigationNode(
    val name: String,
    val dri: DRI,
    val sourceSets: Set<DisplaySourceSet>,
    val icon: NavigationNodeIcon?,
    override val children: List<NavigationNode>
) : WithChildren<NavigationNode>

enum class NavigationNodeIcon {
    CLASS, ABSTRACT_CLASS, ENUM_CLASS, ANNOTATION_CLASS, FUNCTION, INTERFACE, EXCEPTION, OBJECT, VAL, VAR
}

fun NavigationPage.transform(block: (NavigationNode) -> NavigationNode) =
    NavigationPage(root.transform(block), moduleName, context)

fun NavigationNode.transform(block: (NavigationNode) -> NavigationNode) =
    run(block).let { NavigationNode(it.name, it.dri, it.sourceSets, it.icon, it.children.map(block)) }
