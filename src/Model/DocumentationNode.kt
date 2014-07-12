package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import java.util.LinkedHashSet


public open class DocumentationNode(val descriptor: DeclarationDescriptor,
                                    val name: String,
                                    val doc: DocumentationContent,
                                    val kind: DocumentationNode.Kind) {

    private val references = LinkedHashSet<DocumentationReference>()

    public val owner: DocumentationNode?
        get() = references(DocumentationReference.Kind.Owner).firstOrNull()?.to // TODO: should be singleOrNull, but bugz!
    public val details: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Detail).map { it.to }
    public val members: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Member).map { it.to }
    public val links: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Link).map { it.to }

    // TODO: Should we allow node mutation? Model merge will copy by ref, so references are transparent, which could nice
    public fun addReferenceTo(to: DocumentationNode, kind: DocumentationReference.Kind) {
        references.add(DocumentationReference(this, to, kind))
    }

    public fun addAllReferencesFrom(other: DocumentationNode) {
        references.addAll(other.references)
    }

    public fun details(kind: DocumentationNode.Kind): List<DocumentationNode> = details.filter { it.kind == kind }
    public fun members(kind: DocumentationNode.Kind): List<DocumentationNode> = members.filter { it.kind == kind }
    public fun links(kind: DocumentationNode.Kind): List<DocumentationNode> = links.filter { it.kind == kind }

    public fun detail(kind: DocumentationNode.Kind): DocumentationNode = details.filter { it.kind == kind }.single()
    public fun member(kind: DocumentationNode.Kind): DocumentationNode = members.filter { it.kind == kind }.single()
    public fun link(kind: DocumentationNode.Kind): DocumentationNode = links.filter { it.kind == kind }.single()

    public fun references(kind: DocumentationReference.Kind): List<DocumentationReference> = references.filter { it.kind == kind }
    public fun allReferences(): Set<DocumentationReference> = references

    public override fun toString(): String {
        return "$kind:$name"
    }

    public enum class Kind {
        Unknown

        Package
        Class
        Interface
        Object

        Constructor
        Function
        Property

        Parameter
        Receiver
        TypeParameter
        Type
        UpperBound
        LowerBound
        Exception

        Modifier

        Module
    }

}


