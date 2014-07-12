package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.*

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
    Type
    UpperBound
    LowerBound
    Exception

    Module
}

public enum class DocumentationReferenceKind {
    Owner
    Member
    Detail
    Link
    Override
}

public open class DocumentationNode(val descriptor: DeclarationDescriptor,
                                    val name: String,
                                    val doc: DocumentationContent,
                                    val kind: DocumentationNodeKind) {

    private val references = arrayListOf<DocumentationReference>()

    public val owner: DocumentationNode?
        get() = references(DocumentationReferenceKind.Owner).firstOrNull()?.to // TODO: should be singleOrNull, but bugz!
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
    public fun allReferences(): List<DocumentationReference> = references

    public override fun toString(): String {
        return "$kind:$name"
    }
}

public class DocumentationModule(val module: ModuleDescriptor) : DocumentationNode(module, "model", DocumentationContent.Empty, DocumentationNodeKind.Module) {
    fun merge(other: DocumentationModule): DocumentationModule {
        val model = DocumentationModule(module)
        model.addAllReferencesFrom(other)
        model.addAllReferencesFrom(this)
        return model
    }

    public val nodes: List<DocumentationNode>
        get() = members
}

public data class DocumentationReference(val from: DocumentationNode, val to: DocumentationNode, val kind: DocumentationReferenceKind)

fun BindingContext.createDocumentationModel(module: ModuleDescriptor, file: JetFile): DocumentationModule {
    val packageFragment = getPackageFragment(file)
    val model = DocumentationModule(module)
    if (packageFragment == null) throw IllegalArgumentException("File $file should have package fragment")

    val visitor = DocumentationNodeBuilder(this)
    packageFragment.accept(DocumentationBuildingVisitor(this, visitor), model)

    checkResolveChildren(model)

    return model
}
