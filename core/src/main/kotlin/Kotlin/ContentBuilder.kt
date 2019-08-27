package org.jetbrains.dokka

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.html.entities.EntityConverter
import org.intellij.markdown.parser.LinkMap
import java.util.*

class LinkResolver(private val linkMap: LinkMap, private val contentFactory: (String) -> ContentBlock) {
    fun getLinkInfo(refLabel: String) = linkMap.getLinkInfo(refLabel)
    fun resolve(href: String): ContentBlock = contentFactory(href)
}

fun buildContent(tree: MarkdownNode, linkResolver: LinkResolver, inline: Boolean = false): MutableContent {
    val result = MutableContent()
    if (inline) {
        buildInlineContentTo(tree, result, linkResolver)
    } else {
        buildContentTo(tree, result, linkResolver)
    }
    return result
}

fun buildContentTo(tree: MarkdownNode, target: ContentBlock, linkResolver: LinkResolver) {
//    println(tree.toTestString())
    val nodeStack = ArrayDeque<ContentBlock>()
    nodeStack.push(target)

    tree.visit { node, processChildren ->
        val parent = nodeStack.peek()

        fun appendNodeWithChildren(content: ContentBlock) {
            nodeStack.push(content)
            processChildren()
            parent.append(nodeStack.pop())
        }

        when (node.type) {
            MarkdownElementTypes.ATX_1 -> appendNodeWithChildren(ContentHeading(1))
            MarkdownElementTypes.ATX_2 -> appendNodeWithChildren(ContentHeading(2))
            MarkdownElementTypes.ATX_3 -> appendNodeWithChildren(ContentHeading(3))
            MarkdownElementTypes.ATX_4 -> appendNodeWithChildren(ContentHeading(4))
            MarkdownElementTypes.ATX_5 -> appendNodeWithChildren(ContentHeading(5))
            MarkdownElementTypes.ATX_6 -> appendNodeWithChildren(ContentHeading(6))
            MarkdownElementTypes.UNORDERED_LIST -> appendNodeWithChildren(ContentUnorderedList())
            MarkdownElementTypes.ORDERED_LIST -> appendNodeWithChildren(ContentOrderedList())
            MarkdownElementTypes.LIST_ITEM -> appendNodeWithChildren(ContentListItem())
            MarkdownElementTypes.EMPH -> appendNodeWithChildren(ContentEmphasis())
            MarkdownElementTypes.STRONG -> appendNodeWithChildren(ContentStrong())
            MarkdownElementTypes.CODE_SPAN -> {
                val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
                if (startDelimiter != null) {
                    val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
                    val codeSpan = ContentCode().apply { append(ContentText(text)) }
                    parent.append(codeSpan)
                }
            }
            MarkdownElementTypes.CODE_BLOCK,
            MarkdownElementTypes.CODE_FENCE -> {
                val language = node.child(MarkdownTokenTypes.FENCE_LANG)?.text?.trim() ?: ""
                appendNodeWithChildren(ContentBlockCode(language))
            }
            MarkdownElementTypes.PARAGRAPH -> appendNodeWithChildren(ContentParagraph())

            MarkdownElementTypes.INLINE_LINK -> {
                val linkTextNode = node.child(MarkdownElementTypes.LINK_TEXT)
                val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)
                if (linkTextNode != null) {
                    if (destination != null) {
                        val link = ContentExternalLink(destination.text)
                        renderLinkTextTo(linkTextNode, link, linkResolver)
                        parent.append(link)
                    } else {
                        val link = ContentExternalLink(linkTextNode.getLabelText())
                        renderLinkTextTo(linkTextNode, link, linkResolver)
                        parent.append(link)
                    }
                }
            }
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                val labelElement = node.child(MarkdownElementTypes.LINK_LABEL)
                if (labelElement != null) {
                    val linkInfo = linkResolver.getLinkInfo(labelElement.text)
                    val labelText = labelElement.getLabelText()
                    val link = linkInfo?.let { linkResolver.resolve(it.destination.toString()) } ?: linkResolver.resolve(labelText)
                    val linkText = node.child(MarkdownElementTypes.LINK_TEXT)
                    if (linkText != null) {
                        renderLinkTextTo(linkText, link, linkResolver)
                    } else {
                        link.append(ContentText(labelText))
                    }
                    parent.append(link)
                }
            }
            MarkdownTokenTypes.WHITE_SPACE -> {
                // Don't append first space if start of header (it is added during formatting later)
                //                   v
                //               #### Some Heading
                if (nodeStack.peek() !is ContentHeading || node.parent?.children?.first() != node) {
                    parent.append(ContentText(node.text))
                }
            }
            MarkdownTokenTypes.EOL -> {
                if ((keepEol(nodeStack.peek()) && node.parent?.children?.last() != node) ||
                        // Keep extra blank lines when processing lists (affects Markdown formatting)
                        (processingList(nodeStack.peek()) && node.previous?.type == MarkdownTokenTypes.EOL)) {
                    parent.append(ContentText(node.text))
                }
            }

            MarkdownTokenTypes.CODE_LINE -> {
                val content = ContentText(node.text)
                if (parent is ContentBlockCode) {
                    parent.append(content)
                } else {
                    parent.append(ContentBlockCode().apply { append(content) })
                }
            }

            MarkdownTokenTypes.TEXT -> {
                fun createEntityOrText(text: String): ContentNode {
                    if (text == "&amp;" || text == "&quot;" || text == "&lt;" || text == "&gt;") {
                        return ContentEntity(text)
                    }
                    if (text == "&") {
                        return ContentEntity("&amp;")
                    }
                    val decodedText = EntityConverter.replaceEntities(text, true, true)
                    if (decodedText != text) {
                        return ContentEntity(text)
                    }
                    return ContentText(text)
                }

                parent.append(createEntityOrText(node.text))
            }

            MarkdownTokenTypes.EMPH -> {
                val parentNodeType = node.parent?.type
                if (parentNodeType != MarkdownElementTypes.EMPH && parentNodeType != MarkdownElementTypes.STRONG) {
                    parent.append(ContentText(node.text))
                }
            }

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
                parent.append(ContentText(node.text))
            }

            MarkdownElementTypes.LINK_DEFINITION -> {
            }

            else -> {
                processChildren()
            }
        }
    }
}

private fun MarkdownNode.getLabelText() = children.filter { it.type == MarkdownTokenTypes.TEXT || it.type == MarkdownTokenTypes.EMPH || it.type == MarkdownTokenTypes.COLON }.joinToString("") { it.text }

private fun keepEol(node: ContentNode) = node is ContentParagraph || node is ContentSection || node is ContentBlockCode
private fun processingList(node: ContentNode) = node is ContentOrderedList || node is ContentUnorderedList

fun buildInlineContentTo(tree: MarkdownNode, target: ContentBlock, linkResolver: LinkResolver) {
    val inlineContent = tree.children.singleOrNull { it.type == MarkdownElementTypes.PARAGRAPH }?.children ?: listOf(tree)
    inlineContent.forEach {
        buildContentTo(it, target, linkResolver)
    }
}

fun renderLinkTextTo(tree: MarkdownNode, target: ContentBlock, linkResolver: LinkResolver) {
    val linkTextNodes = tree.children.drop(1).dropLast(1)
    linkTextNodes.forEach {
        buildContentTo(it, target, linkResolver)
    }
}
