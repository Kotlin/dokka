package org.jetbrains.dokka

import org.jetbrains.markdown.MarkdownElementTypes
import java.util.ArrayDeque

public fun MarkdownTree.toContent(): Content {
    val nodeStack = ArrayDeque<ContentNode>()
    nodeStack.push(Content())

    visit {(node, text, processChildren) ->
        val parent = nodeStack.peek()!!
        val nodeType = node.getTokenType()
        val nodeText = getNodeText(node)
        when (nodeType) {
            MarkdownElementTypes.BULLET_LIST -> {
                nodeStack.push(ContentList())
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.ORDERED_LIST -> {
                nodeStack.push(ContentList()) // TODO: add list kind
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.HORIZONTAL_RULE -> {
            }
            MarkdownElementTypes.LIST_BLOCK -> {
                nodeStack.push(ContentBlock())
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.EMPH -> {
                nodeStack.push(ContentEmphasis())
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.STRONG -> {
                nodeStack.push(ContentStrong())
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.ANONYMOUS_SECTION -> {
                nodeStack.push(ContentSection(""))
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.NAMED_SECTION -> {
                val label = findChildByType(node, MarkdownElementTypes.SECTION_NAME)?.let { getNodeText(it) } ?: ""
                nodeStack.push(ContentSection(label))
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.PLAIN_TEXT -> {
                nodeStack.push(ContentText(nodeText))
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.END_LINE -> {
                nodeStack.push(ContentText(nodeText))
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            MarkdownElementTypes.BLANK_LINE -> {
                processChildren()
            }
            MarkdownElementTypes.PARA -> {
                nodeStack.push(ContentBlock())
                processChildren()
                parent.children.add(nodeStack.pop())
            }
            else -> {
                processChildren()
            }
        }
    }
    return nodeStack.pop() as Content
}


