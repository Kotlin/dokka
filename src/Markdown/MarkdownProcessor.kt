package org.jetbrains.dokka

import org.jetbrains.markdown.*
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.tree.TokenSet
import com.intellij.lang.Language
import com.intellij.psi.tree.IFileElementType
import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import com.intellij.openapi.util.Ref
import org.jetbrains.markdown.lexer.MarkdownLexer

public class MarkdownProcessor {
    class object {
        val EXPR_LANGUAGE = object : Language("MARKDOWN") {}
        val DOCUMENT = IFileElementType("DOCUMENT", EXPR_LANGUAGE);
    }

    public fun parse(markdown: String): MarkdownTree {
        val parser = MarkdownParser()
        val builder = PsiBuilderImpl(null, null, TokenSet.EMPTY, TokenSet.EMPTY, MarkdownLexer(), null, markdown, null, null)
        parser.parse_only_(DOCUMENT, builder)
        val light = builder.getLightTree()!!
        return MarkdownTree(markdown, light)
    }
}

public class MarkdownTree(private val text: String, private val structure: FlyweightCapableTreeStructure<LighterASTNode>) {
    fun visit(action: (LighterASTNode, String, visitChildren: () -> Unit) -> Unit) {
        visit(structure.getRoot(), action)
    }

    fun visit(node: LighterASTNode, action: (LighterASTNode, String, visitChildren: () -> Unit) -> Unit) {
        action(node, text) {
            val ref = Ref.create<Array<LighterASTNode>?>()
            val count = structure.getChildren(node, ref)
            val children = ref.get()
            if (children != null) {
                for (index in 0..count - 1) {
                    val child = children[index]
                    visit(child, action)
                }
            }
        }
    }

}

public fun MarkdownTree.dump(): String {
    val sb = StringBuilder()
    var level = 0
    visit {(node, text, visitChildren) ->
        val nodeText = text.substring(node.getStartOffset(), node.getEndOffset())
        sb.append(" ".repeat(level * 2))
        sb.append(node.getTokenType().toString())
        sb.append(":" + nodeText.replace("\n", "\u23CE"))
        sb.appendln()
        level++
        visitChildren()
        level--
    }
    return sb.toString()
}

public fun MarkdownTree.toHtml(): String {
    val sb = StringBuilder()
    var level = 0
    visit {(node, text, processChildren) ->
        val nodeType = node.getTokenType()
        val nodeText = text.substring(node.getStartOffset(), node.getEndOffset())
        val indent = " ".repeat(level * 2)
        when (nodeType) {
            MarkdownElementTypes.BULLET_LIST -> {
                sb.appendln("$indent<ul>")
                level++
                processChildren()
                level--
                sb.appendln("$indent</ul>")
            }
            MarkdownElementTypes.HORIZONTAL_RULE -> {
                sb.appendln("$indent<hr/>")
            }
            MarkdownElementTypes.ORDERED_LIST -> {
                sb.appendln("$indent<ol>")
                processChildren()
                sb.appendln("$indent</ol>")
            }
            MarkdownElementTypes.LIST_BLOCK -> {
                sb.append("$indent<li>")
                processChildren()
                sb.appendln("</li>")
            }
            MarkdownElementTypes.EMPH -> {
                sb.append("<em>")
                processChildren()
                sb.append("</em>")
            }
            MarkdownElementTypes.STRONG -> {
                sb.append("<strong>")
                processChildren()
                sb.append("</strong>")
            }
            MarkdownElementTypes.PLAIN_TEXT -> {
                sb.append(nodeText)
            }
            MarkdownElementTypes.END_LINE -> {
                sb.appendln()
            }
            MarkdownElementTypes.BLANK_LINE -> {
                sb.appendln()
            }
            MarkdownElementTypes.PARA -> {
                sb.appendln("$indent<p>")
                processChildren()
                sb.appendln("$indent</p>")
            }
            MarkdownElementTypes.VERBATIM -> {
                sb.appendln("$indent<pre><code>")
                processChildren()
                sb.appendln("$indent</code></pre>")
            }
            else -> {
                processChildren()
            }
        }
    }
    return sb.toString()
}


fun markdownToHtml(markdown: String): String {
    val markdownTree = MarkdownProcessor().parse(markdown)
    val ast = markdownTree.dump()
    return markdownTree.toHtml()
}

