package org.jetbrains.dokka.gfm

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.local.DefaultLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.pages.PageTransformer
import java.lang.StringBuilder


class GfmPlugin : DokkaPlugin() {

    val gfmPreprocessors by extensionPoint<PageTransformer>()

    val renderer by extending {
        CoreExtensions.renderer providing { CommonmarkRenderer(it) } applyIf { format == "gfm" }
    }

    val locationProvider by extending {
        plugin<DokkaBase>().locationProviderFactory providing { MarkdownLocationProviderFactory(it) } applyIf { format == "gfm" }
    }

    val rootCreator by extending {
        gfmPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        gfmPreprocessors providing {
            PackageListCreator(
                it,
                "gfm",
                "md"
            )
        } order { after(rootCreator) }
    }
}

open class CommonmarkRenderer(
    context: DokkaContext
) : DefaultRenderer<StringBuilder>(context) {

    override val preprocessors = context.plugin<GfmPlugin>().query { gfmPreprocessors }

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
        platformRestriction: Set<SourceSetData>?
    ) {
        buildParagraph()
        buildListLevel(node, pageContext)
        buildParagraph()
    }

    private val indent = " ".repeat(4)

    private fun StringBuilder.buildListItem(items: List<ContentNode>, pageContext: ContentPage, bullet: String = "*") {
        items.forEach {
            if (it is ContentList) {
                val builder = StringBuilder()
                builder.append(indent)
                builder.buildListLevel(it, pageContext)
                append(builder.toString().replace(Regex("  \n(?!$)"), "  \n$indent"))
            } else {
                append("$bullet ")
                it.build(this, pageContext)
                buildNewLine()
            }
        }
    }

    private fun StringBuilder.buildListLevel(node: ContentList, pageContext: ContentPage) {
        if (node.ordered) {
            buildListItem(
                node.children,
                pageContext,
                "${node.extra.allOfType<SimpleAttr>().find { it.extraKey == "start" }?.extraValue
                    ?: 1.also { context.logger.error("No starting number specified for ordered list in node ${pageContext.dri.first()}!") }}."
            )
        } else {
            buildListItem(node.children, pageContext, "*")
        }
    }

    override fun StringBuilder.buildNewLine() {
        append("  \n")
    }

    private fun StringBuilder.buildParagraph() {
        append("\n\n")
    }

    override fun StringBuilder.buildPlatformDependent(content: PlatformHintedContent, pageContext: ContentPage) {
        val distinct = content.sourceSets.map {
            it to StringBuilder().apply {buildContentNode(content.inner, pageContext, setOf(it)) }.toString()
        }.groupBy(Pair<SourceSetData, String>::second, Pair<SourceSetData, String>::first)

        if (distinct.size == 1)
            append(distinct.keys.single())
        else
            distinct.forEach { text, platforms ->
                append(platforms.joinToString(prefix = " [", postfix = "] $text") { "${it.moduleName}/${it.sourceSetID}"  })
            }
    }

    override fun StringBuilder.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
        append("Resource")
    }

    override fun StringBuilder.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        platformRestriction: Set<SourceSetData>?
    ) {

        buildParagraph()
        val size = node.children.firstOrNull()?.children?.size ?: 0

        if (node.header.size > 0) {
            node.header.forEach {
                it.children.forEach {
                    append("| ")
                    it.build(this, pageContext)
                }
                append("|\n")
            }
        } else {
            append("| ".repeat(size))
            if (size > 0) append("|\n")
        }

        append("|---".repeat(size))
        if (size > 0) append("|\n")

        node.children.forEach {
            it.children.forEach {
                append("| ")
                it.build(this, pageContext)
            }
            append("|\n")
        }

        buildParagraph()
    }

    override fun StringBuilder.buildText(textNode: ContentText) {
        val decorators = decorators(textNode.style)
        append(decorators)
        append(textNode.text.replace(Regex("[<>]"), ""))
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
        StringBuilder().apply {
            content(this, page)
        }.toString()

    override fun buildError(node: ContentNode) {
        context.logger.warn("Markdown renderer has encountered problem. The unmatched node is $node")
    }

    private fun decorators(styles: Set<Style>) = StringBuilder().apply {
        styles.forEach {
            when (it) {
                TextStyle.Bold -> append("**")
                TextStyle.Italic -> append("*")
                TextStyle.Strong -> append("**")
                TextStyle.Strikethrough -> append("~~")
                else -> Unit
            }
        }
    }.toString()

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