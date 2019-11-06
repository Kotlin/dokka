package org.jetbrains.dokka.pages

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.jetbrains.dokka.DokkaLogger
import org.jetbrains.dokka.DokkaResolutionFacade
import org.jetbrains.dokka.MarkdownNode
import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.links.DRI
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink

class MarkdownToContentConverter(
    private val resolutionFacade: DokkaResolutionFacade,
    private val logger: DokkaLogger
) {
    fun buildContent(
        node: MarkdownNode,
        dci: DCI,
        documentationNode: DocumentationNode<*>
    ): List<ContentNode> {
//    println(tree.toTestString())

        fun buildChildren(node: MarkdownNode) = node.children.flatMap {
            buildContent(it, dci, documentationNode)
        }.coalesceText()

        return when (node.type) {
            MarkdownElementTypes.ATX_1 -> listOf(ContentHeader(buildChildren(node), 1, dci))
            MarkdownElementTypes.ATX_2 -> listOf(ContentHeader(buildChildren(node), 2, dci))
            MarkdownElementTypes.ATX_3 -> listOf(ContentHeader(buildChildren(node), 3, dci))
            MarkdownElementTypes.ATX_4 -> listOf(ContentHeader(buildChildren(node), 4, dci))
            MarkdownElementTypes.ATX_5 -> listOf(ContentHeader(buildChildren(node), 5, dci))
            MarkdownElementTypes.ATX_6 -> listOf(ContentHeader(buildChildren(node), 6, dci))
            MarkdownElementTypes.UNORDERED_LIST -> listOf(ContentList(buildChildren(node), false, dci))
            MarkdownElementTypes.ORDERED_LIST -> listOf(ContentList(buildChildren(node), true, dci))
            MarkdownElementTypes.LIST_ITEM -> TODO()
            MarkdownElementTypes.EMPH -> listOf(
                ContentStyle(
                    buildChildren(node),
                    Style.Emphasis,
                    dci
                )
            )// TODO
            MarkdownElementTypes.STRONG -> listOf(
                ContentStyle(
                    buildChildren(node),
                    Style.Strong,
                    dci
                )
            ) // TODO
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
                listOf(ContentCode(buildChildren(node).toString(), language, dci)) // TODO
            }
            MarkdownElementTypes.PARAGRAPH -> listOf(
                ContentStyle(
                    buildChildren(node),
                    Style.Paragraph,
                    dci
                )
            ) // TODO

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
                if (documentationNode.descriptors.isNotEmpty()) {
                    val destinationNode = node.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }
                            ?: node.children.first { it.type == MarkdownElementTypes.LINK_LABEL }
                    val destination = destinationNode.children.find { it.type == MarkdownTokenTypes.TEXT }?.text
                            ?: destinationNode.text

                    documentationNode.descriptors.flatMap {
                        resolveKDocLink(
                            resolutionFacade.resolveSession.bindingContext,
                            resolutionFacade,
                            it,
                            null,
                            destination.split('.')
                        )
                    }
                        .firstOrNull()
                        ?.let { ContentLink(destination, DRI.from(it), dci) }
                        .let(::listOfNotNull)
                } else {
                    logger.error("Apparently descriptor for $documentationNode was needed in model")
                    emptyList()
                }
            }
            MarkdownTokenTypes.WHITE_SPACE -> {
                // Don't append first space if start of header (it is added during formatting later)
                //                   v
                //               #### Some Heading
//            if (nodeStack.peek() !is ContentHeading || node.parent?.children?.first() != node) {
//                parent.append(ContentText(node.text))
//            }
                listOf(ContentText(" ", dci))
            }
            MarkdownTokenTypes.EOL -> {
//            if ((keepEol(nodeStack.peek()) && node.parent?.children?.last() != node) ||
//                // Keep extra blank lines when processing lists (affects Markdown formatting)
//                (processingList(nodeStack.peek()) && node.previous?.type == MarkdownTokenTypes.EOL)) {
//                parent.append(ContentText(node.text))
//            }
                listOf(ContentText(" ", dci))
            }

            MarkdownTokenTypes.CODE_LINE -> {
                listOf(ContentText(node.text, dci)) // TODO check
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
                listOf(ContentText(node.text, dci)) // TODO


            MarkdownTokenTypes.EMPH ->
//            val parentNodeType = node.parent?.type
//            if (parentNodeType != MarkdownElementTypes.EMPH && parentNodeType != MarkdownElementTypes.STRONG) {
//                parent.append(ContentText(node.text))
//            }
                listOf(ContentStyle(buildChildren(node), Style.Emphasis, dci)) // TODO

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
                listOf(ContentText(node.text, dci))
            }

            MarkdownElementTypes.LINK_DEFINITION -> TODO()

            MarkdownTokenTypes.EMAIL_AUTOLINK ->
                listOf(ContentResolvedLink(node.text, "mailto:${node.text}", dci))

            else -> buildChildren(node)
        }
    }

    private fun Collection<ContentNode>.coalesceText() =
        this
            .sliceWhen { prev, next -> prev::class != next::class }
            .flatMap { nodes ->
                when (nodes.first()) {
                    is ContentText -> listOf(
                        ContentText(
                            nodes.joinToString("") { (it as ContentText).text },
                            nodes.first().dci
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