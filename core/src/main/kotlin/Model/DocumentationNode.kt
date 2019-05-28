package org.jetbrains.dokka

import java.util.*

enum class NodeKind {
    Unknown,

    Package,
    Class,
    Interface,
    Enum,
    AnnotationClass,
    Exception,
    EnumItem,
    Object,
    TypeAlias,

    Constructor,
    Function,
    Property,
    Field,

    CompanionObjectProperty,
    CompanionObjectFunction,

    Parameter,
    Receiver,
    TypeParameter,
    Type,
    Supertype,
    UpperBound,
    LowerBound,

    TypeAliasUnderlyingType,

    Modifier,
    NullabilityModifier,

    Module,

    ExternalClass,
    Annotation,

    Value,

    SourceUrl,
    SourcePosition,
    Signature,

    ExternalLink,
    QualifiedName,
    Platform,

    AllTypes,

    /**
     * A note which is rendered once on a page documenting a group of overloaded functions.
     * Needs to be generated equally on all overloads.
     */
    OverloadGroupNote,

    GroupNode;

    companion object {
        val classLike = setOf(Class, Interface, Enum, AnnotationClass, Exception, Object, TypeAlias)
        val memberLike = setOf(Function, Property, Field, Constructor, CompanionObjectFunction, CompanionObjectProperty, EnumItem)
    }
}

open class DocumentationNode(val name: String,
                             content: Content,
                             val kind: NodeKind) {

    private val references = LinkedHashSet<DocumentationReference>()

    var content: Content = content
        private set

    val summary: ContentNode get() = when (kind) {
        NodeKind.GroupNode -> this.origins
                .map { it.content }
                .firstOrNull { !it.isEmpty() }
                ?.summary ?: ContentEmpty
        else -> content.summary
    }


    val owner: DocumentationNode?
        get() = references(RefKind.Owner).singleOrNull()?.to
    val details: List<DocumentationNode>
        get() = references(RefKind.Detail).map { it.to }
    val members: List<DocumentationNode>
        get() = references(RefKind.Member).map { it.to }
    val origins: List<DocumentationNode>
        get() = references(RefKind.Origin).map { it.to }

    val inheritedMembers: List<DocumentationNode>
        get() = references(RefKind.InheritedMember).map { it.to }
    val allInheritedMembers: List<DocumentationNode>
        get() = recursiveInheritedMembers()
    val inheritedCompanionObjectMembers: List<DocumentationNode>
        get() = references(RefKind.InheritedCompanionObjectMember).map { it.to }
    val extensions: List<DocumentationNode>
        get() = references(RefKind.Extension).map { it.to }
    val inheritors: List<DocumentationNode>
        get() = references(RefKind.Inheritor).map { it.to }
    val overrides: List<DocumentationNode>
        get() = references(RefKind.Override).map { it.to }
    val links: List<DocumentationNode>
        get() = references(RefKind.Link).map { it.to }
    val hiddenLinks: List<DocumentationNode>
        get() = references(RefKind.HiddenLink).map { it.to }
    val annotations: List<DocumentationNode>
        get() = references(RefKind.Annotation).map { it.to }
    val deprecation: DocumentationNode?
        get() = references(RefKind.Deprecation).singleOrNull()?.to
    val platforms: List<String>
        get() = references(RefKind.Platform).map { it.to.name }
    val externalType: DocumentationNode?
        get() = references(RefKind.ExternalType).map { it.to }.firstOrNull()

    var sinceKotlin: String?
        get() = references(RefKind.SinceKotlin).singleOrNull()?.to?.name
        set(value) {
            dropReferences { it.kind == RefKind.SinceKotlin }
            if (value != null) {
                append(DocumentationNode(value, Content.Empty, NodeKind.Value), RefKind.SinceKotlin)
            }
        }

    val supertypes: List<DocumentationNode>
        get() = details(NodeKind.Supertype)

    val superclassType: DocumentationNode?
        get() = when (kind) {
            NodeKind.Supertype -> {
                (links + listOfNotNull(externalType)).firstOrNull { it.kind in NodeKind.classLike }?.superclassType
            }
            NodeKind.Interface -> null
            in NodeKind.classLike -> supertypes.firstOrNull {
                (it.links + listOfNotNull(it.externalType)).any { it.isSuperclassFor(this) }
            }
            else -> null
        }

    val superclassTypeSequence: Sequence<DocumentationNode>
        get() = generateSequence(superclassType) {
            it.superclassType
        }

    // TODO: Should we allow node mutation? Model merge will copy by ref, so references are transparent, which could nice
    fun addReferenceTo(to: DocumentationNode, kind: RefKind) {
        references.add(DocumentationReference(this, to, kind))
    }

    fun addReference(reference: DocumentationReference) {
        references.add(reference)
    }

    fun dropReferences(predicate: (DocumentationReference) -> Boolean) {
        references.removeAll(predicate)
    }

    fun addAllReferencesFrom(other: DocumentationNode) {
        references.addAll(other.references)
    }

    fun updateContent(body: MutableContent.() -> Unit) {
        if (content !is MutableContent) {
            content = MutableContent()
        }
        (content as MutableContent).body()
    }
    fun details(kind: NodeKind): List<DocumentationNode> = details.filter { it.kind == kind }
    fun members(kind: NodeKind): List<DocumentationNode> = members.filter { it.kind == kind }
    fun inheritedMembers(kind: NodeKind): List<DocumentationNode> = inheritedMembers.filter { it.kind == kind }
    fun inheritedCompanionObjectMembers(kind: NodeKind): List<DocumentationNode> = inheritedCompanionObjectMembers.filter { it.kind == kind }
    fun links(kind: NodeKind): List<DocumentationNode> = links.filter { it.kind == kind }

    fun detail(kind: NodeKind): DocumentationNode = details.single { it.kind == kind }
    fun detailOrNull(kind: NodeKind): DocumentationNode? = details.singleOrNull { it.kind == kind }
    fun member(kind: NodeKind): DocumentationNode = members.single { it.kind == kind }
    fun link(kind: NodeKind): DocumentationNode = links.single { it.kind == kind }


    fun references(kind: RefKind): List<DocumentationReference> = references.filter { it.kind == kind }
    fun allReferences(): Set<DocumentationReference> = references

    override fun toString(): String {
        return "$kind:$name"
    }
}

