package org.jetbrains.dokka

import java.util.ArrayDeque
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.name.*
import org.intellij.markdown.*
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetBlockExpression

public fun DocumentationBuilder.buildContent(tree: MarkdownNode): Content {
    val result = Content()
    buildContentTo(tree, result)
    return result
}

public fun DocumentationBuilder.buildContentTo(tree: MarkdownNode, target: ContentNode) {
//    println(tree.toTestString())
    val nodeStack = ArrayDeque<ContentNode>()
    nodeStack.push(target)

    tree.visit {(node, processChildren) ->
        val parent = nodeStack.peek()!!
        when (node.type) {
            MarkdownElementTypes.UNORDERED_LIST -> {
                nodeStack.push(ContentList())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.ORDERED_LIST -> {
                nodeStack.push(ContentList()) // TODO: add list kind
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
            MarkdownTokenTypes.CODE -> {
                nodeStack.push(ContentCode())
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
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
                val label = node.child(MarkdownElementTypes.LINK_LABEL)?.child(MarkdownTokenTypes.TEXT)
                if (label != null) {
                    val link = ContentExternalLink(label.text)
                    link.append(ContentText(label.text))
                    parent.append(link)
                }
            }
            MarkdownTokenTypes.WHITE_SPACE,
            MarkdownTokenTypes.EOL -> {
                if (keepWhitespace(nodeStack.peek()) && node.parent?.children?.last() != node) {
                    nodeStack.push(ContentText(node.text))
                    processChildren()
                    parent.append(nodeStack.pop())
                }
            }
            MarkdownTokenTypes.TEXT -> {
                nodeStack.push(ContentText(node.text))
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.PARAGRAPH -> {
                nodeStack.push(ContentParagraph())
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownTokenTypes.COLON -> {
                parent.append(ContentText(node.text))
            }
            MarkdownTokenTypes.DOUBLE_QUOTE,
            MarkdownTokenTypes.LT,
            MarkdownTokenTypes.GT -> {
                parent.append(ContentText(node.text))
            }
            else -> {
                processChildren()
            }
        }
    }
}

private fun keepWhitespace(node: ContentNode) = node is ContentParagraph || node is ContentSection

public fun DocumentationBuilder.buildInlineContentTo(tree: MarkdownNode, target: ContentNode) {
    val inlineContent = tree.children.firstOrNull { it.type == MarkdownElementTypes.PARAGRAPH }?.children ?: listOf(tree)
    inlineContent.forEach {
        buildContentTo(it, target)
    }
}

fun DocumentationBuilder.functionBody(descriptor: DeclarationDescriptor, functionName: String): ContentNode {
    val scope = getResolutionScope(descriptor)
    val rootPackage = session.getModuleDescriptor().getPackage(FqName.ROOT)!!
    val rootScope = rootPackage.getMemberScope()
    val symbol = resolveInScope(functionName, scope) ?: resolveInScope(functionName, rootScope)
    if (symbol == null)
        return ContentBlockCode().let() { it.append(ContentText("Unresolved: $functionName")); it }
    val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(symbol)
    if (psiElement == null)
        return ContentBlockCode().let() { it.append(ContentText("Source not found: $functionName")); it }

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
    return ContentBlockCode().let() { it.append(ContentText(finalText)); it }
}

private fun DocumentationBuilder.resolveInScope(functionName: String, scope: JetScope): DeclarationDescriptor? {
    var currentScope = scope
    val parts = functionName.split('.')
    var symbol: DeclarationDescriptor? = null

    for (part in parts) {
        // short name
        val symbolName = Name.guess(part)
        val partSymbol = currentScope.getLocalVariable(symbolName) ?:
                currentScope.getProperties(symbolName).firstOrNull() ?:
                currentScope.getFunctions(symbolName).firstOrNull() ?:
                currentScope.getClassifier(symbolName) ?:
                currentScope.getPackage(symbolName)

        if (partSymbol == null) {
            symbol = null
            break
        }
        currentScope = getResolutionScope(partSymbol)
        symbol = partSymbol
    }

    return symbol
}
