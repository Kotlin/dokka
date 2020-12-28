package org.jetbrains.dokka.webhelp.renderers

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.webhelp.renderers.preprocessors.ConfigurationAppender
import org.jetbrains.dokka.webhelp.renderers.preprocessors.ProjectDefinitionAppender
import org.jetbrains.dokka.webhelp.renderers.preprocessors.TableOfContentPreprocessor
import org.jetbrains.dokka.webhelp.renderers.tags.*
import java.io.File

open class WebhelpRenderer(dokkaContext: DokkaContext) : DefaultRenderer<FlowContent>(dokkaContext) {
    override val extension: String
        get() = ".xml"

    override val preprocessors: List<PageTransformer> =
        listOf(RootCreator, TableOfContentPreprocessor(), ProjectDefinitionAppender, ConfigurationAppender)

    override fun FlowContent.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: FlowContent.() -> Unit
    ) {
        childrenCallback()
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
        code(CodeStyle.BLOCK, code.language) {
            code.children.forEach { buildContentNode(it, pageContext) }
        }

    override fun FlowContent.buildCodeInline(code: ContentCodeInline, pageContext: ContentPage) =
        code(lang = code.language) {
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
                attributes["section"] = sourceset.sourceSetIDs.all.joinToString { it.sourceSetName }
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
        }

    private val ContentPage.id: String?
        get() = locationProvider.resolve(this)?.replace(
            File.separator, "."
        )?.substringBeforeLast(".")

    private fun String.stripDiv() = drop(5).dropLast(6) // TODO: Find a way to do it without arbitrary trims

    override fun FlowContent.buildHeader(level: Int, node: ContentHeader, content: FlowContent.() -> Unit) {
        val classes = node.style.joinToString { it.toString() }.toLowerCase()
        when (level) {
            1 -> h1(classes = classes, content)
            2 -> h2(classes = classes, content)
            3 -> h3(classes = classes, content)
            4 -> h4(classes = classes, content)
            5 -> h5(classes = classes, content)
            else -> h6(classes = classes, content)
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
}