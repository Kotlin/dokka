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
                        val withIcon = node.children.isEmpty() && node.icon != null
                        if (withIcon) {
                            // in case link text is so long that it needs to have word breaks,
                            // and it stretches to two or more lines, make sure the icon
                            // is always on the left in the grid and is not wrapped with text
                            span("nav-link-grid") {
                                span("nav-link-child ${node.icon?.style()}")
                                span("nav-link-child") {
                                    buildBreakableText(node.name)
                                }
                            }
                        } else {
                            buildBreakableText(node.name)
                        }
                    }
                }
                node.children.withIndex().forEach { (n, p) -> visit(p, "$navId-$n", renderer) }
            }
        }

    private fun NavigationNodeIcon.style(): String = when(this) {
        NavigationNodeIcon.CLASS -> "nav-icon class"
        NavigationNodeIcon.CLASS_KT -> "nav-icon class-kt"
        NavigationNodeIcon.ABSTRACT_CLASS -> "nav-icon abstract-class"
        NavigationNodeIcon.ABSTRACT_CLASS_KT -> "nav-icon abstract-class-kt"
        NavigationNodeIcon.ENUM_CLASS -> "nav-icon enum-class"
        NavigationNodeIcon.ENUM_CLASS_KT -> "nav-icon enum-class-kt"
        NavigationNodeIcon.ANNOTATION_CLASS -> "nav-icon annotation-class"
        NavigationNodeIcon.ANNOTATION_CLASS_KT -> "nav-icon annotation-class-kt"
        NavigationNodeIcon.FUNCTION -> "nav-icon function"
        NavigationNodeIcon.INTERFACE -> "nav-icon interface"
        NavigationNodeIcon.INTERFACE_KT -> "nav-icon interface-kt"
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

/**
 * [CLASS] represents a neutral (a.k.a Java-style) icon,
 * whereas [CLASS_KT] should be Kotlin-styled
 */
enum class NavigationNodeIcon {
    CLASS, CLASS_KT,
    ABSTRACT_CLASS, ABSTRACT_CLASS_KT,
    ENUM_CLASS, ENUM_CLASS_KT,
    ANNOTATION_CLASS, ANNOTATION_CLASS_KT,
    INTERFACE, INTERFACE_KT,
    FUNCTION,
    EXCEPTION,
    OBJECT,
    VAL,
    VAR
}

fun NavigationPage.transform(block: (NavigationNode) -> NavigationNode) =
    NavigationPage(root.transform(block), moduleName, context)

fun NavigationNode.transform(block: (NavigationNode) -> NavigationNode) =
    run(block).let { NavigationNode(it.name, it.dri, it.sourceSets, it.icon, it.children.map(block)) }
