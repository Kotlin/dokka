package org.jetbrains.dokka.pages

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.jetbrains.dokka.MarkdownNode
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext

class DefaultMarkdownToContentConverter(
    private val context: DokkaContext
) : MarkdownToContentConverter {
    override fun buildContent(
        node: MarkdownNode,
        dci: DCI,
        platforms: Set<PlatformData>,
        links: Map<String, DRI>,
        styles: Set<Style>,
        extras: Set<Extra>

    ): List<ContentNode> {
//    println(tree.toTestString())

        fun buildChildren(node: MarkdownNode, newStyles: Set<Style> = emptySet(), newExtras: Set<Extra> = emptySet()) =
            node.children.flatMap {
                buildContent(it, dci, platforms, links, styles + newStyles, extras + newExtras)
            }.coalesceText(platforms, styles + newStyles, extras + newExtras)

        fun buildHeader(level: Int) =
            ContentHeader(buildChildren(node), level, dci, platforms, styles)

        return when (node.type) {
            MarkdownElementTypes.ATX_1 -> listOf(buildHeader(1))
            MarkdownElementTypes.ATX_2 -> listOf(buildHeader(2))
            MarkdownElementTypes.ATX_3 -> listOf(buildHeader(3))
            MarkdownElementTypes.ATX_4 -> listOf(buildHeader(4))
            MarkdownElementTypes.ATX_5 -> listOf(buildHeader(5))
            MarkdownElementTypes.ATX_6 -> listOf(buildHeader(6))
            MarkdownElementTypes.UNORDERED_LIST -> listOf(
                ContentList(
                    buildChildren(node),
                    false,
                    dci,
                    platforms,
                    styles,
                    extras
                )
            )
            MarkdownElementTypes.ORDERED_LIST -> listOf(
                ContentList(
                    buildChildren(node),
                    true,
                    dci,
                    platforms,
                    styles,
                    extras
                )
            )
            MarkdownElementTypes.LIST_ITEM -> TODO()
            MarkdownElementTypes.STRONG,
            MarkdownTokenTypes.EMPH,
            MarkdownElementTypes.EMPH ->
                buildChildren(node, setOf(TextStyle.Strong))
            // TODO
            MarkdownElementTypes.CODE_SPAN -> TODO()
//            val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
//            if (startDelimiter != null) {
//                val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
//                val codeSpan = ContentCode().apply { append(ContentText(text)) }
//                parent.append(codeSpan)
//            }

            MarkdownElementTypes.CODE_BLOCK,
            MarkdownElementTypes.CODE_FENCE -> {
                val language = node.child(MarkdownTokenTypes.FENCE_LANG)?.text?.trim() ?: ""
                listOf(ContentCode(buildChildren(node), language, dci, platforms, styles, extras)) // TODO
            }
            MarkdownElementTypes.PARAGRAPH -> buildChildren(node, newStyles = setOf(TextStyle.Paragraph))

            MarkdownElementTypes.INLINE_LINK -> {
//            val linkTextNode = node.child(MarkdownElementTypes.LINK_TEXT)
//            val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)
//            if (linkTextNode != null) {
//                if (destination != null) {
//                    val link = ContentExternalLink(destination.text)
//                    renderLinkTextTo(linkTextNode, link, linkResolver)
//                    parent.append(link)
//                } else {
//                    val link = ContentExternalLink(linkTextNode.getLabelText())
//                    renderLinkTextTo(linkTextNode, link, linkResolver)
//                    parent.append(link)
//                }
//            }
                //TODO: Linking!!!
//            ContentLink()
                TODO()
            }
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                val destinationNode = node.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }
                        ?: node.children.first { it.type == MarkdownElementTypes.LINK_LABEL }
                val destination = destinationNode.children.find { it.type == MarkdownTokenTypes.TEXT }?.text
                        ?: destinationNode.text
                links[destination]?.let { dri ->
                    listOf(
                        ContentResolvedLink(
                            buildChildren(node),
                            destination,
                            DCI(dri, ContentKind.Symbol),
                            platforms,
                            styles,
                            extras
                        )
                    )
                } ?: let {
                    context.logger.error("Apparently there is no link resolved for $destination")
                    emptyList<ContentNode>()
                }
            }
            MarkdownTokenTypes.WHITE_SPACE -> {
                // Don't append first space if start of header (it is added during formatting later)
                //                   v
                //               #### Some Heading
//            if (nodeStack.peek() !is ContentHeading || node.parent?.children?.first() != node) {
//                parent.append(ContentText(node.text))
//            }
                listOf(ContentText(" ", dci, platforms, styles, extras))
            }
            MarkdownTokenTypes.EOL -> {
//            if ((keepEol(nodeStack.peek()) && node.parent?.children?.last() != node) ||
//                // Keep extra blank lines when processing lists (affects Markdown formatting)
//                (processingList(nodeStack.peek()) && node.previous?.type == MarkdownTokenTypes.EOL)) {
//                parent.append(ContentText(node.text))
//            }
                listOf(ContentText(" ", dci, platforms, styles, extras))
            }

            MarkdownTokenTypes.CODE_LINE -> {
                listOf(ContentText(node.text, dci, platforms, styles, extras)) // TODO check
//            if (parent is ContentBlockCode) {
//                parent.append(content)
//            } else {
//                parent.append(ContentBlockCode().apply { append(content) })
//            }
            }

            MarkdownTokenTypes.TEXT ->
//            fun createEntityOrText(text: String): ContentNode {
//                if (text == "&amp;" || text == "&quot;" || text == "&lt;" || text == "&gt;") {
//                    return ContentEntity(text)
//                }
//                if (text == "&") {
//                    return ContentEntity("&amp;")
//                }
//                val decodedText = EntityConverter.replaceEntities(text, true, true)
//                if (decodedText != text) {
//                    return ContentEntity(text)
//                }
//                return ContentText(text)
//            }
//
//            parent.append(createEntityOrText(node.text))
                listOf(ContentText(node.text, dci, platforms, styles, extras)) // TODO

            MarkdownTokenTypes.COLON,
            MarkdownTokenTypes.SINGLE_QUOTE,
            MarkdownTokenTypes.DOUBLE_QUOTE,
            MarkdownTokenTypes.LT,
            MarkdownTokenTypes.GT,
            MarkdownTokenTypes.LPAREN,
            MarkdownTokenTypes.RPAREN,
            MarkdownTokenTypes.LBRACKET,
            MarkdownTokenTypes.RBRACKET,
            MarkdownTokenTypes.EXCLAMATION_MARK,
            MarkdownTokenTypes.BACKTICK,
            MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                listOf(ContentText(node.text, dci, platforms, styles, extras))
            }

            MarkdownElementTypes.LINK_DEFINITION -> TODO()

            MarkdownTokenTypes.EMAIL_AUTOLINK ->
                listOf(
                    ContentResolvedLink(
                        listOf(ContentText(node.text, dci, platforms, styles, extras)),
                        "mailto:${node.text}",
                        dci, platforms, styles, extras
                    )
                )

            else -> buildChildren(node)
        }
    }

    private fun Collection<ContentNode>.coalesceText(
        platforms: Set<PlatformData>,
        styles: Set<Style>,
        extras: Set<Extra>
    ) =
        this
            .sliceWhen { prev, next -> prev::class != next::class }
            .flatMap { nodes ->
                when (nodes.first()) {
                    is ContentText -> listOf(
                        ContentText(
                            nodes.joinToString("") { (it as ContentText).text },
                            nodes.first().dci, platforms, styles, extras
                        )
                    )
                    else -> nodes
                }
            }
}

fun <T> Collection<T>.sliceWhen(predicate: (before: T, after: T) -> Boolean): Collection<Collection<T>> {
    val newCollection = mutableListOf<Collection<T>>()
    var currentSlice = mutableListOf<T>()
    for ((prev, next) in this.windowed(2, 1, false)) {
        currentSlice.add(prev)
        if (predicate(prev, next)) {
            newCollection.add(currentSlice)
            currentSlice = mutableListOf<T>()
        }
    }
    if (this.isNotEmpty()) {
        currentSlice.add(this.last())
        newCollection.add(currentSlice)
    }
    return newCollection
}