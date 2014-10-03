package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.name.Name

/**
 * Generates cross-references for documentation such as extensions for a type, inheritors, etc
 *
 * $receiver: [DocumentationContext] for node/descriptor resolutions
 * $node: [DocumentationNode] to visit
 */
public fun DocumentationContext.resolveReferences(node: DocumentationNode) {
    node.details(DocumentationNode.Kind.Receiver).forEach { detail ->
        val receiverType = detail.detail(DocumentationNode.Kind.Type)
        val descriptor = relations[receiverType]
        if (descriptor != null) {
            val typeNode = descriptorToNode[descriptor]
            // if typeNode is null, extension is to external type like in a library
            // should we create dummy node here?
            typeNode?.addReferenceTo(node, DocumentationReference.Kind.Extension)
        }
    }
    node.details(DocumentationNode.Kind.Supertype).forEach { detail ->
        val descriptor = relations[detail]
        if (descriptor != null) {
            val typeNode = descriptorToNode[descriptor]
            typeNode?.addReferenceTo(node, DocumentationReference.Kind.Inheritor)
        }
    }
    node.details.forEach { detail ->
        val descriptor = relations[detail]
        if (descriptor != null) {
            val typeNode = descriptorToNode[descriptor]
            if (typeNode != null) {
                detail.addReferenceTo(typeNode, DocumentationReference.Kind.Link)
            }
        }
    }

    resolveContentLinks(node, node.doc)

    for (child in node.members) {
        resolveReferences(child)
    }
    for (child in node.details) {
        resolveReferences(child)
    }
}

fun DocumentationContext.resolveContentLinks(node: DocumentationNode, content: ContentNode) {
    val snapshot = content.children.toList()
    for (child in snapshot) {
        if (child is ContentExternalLink) {
            val referenceText = child.href
            if (Name.isValidIdentifier(referenceText)) {
                val scope = getResolutionScope(node)
                val symbolName = Name.guess(referenceText)
                val symbol = scope.getLocalVariable(symbolName) ?:
                        scope.getProperties(symbolName).firstOrNull() ?:
                        scope.getFunctions(symbolName).firstOrNull() ?:
                        scope.getClassifier(symbolName)

                if (symbol != null) {
                    val targetNode = descriptorToNode[symbol]
                    val contentLink = if (targetNode != null) ContentNodeLink(targetNode) else ContentExternalLink("#")

                    val index = content.children.indexOf(child)
                    content.children.remove(index)
                    contentLink.children.addAll(child.children)
                    content.children.add(index, contentLink)
                }
            }
        }
        resolveContentLinks(node, child)
    }
}