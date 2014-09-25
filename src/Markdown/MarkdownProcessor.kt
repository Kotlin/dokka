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
    public fun dump(): String {
        val sb = StringBuilder()
        visit(sb, "", structure.getRoot(), structure, text)
        return sb.toString()
    }
}

fun markdownToHtml(markdown : String) : String {
    return MarkdownProcessor().parse(markdown).dump()
}


fun visit(sb: StringBuilder, indent: String, node: LighterASTNode, structure: FlyweightCapableTreeStructure<LighterASTNode>, markdown: String) {
    sb.append(indent)
    sb.append(node.getTokenType().toString())
    val nodeText = markdown.substring(node.getStartOffset(), node.getEndOffset())
    sb.append(":" + nodeText.replace("\n","\u23CE"))
    sb.appendln()
    val ref = Ref.create<Array<LighterASTNode>?>()
    val count = structure.getChildren(node, ref)
    val children = ref.get()
    if (children == null)
        return
    for (index in 0..count - 1) {
        val child = children[index]
        visit(sb, indent + "  ", child, structure, markdown)
    }
}