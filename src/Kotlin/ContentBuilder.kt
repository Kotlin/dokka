package org.jetbrains.dokka

import java.util.ArrayDeque
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.idea.kdoc.getResolutionScope
import org.intellij.markdown.*
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetBlockExpression

public fun buildContent(tree: MarkdownNode, linkResolver: (String) -> ContentBlock): MutableContent {
    val result = MutableContent()
    buildContentTo(tree, result, linkResolver)
    return result
}

public fun buildContentTo(tree: MarkdownNode, target: ContentBlock, linkResolver: (String) -> ContentBlock) {
//    println(tree.toTestString())
    val nodeStack = ArrayDeque<ContentBlock>()
    nodeStack.push(target)

    tree.visit {(node, processChildren) ->
        val parent = nodeStack.peek()!!
        when (node.type) {
            MarkdownElementTypes.UNORDERED_LIST -> {
                nodeStack.push(ContentUnorderedList())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.ORDERED_LIST -> {
                nodeStack.push(ContentOrderedList())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.LIST_ITEM -> {
                nodeStack.push(ContentListItem())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.EMPH -> {
                nodeStack.push(ContentEmphasis())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.STRONG -> {
                nodeStack.push(ContentStrong())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.CODE_SPAN -> {
                nodeStack.push(ContentCode())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.CODE_BLOCK -> {
                nodeStack.push(ContentBlockCode())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.INLINE_LINK -> {
                val label = node.child(MarkdownElementTypes.LINK_TEXT)?.child(MarkdownTokenTypes.TEXT)
                val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)
                if (label != null) {
                    if (destination != null) {
                        val link = ContentExternalLink(destination.text)
                        link.append(ContentText(label.text))
                        parent.append(link)
                    } else {
                        val link = ContentExternalLink(label.text)
                        link.append(ContentText(label.text))
                        parent.append(link)
                    }
                }
            }
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                val label = node.child(MarkdownElementTypes.LINK_LABEL)?.child(MarkdownTokenTypes.TEXT)
                if (label != null) {
                    val link = linkResolver(label.text)
                    val linkText = node.child(MarkdownElementTypes.LINK_TEXT)?.child(MarkdownTokenTypes.TEXT)
                    link.append(ContentText(linkText?.text ?: label.text))
                    parent.append(link)
                }
            }
            MarkdownTokenTypes.WHITE_SPACE,
            MarkdownTokenTypes.EOL -> {
                if (keepWhitespace(nodeStack.peek()) && node.parent?.children?.last() != node) {
                    parent.append(ContentText(node.text))
                }
            }

            MarkdownTokenTypes.CODE -> {
                val block = ContentBlockCode()
                block.append(ContentText(node.text))
                parent.append(block)
            }
            MarkdownElementTypes.PARAGRAPH -> {
                nodeStack.push(ContentParagraph())
                processChildren()
                parent.append(nodeStack.pop())
            }

            MarkdownTokenTypes.HTML_ENTITY -> {
                parent.append(ContentEntity(node.text))
            }

            MarkdownTokenTypes.TEXT,
            MarkdownTokenTypes.COLON,
            MarkdownTokenTypes.DOUBLE_QUOTE,
            MarkdownTokenTypes.LT,
            MarkdownTokenTypes.GT,
            MarkdownTokenTypes.LPAREN,
            MarkdownTokenTypes.RPAREN,
            MarkdownTokenTypes.LBRACKET,
            MarkdownTokenTypes.RBRACKET -> {
                parent.append(ContentText(node.text))
            }
            else -> {
                processChildren()
            }
        }
    }
}

private fun keepWhitespace(node: ContentNode) = node is ContentParagraph || node is ContentSection

public fun buildInlineContentTo(tree: MarkdownNode, target: ContentBlock, linkResolver: (String) -> ContentBlock) {
    val inlineContent = tree.children.singleOrNull { it.type == MarkdownElementTypes.PARAGRAPH }?.children ?: listOf(tree)
    inlineContent.forEach {
        buildContentTo(it, target, linkResolver)
    }
}

fun DocumentationBuilder.functionBody(descriptor: DeclarationDescriptor, functionName: String?): ContentNode {
    if (functionName == null) {
        logger.warn("Missing function name in @sample in ${descriptor.signature()}")
        return ContentBlockCode().let() { it.append(ContentText("Missing function name in @sample")); it }
    }
    val scope = getResolutionScope(session, descriptor)
    val rootPackage = session.getModuleDescriptor().getPackage(FqName.ROOT)!!
    val rootScope = rootPackage.getMemberScope()
    val symbol = resolveInScope(functionName, scope) ?: resolveInScope(functionName, rootScope)
    if (symbol == null) {
        logger.warn("Unresolved function $functionName in @sample in ${descriptor.signature()}")
        return ContentBlockCode().let() { it.append(ContentText("Unresolved: $functionName")); it }
    }
    val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(symbol)
    if (psiElement == null) {
        logger.warn("Can't find source for function $functionName in @sample in ${descriptor.signature()}")
        return ContentBlockCode().let() { it.append(ContentText("Source not found: $functionName")); it }
    }

    val text = when (psiElement) {
        is JetDeclarationWithBody -> ContentBlockCode().let() {
            val bodyExpression = psiElement.getBodyExpression()
            when (bodyExpression) {
                is JetBlockExpression -> bodyExpression.getText().trim("{", "}")
                else -> bodyExpression.getText()
            }
        }
        else -> psiElement.getText()
    }

    val lines = text.trimTrailing().split("\n").filterNot { it.length() == 0 }
    val indent = lines.map { it.takeWhile { it.isWhitespace() }.count() }.min() ?: 0
    val finalText = lines.map { it.drop(indent) }.join("\n")
    return ContentBlockCode("kotlin").let() { it.append(ContentText(finalText)); it }
}

private fun DocumentationBuilder.resolveInScope(functionName: String, scope: JetScope): DeclarationDescriptor? {
    var currentScope = scope
    val parts = functionName.split('.')

    var symbol: DeclarationDescriptor? = null

    for (part in parts) {
        // short name
        val symbolName = Name.guess(part)
        val partSymbol = currentScope.getAllDescriptors().filter { it.getName() == symbolName }.firstOrNull()

        if (partSymbol == null) {
            symbol = null
            break
        }
        currentScope = if (partSymbol is ClassDescriptor)
            partSymbol.getDefaultType().getMemberScope()
        else
            getResolutionScope(session, partSymbol)
        symbol = partSymbol
    }

    return symbol
}
