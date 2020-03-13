package org.jetbrains.dokka.commonmarkrenderer

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import java.lang.StringBuilder


class CommonmarkRendererPlugin : DokkaPlugin() {

    val locationProviderFactory by extensionPoint<LocationProviderFactory>()
    val outputWriter by extensionPoint<OutputWriter>()

    val renderer by extending {
        CoreExtensions.renderer providing { CommonmarkRenderer(it.single(outputWriter), it) }
    }

    val locationProvider by extending {
        locationProviderFactory providing { MarkdownLocationProviderFactory(it) } order {
            before(renderer)
        }
    }
}

class CommonmarkRenderer(
    outputWriter: OutputWriter,
    context: DokkaContext
) : DefaultRenderer<StringBuilder>(outputWriter, context) {
    override fun StringBuilder.buildHeader(level: Int, content: StringBuilder.() -> Unit) {
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

    override fun StringBuilder.buildList(node: ContentList, pageContext: ContentPage, platformRestriction: PlatformData?) {
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
                    ?: 1.also { context.logger.error("No starting number specified for ordered list in node ${pageContext.dri.first()}!")}}."
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

    override fun StringBuilder.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
        append("Resource")
    }

    override fun StringBuilder.buildTable(node: ContentTable, pageContext: ContentPage, platformRestriction: PlatformData?) {

        val size = node.children.firstOrNull()?.children?.size ?: 0
        buildParagraph()

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
    }

    override fun StringBuilder.buildText(textNode: ContentText) {
        val decorators = decorators(textNode.style)
        append(decorators)
        append(textNode.text.escapeIllegalCharacters())
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

    override fun renderPage(page: PageNode) {
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