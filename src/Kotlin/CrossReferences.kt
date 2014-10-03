package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.ClassKind

/**
 * Generates cross-references for documentation such as extensions for a type
 *
 * $receiver: [DocumentationContext] for node/descriptor resolutions
 * $node: [DocumentationNode] to visit
 */
public fun DocumentationContext.buildCrossReferences(node: DocumentationNode) {
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
    node.details(DocumentationNode.Kind.Type).forEach { detail ->
        val descriptor = relations[detail]
        if (descriptor != null) {
            val typeNode = descriptorToNode[descriptor]
            if (typeNode != null) {
                // if typeNode is null, type is external to module
                detail.addReferenceTo(typeNode, DocumentationReference.Kind.Link)
            }
        }
    }

    for (member in node.members) {
        buildCrossReferences(member)
    }
}

