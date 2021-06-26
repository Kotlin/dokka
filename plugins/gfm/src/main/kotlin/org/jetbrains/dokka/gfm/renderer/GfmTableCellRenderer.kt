package org.jetbrains.dokka.gfm.renderer

import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.base.renderers.isImage
import org.jetbrains.dokka.gfm.GfmCommand.Companion.templateCommand
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.ResolveLinkGfmCommand
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query

open class GfmTableCellRenderer(
    context: DokkaContext
) : CommonmarkDivirgentsRenderer(context) {

    override fun StringBuilder.buildNewLine() {
        if (!endsWith(' ')) {
            append(' ')
        }
    }

    override fun StringBuilder.buildLineBreak() {
        buildNewLine()
    }

    override fun StringBuilder.buildParagraph() {
        if (!endsWith("<br>")) {
            append("<br>")
        }
    }

    override fun StringBuilder.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: StringBuilder.() -> Unit
    ) {
        return when {
            node.hasStyle(TextStyle.Block) || node.hasStyle(TextStyle.Paragraph) -> {
                childrenCallback()
                buildParagraph()
            }
            else -> childrenCallback()
        }
    }

    override fun StringBuilder.buildHeader(level: Int, node: ContentHeader, content: StringBuilder.() -> Unit) {
        content()
        buildParagraph()
    }

    override fun StringBuilder.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildParagraph()
        buildListLevel(node, pageContext)
        buildParagraph()
    }

    private fun StringBuilder.buildListItem(items: List<ContentNode>, pageContext: ContentPage) {
        items.forEach {
            if (it is ContentList) {
                buildList(it, pageContext)
            } else {
                append("<li>")
                append(buildString { it.build(this, pageContext, it.sourceSets) }.trim())
                append("</li>")
            }
        }
    }

    private fun StringBuilder.buildListLevel(node: ContentList, pageContext: ContentPage) {
        if (node.ordered) {
            append("<ol>")
            buildListItem(node.children, pageContext)
            append("</ol>")
        } else {
            append("<ul>")
            buildListItem(node.children, pageContext)
            append("</ul>")
        }
    }

    override fun StringBuilder.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        // Table in table is unsupported in GFM
    }

    override fun StringBuilder.buildNavigation(page: PageNode) {
        // Unsupported
    }

    override fun buildPage(page: ContentPage, content: (StringBuilder, ContentPage) -> Unit): String =
        buildString {
            content(this, page)
        }.trim().removeSuffix("<br>").trim()
}
