package org.jetbrains.dokka

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.html.entities.EntityConverter
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.kdoc.getResolutionScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.utils.asJetScope
import java.util.*

public fun buildContent(tree: MarkdownNode, linkResolver: (String) -> ContentBlock): MutableContent {
    val result = MutableContent()
    buildContentTo(tree, result, linkResolver)
    return result
}

public fun buildContentTo(tree: MarkdownNode, target: ContentBlock, linkResolver: (String) -> ContentBlock) {
//    println(tree.toTestString())
    val nodeStack = ArrayDeque<ContentBlock>()
    nodeStack.push(target)

    tree.visit {node, processChildren ->
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
            MarkdownElementTypes.LIST_ITEM ->  appendNodeWithChildren(ContentListItem())
            MarkdownElementTypes.EMPH -> appendNodeWithChildren(ContentEmphasis())
            MarkdownElementTypes.STRONG -> appendNodeWithChildren(ContentStrong())
            MarkdownElementTypes.CODE_SPAN -> appendNodeWithChildren(ContentCode())
            MarkdownElementTypes.CODE_BLOCK,
            MarkdownElementTypes.CODE_FENCE -> appendNodeWithChildren(ContentBlockCode())
            MarkdownElementTypes.PARAGRAPH -> appendNodeWithChildren(ContentParagraph())

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

            MarkdownTokenTypes.COLON,
            MarkdownTokenTypes.DOUBLE_QUOTE,
            MarkdownTokenTypes.LT,
            MarkdownTokenTypes.GT,
            MarkdownTokenTypes.LPAREN,
            MarkdownTokenTypes.RPAREN,
            MarkdownTokenTypes.LBRACKET,
            MarkdownTokenTypes.RBRACKET,
            MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
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
    val scope = getResolutionScope(resolutionFacade, descriptor).asJetScope()
    val rootPackage = resolutionFacade.moduleDescriptor.getPackage(FqName.ROOT)
    val rootScope = rootPackage.memberScope
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
            val bodyExpression = psiElement.bodyExpression
            when (bodyExpression) {
                is JetBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                else -> bodyExpression!!.text
            }
        }
        else -> psiElement.text
    }

    val lines = text.trimEnd().split("\n".toRegex()).toTypedArray().filterNot { it.length() == 0 }
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
        val partSymbol = currentScope.getAllDescriptors().filter { it.name == symbolName }.firstOrNull()

        if (partSymbol == null) {
            symbol = null
            break
        }
        currentScope = if (partSymbol is ClassDescriptor)
            partSymbol.defaultType.memberScope
        else
            getResolutionScope(resolutionFacade, partSymbol).asJetScope()
        symbol = partSymbol
    }

    return symbol
}
