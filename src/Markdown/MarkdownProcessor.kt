package org.jetbrains.dokka

import org.intellij.markdown.*
import org.intellij.markdown.ast.*
import org.intellij.markdown.parser.*
import org.intellij.markdown.parser.dialects.commonmark.CommonMarkMarkerProcessor

class MarkdownNode(val node: ASTNode, val parent: MarkdownNode?, val markdown: String) {
    val children: List<MarkdownNode> = node.children.map { MarkdownNode(it, this, markdown) }
    val endOffset: Int get() = node.endOffset
    val startOffset: Int get() = node.startOffset
    val type: IElementType get() = node.type
    val text: String get() = markdown.substring(startOffset, endOffset)
    fun child(type: IElementType): MarkdownNode? = children.firstOrNull { it.type == type }

    override fun toString(): String = present()
}

fun MarkdownNode.visit(action: (MarkdownNode, () -> Unit) -> Unit) {
    action(this) {
        for (child in children) {
            child.visit(action)
        }
    }
}

public fun MarkdownNode.toTestString(): String {
    val sb = StringBuilder()
    var level = 0
    visit {(node, visitChildren) ->
        sb.append(" ".repeat(level * 2))
        node.presentTo(sb)
        level++
        visitChildren()
        level--
    }
    return sb.toString()
}

private fun MarkdownNode.present() = StringBuilder { presentTo(this) }.toString()
private fun MarkdownNode.presentTo(sb: StringBuilder) {
    sb.append(type.toString())
    sb.append(":" + text.replace("\n", "\u23CE"))
    sb.appendln()
}

fun parseMarkdown(markdown: String): MarkdownNode {
    if (markdown.isEmpty())
        return MarkdownNode(LeafASTNode(MarkdownElementTypes.MARKDOWN_FILE, 0, 0), null, markdown)
    return MarkdownNode(MarkdownParser(CommonMarkMarkerProcessor.Factory).buildMarkdownTreeFromString(markdown), null, markdown)
}
