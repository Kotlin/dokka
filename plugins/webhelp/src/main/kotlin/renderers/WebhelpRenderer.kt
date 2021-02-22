package org.jetbrains.dokka.webhelp.renderers

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.resolvers.local.DokkaBaseLocationProvider
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.webhelp.renderers.preprocessors.ConfigurationAppender
import org.jetbrains.dokka.webhelp.renderers.preprocessors.ProjectDefinitionAppender
import org.jetbrains.dokka.webhelp.renderers.preprocessors.TableOfContentPreprocessor
import org.jetbrains.dokka.webhelp.renderers.tags.*
import java.io.File

open class WebhelpRenderer(private val dokkaContext: DokkaContext) : DefaultRenderer<FlowContent>(dokkaContext) {
    override val extension: String
        get() = ".xml"

    override val preprocessors: List<PageTransformer> =
        listOf(RootCreator, TableOfContentPreprocessor(), ProjectDefinitionAppender, ConfigurationAppender)

    override fun FlowContent.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: FlowContent.() -> Unit
    ) {
        when {
            node.isAnchorable -> {
                anchor(node.anchor!!) { }
                childrenCallback()
            }
            node.hasStyle(TextStyle.Paragraph) || node.hasStyle(TextStyle.Block) -> p { childrenCallback() }
            else -> childrenCallback()
        }
    }

    override fun FlowContent.buildGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        if (node.dci.kind in setOf(ContentKind.Symbol)) {
            code {
                attributes["style"] = "block"
                attributes["lang"] = "kotlin"
                val renderer = WebhelpCodeRenderer(dokkaContext)
                +node.children.joinToString(separator = "") { renderer.buildContentNode(it, pageContext) }
            }
        } else {
            wrapGroup(node, pageContext) { node.children.forEach { it.build(this, pageContext, sourceSetRestriction) } }
        }
    }

    override fun FlowContent.buildLink(address: String, content: FlowContent.() -> Unit) =
        a(href = address) {
            attributes["nullable"] = "true"
            content()
        }

    override fun FlowContent.buildDRILink(
        node: ContentDRILink,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        locationProvider.resolve(node.address, node.sourceSets)?.let { address ->
            buildLink(address) {
                buildText(node.children, pageContext, sourceSetRestriction)
            }
        }
    }

    override fun FlowContent.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) = table {
        when {
            node.style.contains(CommentTable) -> node.header.takeIf { it.isNotEmpty() }?.let { headers ->
                tr {
                    headers.forEach {
                        td {
                            it.build(this@table, pageContext, sourceSetRestriction)
                        }
                    }
                }
            }
            else -> node.children.firstOrNull()?.let {
                tr {
                    it.children.forEach { _ -> td {} }
                }
            }
        }

        node.children.forEach { row ->
            tr {
                row.children.forEach {
                    td {
                        it.build(this@table, pageContext, sourceSetRestriction)
                    }
                }
            }
        }
    }

    override fun FlowContent.buildCodeBlock(code: ContentCodeBlock, pageContext: ContentPage) =
        code {
            code.children.forEach { buildContentNode(it, pageContext) }
        }

    override fun FlowContent.buildCodeInline(code: ContentCodeInline, pageContext: ContentPage) =
        code {
            code.children.forEach { buildContentNode(it, pageContext) }
        }


    override fun FlowContent.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) = list {
        buildListItems(node.children, pageContext, sourceSetRestriction)
    }

    override fun FlowContent.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        content.sourceSets.filter {
            sourceSetRestriction == null || it in sourceSetRestriction
        }.map { it to setOf(content.inner) }.forEach { (sourceset, nodes) ->
            p {
//                attributes["section"] = sourceset.sourceSetIDs.all.joinToString { it.sourceSetName }
                nodes.forEach { node -> node.build(this, pageContext) }
            }
        }
    }

    open fun LIST.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        items.forEach {
            if (it is ContentList)
                buildList(it, pageContext)
            else
                li { it.build(this, pageContext) }
        }
    }

    override fun FlowContent.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) {
        node.children.forEach { it.build(this, pageContext) }
    }

    override fun FlowContent.buildDivergentInstance(node: ContentDivergentInstance, pageContext: ContentPage) {
        node.before?.let { p { it.build(this, pageContext) } }
        node.divergent.let { p { it.build(this, pageContext) } }
        node.after?.let { p { it.build(this, pageContext) } }
    }

    override fun FlowContent.buildText(textNode: ContentText) = text(textNode.text)

    override fun FlowContent.buildNavigation(page: PageNode) {}

    override fun buildPage(page: ContentPage, content: (FlowContent, ContentPage) -> Unit): String =
        createHTML(prettyPrint = false).div {
            content(this, page)
        }.stripDiv().let { topic ->
            """
               <?xml version="1.0" encoding="UTF-8"?>
               <!DOCTYPE topic SYSTEM "https://resources.jetbrains.com/stardust/html-entities.dtd">
               <topic title="${page.name}" id="${page.id}" xsi:noNamespaceSchemaLocation="https://resources.jetbrains.com/stardust/topic.v2.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
               $topic
               </topic>
            """.trimIndent()
        }.replace("&quot;", "\"")

    private val ContentPage.id: String?
        get() = locationProvider.resolve(this)?.replace(
            File.separator, "."
        )?.substringBeforeLast(".")?.substringAfter(".")

    private fun String.stripDiv() = drop(5).dropLast(6) // TODO: Find a way to do it without arbitrary trims

    override fun FlowContent.buildHeader(level: Int, node: ContentHeader, content: FlowContent.() -> Unit) {
        when (level) {
            1 -> h1(classes = null, content)
            2 -> h2(classes = null, content)
            3 -> h3(classes = null, content)
            4 -> h4(classes = null, content)
            5 -> h5(classes = null, content)
            else -> h6(classes = null, content)
        }
    }

    override fun FlowContent.buildNewLine() {
        p { }
    }

    override fun FlowContent.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
        TODO("Not yet implemented")
    }

    override fun buildError(node: ContentNode) {
        TODO("Not yet implemented")
    }

    private inner class WebhelpCodeRenderer(dokkaContext: DokkaContext) : DefaultRenderer<StringBuilder>(dokkaContext) {
        override fun StringBuilder.wrapGroup(
            node: ContentGroup,
            pageContext: ContentPage,
            childrenCallback: StringBuilder.() -> Unit
        ) {
            when {
                node.hasStyle(TextStyle.Paragraph) || node.hasStyle(TextStyle.Block) -> {
                    if (lastOrNull() != '\n') append("\n")
                    childrenCallback()
                    if (lastOrNull() != '\n') append("\n")
                }
                else -> childrenCallback()
            }
        }

        fun buildContentNode(node: ContentNode, pageContext: ContentPage): String {
            locationProvider = this@WebhelpRenderer.locationProvider
            return StringBuilder().also { it.buildContentNode(node, pageContext) }.toString()
        }

        override fun StringBuilder.buildHeader(level: Int, node: ContentHeader, content: StringBuilder.() -> Unit) {
            append("<h$level>")
            content()
            append("</h$level>")
        }

        override fun StringBuilder.buildLink(address: String, content: StringBuilder.() -> Unit) {
            append("[[[")
            content()
//            append("|https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html]]]")
            append("|$address]]]")
        }

        override fun StringBuilder.buildList(
            node: ContentList,
            pageContext: ContentPage,
            sourceSetRestriction: Set<DisplaySourceSet>?
        ) {
            TODO("Not yet implemented")
        }

        override fun StringBuilder.buildNewLine() {
            append("\n")
        }

        override fun StringBuilder.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
            TODO("Not yet implemented")
        }

        override fun StringBuilder.buildTable(
            node: ContentTable,
            pageContext: ContentPage,
            sourceSetRestriction: Set<DisplaySourceSet>?
        ) {
            TODO("Not yet implemented")
        }

        override fun StringBuilder.buildText(textNode: ContentText) {
            append(textNode.text)
        }

        override fun StringBuilder.buildNavigation(page: PageNode) {
            TODO("Not yet implemented")
        }

        override fun buildPage(page: ContentPage, content: (StringBuilder, ContentPage) -> Unit): String =
            StringBuilder().also { content(it, page) }.toString()

        override fun buildError(node: ContentNode) {
            TODO("Not yet implemented")
        }
    }

    private val ContentNode.isAnchorable: Boolean
        get() = anchorLabel != null

    private val ContentNode.anchorLabel: String?
        get() = extra[SymbolAnchorHint]?.anchorName

    private val ContentNode.anchor: String?
        get() = extra[SymbolAnchorHint]?.contentKind?.let { contentKind ->
            (locationProvider as DokkaBaseLocationProvider).anchorForDCI(DCI(dci.dri, contentKind), sourceSets)
        }
}