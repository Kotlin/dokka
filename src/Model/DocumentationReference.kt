package org.jetbrains.dokka

public data class DocumentationReference(val from: DocumentationNode, val to: DocumentationNode, val kind: DocumentationReference.Kind) {
    public enum class Kind {
        Owner
        Member
        Detail
        Link
        Extension
        Inheritor
        Override
        Annotation
        Deprecation
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
