package org.jetbrains.dokka.webhelp.renderers

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.renderers.TabSortingStrategy
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.resolvers.local.DokkaBaseLocationProvider
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.webhelp.renderers.preprocessors.ConfigurationAppender
import org.jetbrains.dokka.webhelp.renderers.preprocessors.ProjectDefinitionAppender
import org.jetbrains.dokka.webhelp.renderers.preprocessors.TableOfContentPreprocessor
import org.jetbrains.dokka.webhelp.renderers.tags.*
import org.jsoup.Jsoup
import java.io.File

open class WebhelpRenderer(private val dokkaContext: DokkaContext) : DefaultRenderer<FlowContent>(dokkaContext) {
    override val extension: String
        get() = ".xml"

    override val preprocessors: List<PageTransformer> =
        listOf(RootCreator, TableOfContentPreprocessor(), ProjectDefinitionAppender, ConfigurationAppender)

    private val tabSortingStrategy = context.plugin<DokkaBase>().querySingle { tabSortingStrategy }

    private fun <T : ContentNode> sortTabs(strategy: TabSortingStrategy, tabs: Collection<T>): List<T> {
        val sorted = strategy.sort(tabs)
        if (sorted.size != tabs.size)
            context.logger.warn("Tab sorting strategy has changed number of tabs from ${tabs.size} to ${sorted.size}")
        return sorted
    }

    override fun FlowContent.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: FlowContent.() -> Unit
    ) {
        when {
            node.hasStyle(ContentStyle.TabbedContent) && node.children.isNotEmpty() -> tabs {
                val secondLevel = node.children.filterIsInstance<ContentComposite>().flatMap { it.children }
                    .filterIsInstance<ContentHeader>().flatMap { it.children }.filterIsInstance<ContentText>()
                val firstLevel = node.children.filterIsInstance<ContentHeader>().flatMap { it.children }
                    .filterIsInstance<ContentText>()

                sortTabs(tabSortingStrategy, firstLevel.union(secondLevel)).forEach { element ->
                    tab(title = element.text) {
                        node.dfs {
                            it.extra.allOfType<SimpleAttr>()
                                .any { it.extraKey == "data-togglable" && it.extraValue == element.text }
                        }?.build(this, pageContext)
                    }
                }
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
                attributes["section"] = row.sourceSets.joinToString(",") { it.name }
                if (row.isAnchorable) {
                    attributes["id"] = row.anchor!!
                }
                row.children.forEach { cell ->
//                    cell.sourceSets.forEach { cellSourceset ->
                        td {
                            attributes["section"] = cell.sourceSets.joinToString(",") { it.name }
                            cell.build(this@table, pageContext, sourceSetRestriction)
                        }
//                    }
                }
            }
//            tr {
//                if (row.isAnchorable) {
//                    attributes["id"] = row.anchor!!
//                }
//                row.children.forEach {
//                    td {
//                        it.build(this, pageContext, sourceSetRestriction)
//                    }
//                }
//            }
        }
    }

