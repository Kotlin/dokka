package org.jetbrains.dokka.gfm

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.local.DefaultLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.pages.PageTransformer

class GfmPlugin : DokkaPlugin() {

    val gfmPreprocessors by extensionPoint<PageTransformer>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val renderer by extending {
        (CoreExtensions.renderer
                providing { CommonmarkRenderer(it) }
                applyIf { format == "gfm" }
                override dokkaBase.htmlRenderer)
    }

    val locationProvider by extending {
        (dokkaBase.locationProviderFactory
                providing { MarkdownLocationProviderFactory(it) }
                applyIf { format == "gfm" }
                override dokkaBase.locationProvider)
    }

    val rootCreator by extending {
        gfmPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        (gfmPreprocessors
                providing { PackageListCreator(it, "gfm", "md") }
                order { after(rootCreator) })
    }
}

open class CommonmarkRenderer(
    context: DokkaContext
) : DefaultRenderer<StringBuilder>(context) {

    override val preprocessors = context.plugin<GfmPlugin>().query { gfmPreprocessors }

    override fun StringBuilder.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: StringBuilder.() -> Unit
    ) {
        return when {
            node.hasStyle(TextStyle.Block) -> {
                childrenCallback()
                buildNewLine()
            }
            node.hasStyle(TextStyle.Paragraph) -> {
                buildParagraph()
                childrenCallback()
                buildParagraph()
            }
            else -> childrenCallback()
        }
    }

    override fun StringBuilder.buildHeader(level: Int, node: ContentHeader, content: StringBuilder.() -> Unit) {
        buildParagraph()
        append("#".repeat(level) + " ")
        content()
        buildNewLine()
    }

    override fun StringBuilder.buildLink(address: String, content: StringBuilder.() -> Unit) {
        append("[")
        content()
        append("]($address)")
    }

    override fun StringBuilder.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?
    ) {
        buildListLevel(node, pageContext)
    }

    private fun StringBuilder.buildListItem(items: List<ContentNode>, pageContext: ContentPage) {
        items.forEach {
            if (it is ContentList) {
                buildList(it, pageContext)
            } else {
                append("<li>")
                it.build(this, pageContext)
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

    override fun StringBuilder.buildNewLine() {
        append("  \n")
    }

    private fun StringBuilder.buildParagraph() {
        append("\n\n")
    }

    override fun StringBuilder.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?
    ) {
        buildPlatformDependentItem(content.inner, content.sourceSets, pageContext)
    }

    private fun StringBuilder.buildPlatformDependentItem(
        content: ContentNode,
        sourceSets: Set<DokkaSourceSet>,
        pageContext: ContentPage,
    ) {
        if (content is ContentGroup && content.children.firstOrNull { it is ContentTable } != null) {
            buildContentNode(content, pageContext)
        } else {
            val distinct = sourceSets.map {
                it to buildString { buildContentNode(content, pageContext, setOf(it)) }
            }.groupBy(Pair<DokkaSourceSet, String>::second, Pair<DokkaSourceSet, String>::first)

            distinct.filter { it.key.isNotBlank() }.forEach { (text, platforms) ->
                append(
                    platforms.joinToString(
                        prefix = " [",
                        postfix = "] $text "
                    ) { it.sourceSetID.toString() })
                buildNewLine()
            }
        }
    }

    override fun StringBuilder.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
        append("Resource")
    }

    override fun StringBuilder.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DokkaSourceSet>?
    ) {
        if (node.dci.kind == ContentKind.Sample || node.dci.kind == ContentKind.Parameters) {
            node.sourceSets.forEach { sourcesetData ->
                append(sourcesetData.sourceSetID.toString())
                buildNewLine()
                buildTable(
                    node.copy(
                        children = node.children.filter { it.sourceSets.contains(sourcesetData) },
                        dci = node.dci.copy(kind = ContentKind.Main)
                    ), pageContext, sourceSetRestriction
                )
                buildNewLine()
            }
        } else {
            val size = node.header.size

            if (node.header.isNotEmpty()) {
                append("| ")
                node.header.forEach {
                    it.children.forEach {
                        append(" ")
                        it.build(this, pageContext)
                    }
                    append("| ")
                }
                append("\n")
            } else {
                append("| ".repeat(size))
                if (size > 0) append("|\n")
            }

            append("|---".repeat(size))
            if (size > 0) append("|\n")

            node.children.forEach {
                val builder = StringBuilder()
                it.children.forEach {
                    builder.append("| ")
                    it.build(builder, pageContext)
                }
                append(builder.toString().withEntersAsHtml())
                append(" | ".repeat(size - it.children.size))
                append("\n")
            }
        }
    }

    override fun StringBuilder.buildText(textNode: ContentText) {
        val decorators = decorators(textNode.style)
        append(decorators)
        append(textNode.text)
        append(decorators.reversed())
    }

    override fun StringBuilder.buildNavigation(page: PageNode) {
        locationProvider.ancestors(page).asReversed().forEach { node ->
            append("/")
            if (node.isNavigable) buildLink(node, page)
            else append(node.name)
        }
        buildParagraph()
    }

    override fun buildPage(page: ContentPage, content: (StringBuilder, ContentPage) -> Unit): String =
        buildString {
            content(this, page)
        }

    override fun buildError(node: ContentNode) {
        context.logger.warn("Markdown renderer has encountered problem. The unmatched node is $node")
    }

    override fun StringBuilder.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) {

        val distinct =
            node.groupDivergentInstances(pageContext, { instance, contentPage, sourceSet ->
                instance.before?.let { before ->
                    buildString { buildContentNode(before, pageContext, setOf(sourceSet)) }
                } ?: ""
            }, { instance, contentPage, sourceSet ->
                instance.after?.let { after ->
                    buildString { buildContentNode(after, pageContext, setOf(sourceSet)) }
                } ?: ""
            })

        distinct.values.forEach { entry ->
            val (instance, sourceSets) = entry.getInstanceAndSourceSets()

            append(sourceSets.joinToString(prefix = "#### [", postfix = "]") { it.sourceSetID.toString() })
            buildNewLine()
            instance.before?.let {
                append("##### Brief description")
                buildNewLine()
                buildContentNode(it, pageContext)
                buildNewLine()
            }

            append("##### Content")
            buildNewLine()
            entry.groupBy { buildString { buildContentNode(it.first.divergent, pageContext, setOf(it.second)) } }
                .values.forEach { innerEntry ->
                    val (innerInstance, innerSourceSets) = innerEntry.getInstanceAndSourceSets()
                    if(sourceSets.size > 1) {
                        append(innerSourceSets.joinToString(prefix = "###### [", postfix = "]") { it.sourceSetID.toString() })
                        buildNewLine()
                    }
                    innerInstance.divergent.build(this@buildDivergent, pageContext)
                    buildNewLine()
                }
            instance.after?.let {
                append("##### More info")
                buildNewLine()
                buildContentNode(it, pageContext)
                buildNewLine()
            }
        }
    }

    private fun decorators(styles: Set<Style>) = buildString {
        styles.forEach {
            when (it) {
                TextStyle.Bold -> append("**")
                TextStyle.Italic -> append("*")
                TextStyle.Strong -> append("**")
                TextStyle.Strikethrough -> append("~~")
                else -> Unit
            }
        }
    }

    private val PageNode.isNavigable: Boolean
        get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing

    private fun StringBuilder.buildLink(to: PageNode, from: PageNode) =
        buildLink(locationProvider.resolve(to, from)) {
            append(to.name)
        }

    override suspend fun renderPage(page: PageNode) {
        val path by lazy { locationProvider.resolve(page, skipExtension = true) }
        when (page) {
            is ContentPage -> outputWriter.write(path, buildPage(page) { c, p -> buildPageContent(c, p) }, ".md")
            is RendererSpecificPage -> when (val strategy = page.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, path)
                is RenderingStrategy.Write -> outputWriter.write(path, strategy.text, "")
                is RenderingStrategy.Callback -> outputWriter.write(path, strategy.instructions(this, page), ".md")
                RenderingStrategy.DoNothing -> Unit
            }
            else -> throw AssertionError(
                "Page ${page.name} cannot be rendered by renderer as it is not renderer specific nor contains content"
            )
        }
    }

    private fun String.withEntersAsHtml(): String = replace("\n", "<br>")

    private fun List<Pair<ContentDivergentInstance, DokkaSourceSet>>.getInstanceAndSourceSets() = this.let { Pair(it.first().first, it.map { it.second }.toSet()) }
}

class MarkdownLocationProviderFactory(val context: DokkaContext) : LocationProviderFactory {

    override fun getLocationProvider(pageNode: RootPageNode) = MarkdownLocationProvider(pageNode, context)
}

class MarkdownLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext
) : DefaultLocationProvider(
    pageGraphRoot,
    dokkaContext
) {
    override val extension = ".md"
}