class DocumentationModule(name: String, content: Content = Content.Empty, val nodeRefGraph: NodeReferenceGraph = NodeReferenceGraph())
    : DocumentationNode(name, content, NodeKind.Module) {

}

val DocumentationNode.path: List<DocumentationNode>
    get() {
        val parent = owner ?: return listOf(this)
        return parent.path + this
    }

fun findOrCreatePackageNode(module: DocumentationNode?, packageName: String, packageContent: Map<String, Content>, refGraph: NodeReferenceGraph): DocumentationNode {
    val node = refGraph.lookup(packageName)  ?: run {
        val newNode = DocumentationNode(
            packageName,
            packageContent.getOrElse(packageName) { Content.Empty },
            NodeKind.Package
        )

        refGraph.register(packageName, newNode)
        newNode
    }
    if (module != null && node !in module.members) {
        module.append(node, RefKind.Member)
    }
    return node
}

fun DocumentationNode.append(child: DocumentationNode, kind: RefKind) {
    addReferenceTo(child, kind)
    when (kind) {
        RefKind.Detail -> child.addReferenceTo(this, RefKind.Owner)
        RefKind.Member -> child.addReferenceTo(this, RefKind.Owner)
        RefKind.Owner -> child.addReferenceTo(this, RefKind.Member)
        RefKind.Origin -> child.addReferenceTo(this, RefKind.Owner)
        else -> { /* Do not add any links back for other types */
        }
    }
}

fun DocumentationNode.appendTextNode(text: String,
                                     kind: NodeKind,
                                     refKind: RefKind = RefKind.Detail) {
    append(DocumentationNode(text, Content.Empty, kind), refKind)
}

fun DocumentationNode.qualifiedName(): String {
    if (kind == NodeKind.Type) {
        return qualifiedNameFromType()
    } else if (kind == NodeKind.Package) {
        return name
    }
    return path.dropWhile { it.kind == NodeKind.Module }.map { it.name }.filter { it.isNotEmpty() }.joinToString(".")
}

fun DocumentationNode.simpleName() = name.substringAfterLast('.')

private fun DocumentationNode.recursiveInheritedMembers(): List<DocumentationNode> {
    val allInheritedMembers = mutableListOf<DocumentationNode>()
    recursiveInheritedMembers(allInheritedMembers)
    return allInheritedMembers
}

private fun DocumentationNode.recursiveInheritedMembers(allInheritedMembers: MutableList<DocumentationNode>) {
    allInheritedMembers.addAll(inheritedMembers)
    System.out.println(allInheritedMembers.size)
    inheritedMembers.groupBy { it.owner!! } .forEach { (node, _) ->
        node.recursiveInheritedMembers(allInheritedMembers)
    }
}

private fun DocumentationNode.isSuperclassFor(node: DocumentationNode): Boolean {
    return when(node.kind) {
        NodeKind.Object, NodeKind.Class, NodeKind.Enum -> kind == NodeKind.Class
        NodeKind.Exception -> kind == NodeKind.Class || kind == NodeKind.Exception
        else -> false
    }
}

fun DocumentationNode.classNodeNameWithOuterClass(): String {
    assert(kind in NodeKind.classLike)
    return path.dropWhile { it.kind == NodeKind.Package || it.kind == NodeKind.Module }.joinToString(separator = ".") { it.name }
}