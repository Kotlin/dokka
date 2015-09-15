package org.jetbrains.dokka

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

class MarkdownNode(val node: ASTNode, val parent: MarkdownNode?, val markdown: String) {
    val children: List<MarkdownNode> = node.children.map { MarkdownNode(it, this, markdown) }
    val type: IElementType get() = node.type
    val text: String get() = markdown.substring(node.startOffset, node.endOffset)
    fun child(type: IElementType): MarkdownNode? = children.firstOrNull { it.type == type }

    override fun toString(): String = StringBuilder { presentTo(this) }.toString()
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
    visit { node, visitChildren ->
        sb.append(" ".repeat(level * 2))
        node.presentTo(sb)
        level++
        visitChildren()
        level--
    }
    return sb.toString()
}

private fun MarkdownNode.presentTo(sb: StringBuilder) {
    sb.append(type.toString())
    sb.append(":" + text.replace("\n", "\u23CE"))
    sb.appendln()
}

fun parseMarkdown(markdown: String): MarkdownNode {
    if (markdown.isEmpty())
        return MarkdownNode(LeafASTNode(MarkdownElementTypes.MARKDOWN_FILE, 0, 0), null, markdown)
    return MarkdownNode(MarkdownParser(CommonMarkFlavourDescriptor()).buildMarkdownTreeFromString(markdown), null, markdown)
}
