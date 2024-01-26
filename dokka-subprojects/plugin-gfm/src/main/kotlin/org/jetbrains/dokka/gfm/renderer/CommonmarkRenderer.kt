/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.utilities.htmlEscape

public open class CommonmarkRenderer(
    context: DokkaContext
) : DefaultRenderer<StringBuilder>(context) {

    override val preprocessors: List<PageTransformer> = context.plugin<GfmPlugin>().query { gfmPreprocessors }

    private val isPartial = context.configuration.delayTemplateSubstitution

    override fun StringBuilder.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: StringBuilder.() -> Unit
    ) {
        return when {
            node.hasStyle(TextStyle.Block) || node.hasStyle(TextStyle.Paragraph) -> {
                buildParagraph()
                childrenCallback()
                buildParagraph()
            }
            node.dci.kind == ContentKind.Deprecation -> {
                append("---")
                childrenCallback()
                append("---")
                buildNewLine()
            }
            node.hasStyle(ContentStyle.Footnote) -> {
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
        buildParagraph()
    }

    override fun StringBuilder.buildLink(address: String, content: StringBuilder.() -> Unit) {
        append("[")
        content()
        append("]($address)")
    }

    override fun StringBuilder.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildParagraph()
        buildList(node, pageContext)
        buildParagraph()
    }

    private fun StringBuilder.buildList(
        node: ContentList,
        pageContext: ContentPage
    ) {
        node.children.forEachIndexed { i, it ->
            if (node.ordered) {
                // number is irrelevant, but a nice touch
                // period is more widely compatible
                append("${i + 1}. ")
            } else {
                append("- ")
            }

            /*
            Handle case when list item transitions to another complex node with no preceding text.
            For example, the equivalent of:
            <li>
               <ul><li><ul>Item</ul></li></ul>
            </li>

            Would be:
            -
               - Item
             */
            if (it is ContentGroup && it.children.firstOrNull()?.let { it !is ContentText } == true) {
                append("\n   ")
            }

            buildString { it.build(this, pageContext, it.sourceSets) }
                .replace("\n", "\n   ") // apply indent
                .trim().let { append(it) }
            buildNewLine()
        }
    }

    override fun StringBuilder.buildDRILink(
        node: ContentDRILink,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        val location = locationProvider.resolve(node.address, node.sourceSets, pageContext)
        if (location == null) {
            if (isPartial) {
                templateCommand(ResolveLinkGfmCommand(node.address)) {
                    buildText(node.children, pageContext, sourceSetRestriction)
                }
            } else {
                buildText(node.children, pageContext, sourceSetRestriction)
            }
        } else {
            buildLink(location) {
                buildText(node.children, pageContext, sourceSetRestriction)
            }
        }
    }

    override fun StringBuilder.buildLineBreak() {
        append("\\")
        buildNewLine()
    }

    private fun StringBuilder.buildNewLine() {
        append("\n")
    }

    private fun StringBuilder.buildParagraph() {
        buildNewLine()
        buildNewLine()
    }

    override fun StringBuilder.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildPlatformDependentItem(content.inner, content.sourceSets, pageContext)
    }

    private fun StringBuilder.buildPlatformDependentItem(
        content: ContentNode,
        sourceSets: Set<DisplaySourceSet>,
        pageContext: ContentPage,
    ) {
        if (content is ContentGroup && content.children.firstOrNull { it is ContentTable } != null) {
            buildContentNode(content, pageContext, sourceSets)
        } else {
            val distinct = sourceSets.map {
                it to buildString { buildContentNode(content, pageContext, setOf(it)) }
            }.groupBy(Pair<DisplaySourceSet, String>::second, Pair<DisplaySourceSet, String>::first)

            distinct.filter { it.key.isNotBlank() }.forEach { (text, platforms) ->
                buildParagraph()
                buildSourceSetTags(platforms.toSet())
                buildLineBreak()
                append(text.trim())
                buildParagraph()
            }
        }
    }

    override fun StringBuilder.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
        if (node.isImage()) {
            append("!")
        }
        append("[${node.altText}](${node.address})")
    }

    override fun StringBuilder.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildNewLine()
        if (node.dci.kind == ContentKind.Sample || node.dci.kind == ContentKind.Parameters) {
            node.sourceSets.forEach { sourcesetData ->
                append(sourcesetData.name)
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
            val size = node.header.firstOrNull()?.children?.size ?: node.children.firstOrNull()?.children?.size ?: 0
            if (size <= 0) return

            if (node.header.isNotEmpty()) {
                node.header.forEach {
                    it.children.forEach {
                        append("| ")
                        it.build(this, pageContext, it.sourceSets)
                        append(" ")
                    }
                }
            } else {
                append("| ".repeat(size))
            }
            append("|")
            buildNewLine()

            append("|---".repeat(size))
            append("|")
            buildNewLine()

            node.children.forEach { row ->
                row.children.forEach { cell ->
                    append("| ")
                    append(buildString { cell.build(this, pageContext) }
                        .trim()
                        .replace("#+ ".toRegex(), "") // Workaround for headers inside tables
                        .replace("\\\n", "\n\n")
                        .replace("\n[\n]+".toRegex(), "<br>")
                        .replace("\n", " ")
                    )
                    append(" ")
                }
                append("|")
                buildNewLine()
            }
        }
    }

    override fun StringBuilder.buildText(textNode: ContentText) {
        if (textNode.extra[HtmlContent] != null) {
            append(textNode.text)
        } else if (textNode.text.isNotBlank()) {
            val decorators = decorators(textNode.style)
            append(textNode.text.takeWhile { it == ' ' })
            append(decorators)
            append(textNode.text.trim().htmlEscape())
            append(decorators.reversed())
            append(textNode.text.takeLastWhile { it == ' ' })
        }
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
        }.trim().replace("\n[\n]+".toRegex(), "\n\n")

    override fun buildError(node: ContentNode) {
        context.logger.warn("Markdown renderer has encountered problem. The unmatched node is $node")
    }

    override fun StringBuilder.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) {

        val distinct =
            node.groupDivergentInstances(pageContext, { instance, _, sourceSet ->
                instance.before?.let { before ->
                    buildString { buildContentNode(before, pageContext, sourceSet) }
                } ?: ""
            }, { instance, _, sourceSet ->
                instance.after?.let { after ->
                    buildString { buildContentNode(after, pageContext, sourceSet) }
                } ?: ""
            })

        distinct.values.forEach { entry ->
            val (instance, sourceSets) = entry.getInstanceAndSourceSets()

            buildParagraph()
            buildSourceSetTags(sourceSets)
            buildLineBreak()

            instance.before?.let {
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
                buildParagraph()
            }

            entry.groupBy { buildString { buildContentNode(it.first.divergent, pageContext, setOf(it.second)) } }
                .values.forEach { innerEntry ->
                    val (innerInstance, innerSourceSets) = innerEntry.getInstanceAndSourceSets()
                    if (sourceSets.size > 1) {
                        buildSourceSetTags(innerSourceSets)
                        buildLineBreak()
                    }
                    innerInstance.divergent.build(
                        this@buildDivergent,
                        pageContext,
                        setOf(innerSourceSets.first())
                    ) // It's workaround to render content only once
                    buildParagraph()
                }

            instance.after?.let {
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
            }

            buildParagraph()
        }
    }

    override fun StringBuilder.buildCodeBlock(code: ContentCodeBlock, pageContext: ContentPage) {
        append("```")
        append(code.language.ifEmpty { "kotlin" })
        buildNewLine()
        code.children.forEach {
            if (it is ContentText) {
                // since this is a code block where text will be rendered as is,
                // no need to escape text, apply styles, etc. Just need the plain value
                append(it.text)
            } else if (it is ContentBreakLine) {
                // since this is a code block where text will be rendered as is,
                // there's no need to add tailing slash for line breaks
                buildNewLine()
            }
        }
        buildNewLine()
        append("```")
        buildNewLine()
    }

    override fun StringBuilder.buildCodeInline(code: ContentCodeInline, pageContext: ContentPage) {
        append("`")
        code.children.filterIsInstance<ContentText>().forEach { append(it.text) }
        append("`")
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
        buildLink(locationProvider.resolve(to, from)!!) {
            append(to.name)
        }

    override suspend fun renderPage(page: PageNode) {
        val path by lazy {
            locationProvider.resolve(page, skipExtension = true)
                ?: throw DokkaException("Cannot resolve path for ${page.name}")
        }

        return when (page) {
            is ContentPage -> outputWriter.write(path, buildPage(page) { c, p -> buildPageContent(c, p) }, ".md")
            is RendererSpecificPage -> when (val strategy = page.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, path)
                is RenderingStrategy.Write -> outputWriter.write(path, strategy.text, "")
                is RenderingStrategy.Callback -> outputWriter.write(path, strategy.instructions(this, page), ".md")
                is RenderingStrategy.DriLocationResolvableWrite -> outputWriter.write(
                    path,
                    strategy.contentToResolve { dri, sourcesets ->
                        locationProvider.resolve(dri, sourcesets)
                    },
                    ""
                )
                is RenderingStrategy.PageLocationResolvableWrite -> outputWriter.write(
                    path,
                    strategy.contentToResolve { pageToLocate, context ->
                        locationProvider.resolve(pageToLocate, context)
                    },
                    ""
                )
                RenderingStrategy.DoNothing -> Unit
            }
            else -> throw AssertionError(
                "Page ${page.name} cannot be rendered by renderer as it is not renderer specific nor contains content"
            )
        }
    }

    private fun List<Pair<ContentDivergentInstance, DisplaySourceSet>>.getInstanceAndSourceSets() =
        this.let { Pair(it.first().first, it.map { it.second }.toSet()) }

    private fun StringBuilder.buildSourceSetTags(sourceSets: Set<DisplaySourceSet>) =
        append(sourceSets.joinToString(prefix = "[", postfix = "]") { it.name })
}
