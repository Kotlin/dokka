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
    }
}



