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

    Attribute,

    AttributeRef,

    ApiLevel,

    DeprecatedLevel,

    ArtifactId,

    GroupNode;

    companion object {
        val classLike = setOf(Class, Interface, Enum, AnnotationClass, Exception, Object, TypeAlias)
        val memberLike = setOf(Function, Property, Field, Constructor, CompanionObjectFunction, CompanionObjectProperty, EnumItem, Attribute)
    }
}

open class DocumentationNode(val name: String,
                             content: Content,
                             val kind: NodeKind) {

    private val references = LinkedHashSet<DocumentationReference>()

    var content: Content = content
        private set

    val summary: ContentNode get() = content.summary

    val owner: DocumentationNode?
        get() = references(RefKind.Owner).singleOrNull()?.to
    val details: List<DocumentationNode>
        get() = references(RefKind.Detail).map { it.to }
    val members: List<DocumentationNode>
        get() = references(RefKind.Member).map { it.to }.sortedBy { it.name }
    val inheritedMembers: List<DocumentationNode>
        get() = references(RefKind.InheritedMember).map { it.to }
    val allInheritedMembers: List<DocumentationNode>
        get() = recursiveInheritedMembers().sortedBy { it.name }
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
        get() = references(RefKind.Deprecation).map { it.to }.firstOrNull()
    val platforms: List<String>
        get() = references(RefKind.Platform).map { it.to.name }
    val externalType: DocumentationNode?
        get() = references(RefKind.ExternalType).map { it.to }.firstOrNull()
    val apiLevel: DocumentationNode
        get() = detailOrNull(NodeKind.ApiLevel) ?: DocumentationNode("", Content.Empty, NodeKind.ApiLevel)
    val deprecatedLevel: DocumentationNode
        get() = detailOrNull(NodeKind.DeprecatedLevel) ?: DocumentationNode("", Content.Empty, NodeKind.DeprecatedLevel)
    val artifactId: DocumentationNode
        get() = detailOrNull(NodeKind.ArtifactId) ?: DocumentationNode("", Content.Empty, NodeKind.ArtifactId)
    val attributes: List<DocumentationNode>
        get() = details(NodeKind.Attribute).sortedBy { it.attributeRef!!.name }
    val attributeRef: DocumentationNode?
        get() = references(RefKind.AttributeRef).map { it.to }.firstOrNull()
    val relatedAttributes: List<DocumentationNode>
        get() = hiddenLinks(NodeKind.Attribute)
    val supertypes: List<DocumentationNode>
        get() = details(NodeKind.Supertype)
    val signatureName = detailOrNull(NodeKind.Signature)?.name

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
    fun hiddenLinks(kind: NodeKind): List<DocumentationNode> = hiddenLinks.filter { it.kind == kind }
    fun inheritedMembers(kind: NodeKind): List<DocumentationNode> = inheritedMembers.filter { it.kind == kind }
    fun inheritedCompanionObjectMembers(kind: NodeKind): List<DocumentationNode> = inheritedCompanionObjectMembers.filter { it.kind == kind }
    fun links(kind: NodeKind): List<DocumentationNode> = links.filter { it.kind == kind }

    fun detail(kind: NodeKind): DocumentationNode = details.filter { it.kind == kind }.single()
    fun detailOrNull(kind: NodeKind): DocumentationNode? = details.filter { it.kind == kind }.singleOrNull()
    fun member(kind: NodeKind): DocumentationNode = members.filter { it.kind == kind }.single()
    fun link(kind: NodeKind): DocumentationNode = links.filter { it.kind == kind }.single()

    fun references(kind: RefKind): List<DocumentationReference> = references.filter { it.kind == kind }
    fun allReferences(): Set<DocumentationReference> = references

    override fun toString(): String {
        return "$kind:$name"
    }
}

class DocumentationModule(name: String, content: Content = Content.Empty)
    : DocumentationNode(name, content, NodeKind.Module) {
    val nodeRefGraph = NodeReferenceGraph()
}

val DocumentationNode.path: List<DocumentationNode>
    get() {
        val parent = owner ?: return listOf(this)
        return parent.path + this
    }

fun findOrCreatePackageNode(module: DocumentationNode?, packageName: String, packageContent: Map<String, Content>, refGraph: NodeReferenceGraph): DocumentationNode {
    val existingNode = refGraph.lookup(packageName)
    if (existingNode != null) {
        return existingNode
    }
    val newNode = DocumentationNode(packageName,
            packageContent.getOrElse(packageName) { Content.Empty },
            NodeKind.Package)

    refGraph.register(packageName, newNode)
    module?.append(newNode, RefKind.Member)
    return newNode
}

fun DocumentationNode.append(child: DocumentationNode, kind: RefKind) {
    addReferenceTo(child, kind)
    when (kind) {
        RefKind.Detail -> child.addReferenceTo(this, RefKind.Owner)
        RefKind.Member -> child.addReferenceTo(this, RefKind.Owner)
        RefKind.Owner -> child.addReferenceTo(this, RefKind.Member)
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
    return path.drop(1).map { it.name }.filter { it.length > 0 }.joinToString(".")
}

fun DocumentationNode.simpleName() = name.substringAfterLast('.')

private fun DocumentationNode.recursiveInheritedMembers(): List<DocumentationNode> {
    val allInheritedMembers = mutableListOf<DocumentationNode>()
    recursiveInheritedMembers(allInheritedMembers)
    return allInheritedMembers
}

private fun DocumentationNode.recursiveInheritedMembers(allInheritedMembers: MutableList<DocumentationNode>) {
    allInheritedMembers.addAll(inheritedMembers)
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

fun DocumentationNode.deprecatedLevelMessage(): String {
    val kindName = when(kind) {
        NodeKind.Enum -> "enum"
        NodeKind.Interface -> "interface"
        NodeKind.AnnotationClass -> "annotation"
        NodeKind.Exception -> "exception"
        else -> "class"
    }
    return "This $kindName was deprecated in API level ${deprecatedLevel.name}."
}
