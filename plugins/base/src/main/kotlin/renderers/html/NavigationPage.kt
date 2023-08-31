/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.html.NavigationNodeIcon.CLASS
import org.jetbrains.dokka.base.renderers.html.NavigationNodeIcon.CLASS_KT
import org.jetbrains.dokka.base.renderers.pageId
import org.jetbrains.dokka.base.templating.AddToNavigationCommand
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext

public class NavigationPage(
    public val root: NavigationNode,
    public val moduleName: String,
    public val context: DokkaContext
) : RendererSpecificPage {

    override val name: String = "navigation"

    override val children: List<PageNode> = emptyList()

    override fun modified(name: String, children: List<PageNode>): NavigationPage = this

    override val strategy: RenderingStrategy = RenderingStrategy<HtmlRenderer> {
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
                        val withIcon = node.icon != null
                        if (withIcon) {
                            // in case link text is so long that it needs to have word breaks,
                            // and it stretches to two or more lines, make sure the icon
                            // is always on the left in the grid and is not wrapped with text
                            span("nav-link-grid") {
                                span("nav-link-child ${node.icon?.style()}")
                                span("nav-link-child") {
                                    nodeText(node)
                                }
                            }
                        } else {
                            nodeText(node)
                        }
                    }
                }
                node.children.withIndex().forEach { (n, p) -> visit(p, "$navId-$n", renderer) }
            }
        }

    private fun FlowContent.nodeText(node: NavigationNode) {
        if (node.styles.contains(TextStyle.Strikethrough)) {
            strike {
                buildBreakableText(node.name)
            }
        } else {
            buildBreakableText(node.name)
        }
    }
}

public data class NavigationNode(
    val name: String,
    val dri: DRI,
    val sourceSets: Set<DisplaySourceSet>,
    val icon: NavigationNodeIcon?,
    val styles: Set<Style> = emptySet(),
    override val children: List<NavigationNode>
) : WithChildren<NavigationNode>

/**
 * [CLASS] represents a neutral (a.k.a Java-style) icon,
 * whereas [CLASS_KT] should be Kotlin-styled
 */
public enum class NavigationNodeIcon(
    private val cssClass: String
) {
    CLASS("class"),
    CLASS_KT("class-kt"),
    ABSTRACT_CLASS("abstract-class"),
    ABSTRACT_CLASS_KT("abstract-class-kt"),
    ENUM_CLASS("enum-class"),
    ENUM_CLASS_KT("enum-class-kt"),
    ANNOTATION_CLASS("annotation-class"),
    ANNOTATION_CLASS_KT("annotation-class-kt"),
    INTERFACE("interface"),
    INTERFACE_KT("interface-kt"),
    FUNCTION("function"),
    EXCEPTION("exception-class"),
    OBJECT("object"),
    TYPEALIAS_KT("typealias-kt"),
    VAL("val"),
    VAR("var");

    internal fun style(): String = "nav-icon $cssClass"
}

public fun NavigationPage.transform(block: (NavigationNode) -> NavigationNode): NavigationPage =
    NavigationPage(root.transform(block), moduleName, context)

public fun NavigationNode.transform(block: (NavigationNode) -> NavigationNode): NavigationNode =
    run(block).let { NavigationNode(it.name, it.dri, it.sourceSets, it.icon, it.styles, it.children.map(block)) }
