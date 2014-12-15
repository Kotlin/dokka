package org.jetbrains.dokka

import java.util.ArrayDeque
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.scopes.*
import org.jetbrains.jet.lang.resolve.name.*
import net.nicoulaj.idea.markdown.lang.*

public fun DocumentationBuilder.buildContent(tree: MarkdownNode, descriptor: DeclarationDescriptor): Content {
    val nodeStack = ArrayDeque<ContentNode>()
    nodeStack.push(Content())

    tree.visit {(node, processChildren) ->
        val parent = nodeStack.peek()!!
        val nodeType = node.type
        val nodeText = tree.text
        when (nodeType) {
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
                nodeStack.push(ContentBlock())
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
        /*            MarkdownElementTypes.ANONYMOUS_SECTION -> {
                        nodeStack.push(ContentSection(""))
                        processChildren()
                        parent.append(nodeStack.pop())
                    }
                    MarkdownElementTypes.DIRECTIVE -> {
                        val name = tree.findChildByType(node, MarkdownElementTypes.DIRECTIVE_NAME)?.let { tree.getNodeText(it) } ?: ""
                        val params = tree.findChildByType(node, MarkdownElementTypes.DIRECTIVE_PARAMS)?.let { tree.getNodeText(it) } ?: ""
                        when (name) {
                            "code" -> parent.append(functionBody(descriptor, params))
                        }
                    }
                    MarkdownElementTypes.NAMED_SECTION -> {
                        val label = tree.findChildByType(node, MarkdownElementTypes.SECTION_NAME)?.let { tree.getNodeText(it) } ?: ""
                        nodeStack.push(ContentSection(label))
                        processChildren()
                        parent.append(nodeStack.pop())
                    }*/
            MarkdownElementTypes.INLINE_LINK -> {
                val target = node.child(MarkdownElementTypes.LINK_TITLE)?.let { it.text } ?: ""
                val href = node.child(MarkdownElementTypes.LINK_DESTINATION)?.let { it.text }
                val link = if (href != null) ContentExternalLink(href) else ContentExternalLink(target)
                link.append(ContentText(target))
                parent.append(link)
            }
            MarkdownTokenTypes.TEXT -> {
                nodeStack.push(ContentText(nodeText))
                processChildren()
                parent.append(nodeStack.pop())
            }
            MarkdownElementTypes.PARAGRAPH -> {
                nodeStack.push(ContentParagraph())
                processChildren()
                parent.append(nodeStack.pop())
            }
            else -> {
                processChildren()
            }
        }
    }
    return nodeStack.pop() as Content
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

    return ContentBlockCode().let() { it.append(ContentText(psiElement.getText())); it }
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