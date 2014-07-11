package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile

public enum class DocumentationNodeKind {
    Unknown

    Package
    Class
    Trait
    Object

    Constructor
    Function
    Property

    Parameter
    Receiver
    TypeParameter
    UpperBound
    LowerBound
    Exception

    Model
}

public enum class DocumentationReferenceKind {
    Owner
    Member
    Detail
    Link
    Override
}

public open class DocumentationNode(val name: String,
                                    val doc: String,
                                    val kind: DocumentationNodeKind,
                                    val scope: JetScope) {
    private val references = arrayListOf<DocumentationReference>()

    public val owner: DocumentationNode
        get() = references(DocumentationReferenceKind.Owner).single().to
    public val details: List<DocumentationNode>
        get() = references(DocumentationReferenceKind.Detail).map { it.to }
    public val members: List<DocumentationNode>
        get() = references(DocumentationReferenceKind.Member).map { it.to }
    public val links: List<DocumentationNode>
        get() = references(DocumentationReferenceKind.Link).map { it.to }

    // TODO: Should we allow node mutation? Model merge will copy by ref, so references are transparent, which could nice
    public fun addReferenceTo(to: DocumentationNode, kind: DocumentationReferenceKind) {
        references.add(DocumentationReference(this, to, kind))
    }

    public fun addAllReferencesFrom(other: DocumentationNode) {
        references.addAll(other.references)
    }

    public fun references(kind: DocumentationReferenceKind): List<DocumentationReference> = references.filter { it.kind == kind }

    public override fun toString(): String {
        return "$kind $name"
    }
}

public class DocumentationModel : DocumentationNode("model", "", DocumentationNodeKind.Model, JetScope.EMPTY) {
    fun merge(other: DocumentationModel): DocumentationModel {
        val model = DocumentationModel()
        model.addAllReferencesFrom(other)
        model.addAllReferencesFrom(this)
        return model
    }

    public val nodes: List<DocumentationNode>
        get() = members
}

public data class DocumentationReference(val from: DocumentationNode, val to: DocumentationNode, val kind: DocumentationReferenceKind)

fun BindingContext.createDocumentationModel(file: JetFile): DocumentationModel {
    val model = DocumentationModel()
    val packageFragment = getPackageFragment(file)
    if (packageFragment == null) throw IllegalArgumentException("File $file should have package fragment")

    val visitor = DocumentationNodeBuilder(this)
    packageFragment.accept(DocumentationBuildingVisitor(this, visitor), model)

    return model
}