//    private fun HTMLTag.buildWithSourcesetInformation(sourceset: DisplaySourceSet, block: FlowContent.() -> Unit) {
//        createHTML().div {
//            visit(block)
////            block()
//        }.let {
//            val document = Jsoup.parse(it, "UTF-8")
//            document.allElements.map { it.attr("section", sourceset.name) }
//            unsafe {
//                +document.select("div").last().html()
//            }
//        }
//    }

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
        buildPlatformDependent(
            content.sourceSets.filter {
                sourceSetRestriction == null || it in sourceSetRestriction
            }.map { it to setOf(content.inner) }.toMap(),
            pageContext,
            content.extra,
            content.style
        )
    }

    fun FlowContent.buildPlatformDependent(
        nodes: Map<DisplaySourceSet, Collection<ContentNode>>,
        pageContext: ContentPage,
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        styles: Set<Style> = emptySet()
    ) {
        val contents = contentsForSourceSetDependent(nodes, pageContext)
        contents.forEach {
            consumer.onTagContentUnsafe { +it.second }
        }
    }

    private fun contentsForSourceSetDependent(
        nodes: Map<DisplaySourceSet, Collection<ContentNode>>,
        pageContext: ContentPage,
    ): List<Pair<DisplaySourceSet, String>> {
        return nodes.toList().map { (sourceSet, elements) ->
            sourceSet to createHTML(prettyPrint = false).div {
                elements.forEach {
                    buildContentNode(it, pageContext, sourceSet.toSet())
                }
            }
        }.groupBy(
            Pair<DisplaySourceSet, String>::second,
            Pair<DisplaySourceSet, String>::first
        ).entries.flatMap { (html, sourceSets) ->
            sourceSets.map { displaySourceSet ->
                displaySourceSet to createHTML(prettyPrint = false)
                    .div {
                        val document = Jsoup.parse(html, "UTF-8")
                        document.allElements.map { it.attr("section", displaySourceSet.name) }
                        unsafe {
                            +document.select("div").last().html()
                        }
                    }.stripDiv()
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
        val distinct =
            node.groupDivergentInstances(pageContext,
                beforeTransformer = { instance, _, sourceSet ->
                    createHTML(prettyPrint = false).div {
                        instance.before?.let { before ->
                            buildContentNode(before, pageContext, sourceSet)
                        }
                    }.stripDiv()
                },
                afterTransformer = { instance, _, sourceSet ->
                    createHTML(prettyPrint = false).div {
                        instance.after?.let { after ->
                            buildContentNode(after, pageContext, sourceSet)
                        }
                    }.stripDiv()
                })

        distinct.forEach { distinctInstances ->
            val groupedDivergent = distinctInstances.value.groupBy { it.second }

            consumer.onTagContentUnsafe {
                +createHTML().p {
                    attributes["section"] = groupedDivergent.keys.joinToString(",") {
                        it.name
                    }

                    val divergentForPlatformDependent = groupedDivergent.map { (sourceSet, elements) ->
                        sourceSet to elements.map { e -> e.first.divergent }
                    }.toMap()

                    consumer.onTagContentUnsafe {
                        +createHTML().p {
                            consumer.onTagContentUnsafe { +distinctInstances.key.first }
                        } //tu sie wywala
                    }
//                    p {
                        if (node.implicitlySourceSetHinted) {
                            buildPlatformDependent(divergentForPlatformDependent, pageContext)
                        } else {
                            distinctInstances.value.forEach {
                                buildContentNode(it.first.divergent, pageContext, setOf(it.second))
                            }
                        }
//                    }
                    consumer.onTagContentUnsafe { +distinctInstances.key.second }
                }
            }
        }
//        node.sourceSets.forEach { sourceset ->
//            node.children.filter { sourceset in it.sourceSets }.forEach { toBuild ->
//                p {
//                    attributes["section"] = sourceset.name
//                    toBuild.build(this, pageContext)
//                }
//            }
//        }
//        node.children.forEach { it.build(this, pageContext) }
//        node.children.forEach {
//            p {
//
//                attributes["section"] = "divergent"
//                it.build(this, pageContext)
//            }
//        }
    }

    override fun FlowContent.buildDivergentInstance(node: ContentDivergentInstance, pageContext: ContentPage) {
        node.sourceSets.forEach { sourceset ->
            node.before?.takeIf { sourceset in it.sourceSets }?.let {
                p {
                    attributes["section"] = sourceset.name
                    it.build(this, pageContext)
                }
            }
            node.divergent.takeIf { sourceset in it.sourceSets }?.let {
                p {
                    attributes["section"] = sourceset.name
                    it.build(this, pageContext)
                }
            }
            node.after?.takeIf { sourceset in it.sourceSets }?.let {
                p {
                    attributes["section"] = sourceset.name
                    it.build(this, pageContext)
                }
            }
        }
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