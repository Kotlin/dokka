package com.jetbrains.dokka

import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.resolve.BindingContext

public enum class DocumentationNodeKind {
    Package
    Class
    Function
    Property
    Parameter
    TypeParameter
    Exception

    Page
    Module
}

public enum class DocumentationReferenceKind {
    Member
    Detail
    Owner
    Link
    Override
}

public class DocumentationNode(val name: String, val doc: String, val kind: DocumentationNodeKind) {
    private val references = arrayListOf<DocumentationReference>()

    public val owner: DocumentationNode
        get() = references(DocumentationReferenceKind.Owner).single().to
    public val details: List<DocumentationNode>
        get() = references(DocumentationReferenceKind.Detail).map { it.to }
    public val members: List<DocumentationNode>
        get() = references(DocumentationReferenceKind.Member).map { it.to }
    public val links: List<DocumentationNode>
        get() = references(DocumentationReferenceKind.Link).map { it.to }

    public fun addReferenceTo(to: DocumentationNode, kind: DocumentationReferenceKind) {
        references.add(DocumentationReference(this, to, kind))
    }

    public fun references(kind: DocumentationReferenceKind): List<DocumentationReference> = references.filter { it.kind == kind }
}

public class DocumentationModel {
    private val items = arrayListOf<DocumentationNode>()

    fun merge(other: DocumentationModel): DocumentationModel {
        val model = DocumentationModel()
        model.addNodes(other.nodes)
        model.addNodes(this.nodes)
        return model
    }

    public fun addNode(node: DocumentationNode) {
        items.add(node)
    }

    public fun addNodes(nodes: List<DocumentationNode>) {
        items.addAll(nodes)
    }

    public val nodes: List<DocumentationNode>
        get() = items
}

public data class DocumentationReference(val from: DocumentationNode, val to: DocumentationNode, val kind: DocumentationReferenceKind)

fun BindingContext.createDocumentation(file: PsiFile): DocumentationModel {
    val model = DocumentationModel()
    model.addNode(DocumentationNode("fn", "doc", DocumentationNodeKind.Function))
    return model
}