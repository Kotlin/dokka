package org.jetbrains.dokka.pages

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.jetbrains.dokka.MarkdownNode

class MarkdownToContentConverter {
    fun buildContent(node: MarkdownNode, platforms: List<PlatformData>): List<ContentNode> {
//    println(tree.toTestString())

        fun buildChildren(node: MarkdownNode) = node.children.flatMap { buildContent(it, platforms) }.coalesceText()

        return when (node.type) {
            MarkdownElementTypes.ATX_1 -> listOf(ContentHeader(buildChildren(node), 1, platforms))
            MarkdownElementTypes.ATX_2 -> listOf(ContentHeader(buildChildren(node), 2, platforms))
            MarkdownElementTypes.ATX_3 -> listOf(ContentHeader(buildChildren(node), 3, platforms))
            MarkdownElementTypes.ATX_4 -> listOf(ContentHeader(buildChildren(node), 4, platforms))
            MarkdownElementTypes.ATX_5 -> listOf(ContentHeader(buildChildren(node), 5, platforms))
            MarkdownElementTypes.ATX_6 -> listOf(ContentHeader(buildChildren(node), 6, platforms))
            MarkdownElementTypes.UNORDERED_LIST -> listOf(ContentList(buildChildren(node), false, platforms))
            MarkdownElementTypes.ORDERED_LIST -> listOf(ContentList(buildChildren(node), true, platforms))
            MarkdownElementTypes.LIST_ITEM -> TODO()
            MarkdownElementTypes.EMPH -> listOf(
                ContentStyle(
                    buildChildren(node),
                    Style.Emphasis,
                    platforms
                )
            )// TODO
            MarkdownElementTypes.STRONG -> listOf(
                ContentStyle(
                    buildChildren(node),
                    Style.Strong,
                    platforms
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
                listOf(ContentCode(buildChildren(node).toString(), language, platforms)) // TODO
            }
            MarkdownElementTypes.PARAGRAPH -> listOf(ContentStyle(buildChildren(node), Style.Paragraph, platforms)) // TODO

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
//            val labelElement = node.child(MarkdownElementTypes.LINK_LABEL)
//            if (labelElement != null) {
//                val linkInfo = linkResolver.getLinkInfo(labelElement.text)
//                val labelText = labelElement.getLabelText()
//                val link =
//                    linkInfo?.let { linkResolver.resolve(it.destination.toString()) } ?: linkResolver.resolve(
//                        labelText
//                    )
//                val linkText = node.child(MarkdownElementTypes.LINK_TEXT)
//                if (linkText != null) {
//                    renderLinkTextTo(linkText, link, linkResolver)
//                } else {
//                    link.append(ContentText(labelText))
//                }
//                parent.append(link)
//            }
                TODO()
            }
            MarkdownTokenTypes.WHITE_SPACE -> {
                // Don't append first space if start of header (it is added during formatting later)
                //                   v
                //               #### Some Heading
//            if (nodeStack.peek() !is ContentHeading || node.parent?.children?.first() != node) {
//                parent.append(ContentText(node.text))
//            }
                listOf(ContentText(" ", platforms))
            }
            MarkdownTokenTypes.EOL -> {
//            if ((keepEol(nodeStack.peek()) && node.parent?.children?.last() != node) ||
//                // Keep extra blank lines when processing lists (affects Markdown formatting)
//                (processingList(nodeStack.peek()) && node.previous?.type == MarkdownTokenTypes.EOL)) {
//                parent.append(ContentText(node.text))
//            }
                emptyList()
            }

            MarkdownTokenTypes.CODE_LINE -> {
                listOf(ContentText(node.text, platforms)) // TODO check
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
                listOf(ContentText(node.text, platforms)) // TODO


            MarkdownTokenTypes.EMPH ->
//            val parentNodeType = node.parent?.type
//            if (parentNodeType != MarkdownElementTypes.EMPH && parentNodeType != MarkdownElementTypes.STRONG) {
//                parent.append(ContentText(node.text))
//            }
                listOf(ContentStyle(buildChildren(node), Style.Emphasis, platforms)) // TODO

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
                listOf(ContentText(node.text, platforms))
            }

            MarkdownElementTypes.LINK_DEFINITION -> TODO()

            MarkdownTokenTypes.EMAIL_AUTOLINK ->
                listOf(ContentResolvedLink(node.text, "mailto:${node.text}", platforms))

            else -> buildChildren(node)
        }
    }

    private fun Collection<ContentNode>.coalesceText() =
        this
            .sliceWhen { prev, next -> prev::class != next::class }
            .flatMap { nodes ->
                when (nodes.first()) {
                    is ContentText -> listOf(ContentText(nodes.joinToString("") { (it as ContentText).text }, nodes.first().platforms))
                    else -> nodes
                }
            }
}

fun <T> Collection<T>.sliceWhen(predicate: (before: T, after: T)->Boolean): Collection<Collection<T>> {
    val newCollection = mutableListOf<Collection<T>>()
    var currentSlice = mutableListOf<T>()
    for ((prev, next) in this.windowed(2, 1, false)) {
        currentSlice.add(prev)
        if(predicate(prev, next)) {
            newCollection.add(currentSlice)
            currentSlice = mutableListOf<T>()
        }
    }
    if(this.isNotEmpty()) {
        currentSlice.add(this.last())
        newCollection.add(currentSlice)
    }
    return newCollection
}