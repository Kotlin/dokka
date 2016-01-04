package org.jetbrains.dokka

import com.google.inject.Singleton

data class DocumentationReference(val from: DocumentationNode, val to: DocumentationNode, val kind: DocumentationReference.Kind) {
    enum class Kind {
        Owner,
        Member,
        InheritedMember,
        Detail,
        Link,
        HiddenLink,
        Extension,
        Inheritor,
        Superclass,
        Override,
        Annotation,
        Deprecation,
        TopLevelPage
    }
}

class PendingDocumentationReference(val lazyNodeFrom: () -> DocumentationNode?,
                                    val lazyNodeTo: () -> DocumentationNode?,
                                    val kind: DocumentationReference.Kind) {
    fun resolve() {
        val fromNode = lazyNodeFrom()
        val toNode = lazyNodeTo()
        if (fromNode != null && toNode != null) {
            fromNode.addReferenceTo(toNode, kind)
        }
    }
}

@Singleton
class NodeReferenceGraph() {
    private val nodeMap = hashMapOf<String, DocumentationNode>()
    val references = arrayListOf<PendingDocumentationReference>()

    fun register(signature: String, node: DocumentationNode) {
        nodeMap.put(signature, node)
    }

    fun link(fromNode: DocumentationNode, toSignature: String, kind: DocumentationReference.Kind) {
        references.add(PendingDocumentationReference({ -> fromNode}, { -> nodeMap[toSignature]}, kind))
    }

    fun link(fromSignature: String, toNode: DocumentationNode, kind: DocumentationReference.Kind) {
        references.add(PendingDocumentationReference({ -> nodeMap[fromSignature]}, { -> toNode}, kind))
    }

    fun link(fromSignature: String, toSignature: String, kind: DocumentationReference.Kind) {
        references.add(PendingDocumentationReference({ -> nodeMap[fromSignature]}, { -> nodeMap[toSignature]}, kind))
    }

    fun lookup(signature: String): DocumentationNode? = nodeMap[signature]

    fun resolveReferences() {
        references.forEach { it.resolve() }
    }
}
