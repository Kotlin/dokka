package org.jetbrains.dokka

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class DocumentationNodes {

    class Unknown(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Package(name: String) :
        DocumentationNode(name)

    class Class(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val classLike: Boolean = true
    }

    class Interface(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val classLike: Boolean = true
        override val superclassType: DocumentationNode? = null
    }

    class Enum(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val classLike: Boolean = true
    }

    class AnnotationClass(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val classLike: Boolean = true
    }

    class Exception(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val classLike: Boolean = true
    }

    class EnumItem(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val memberLike = true
    }

    class Object(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val classLike: Boolean = true
    }

    class TypeAlias(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Constructor(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val memberLike = true
    }

    class Function(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val memberLike = true
    }

    class Property(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val memberLike = true
    }

    class Field(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val memberLike = true
    }

    class CompanionObjectProperty(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val memberLike = true
    }

    class CompanionObjectFunction(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val memberLike = true
    }

    class Parameter(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Receiver(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class TypeParameter(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Type(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Supertype(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor) {
        override val superclassType: DocumentationNode? =
            (links + listOfNotNull(externalType)).firstOrNull { it.classLike }?.superclassType
    }

    class UpperBound(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class LowerBound(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class TypeAliasUnderlyingType(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class NullabilityModifier(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Module(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class ExternalClass(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Annotation(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Value(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class SourceUrl(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class SourcePosition(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class ExternalLink(name: String) : DocumentationNode(name)

    class QualifiedName(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class Platform(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class AllTypes(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)

    class OverloadGroupNote(name: String, descriptor: DeclarationDescriptor?) :
        DocumentationNode(name, descriptor)
}

abstract class DocumentationNode(
    var name: String,
    val descriptor: DeclarationDescriptor? = null
) {

    private val references = LinkedHashSet<DocumentationReference>()

    open val classLike = false

    open val memberLike = false

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

    open val superclassType: DocumentationNode?
        get() = if (classLike) {
            supertypes.firstOrNull {
                (it.links + listOfNotNull(it.externalType)).any { it.isSuperclassFor(this) }
            }
        } else null

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

    private fun Collection<DocumentationNode>.filterByKind(kind: KClass<out DocumentationNode>) =
        filter { node: DocumentationNode -> node::class == kind }

    private fun Collection<DocumentationNode>.singleByKind(kind: KClass<out DocumentationNode>) =
        single { node: DocumentationNode -> node::class == kind }

    fun details(kind: KClass<out DocumentationNode>) = details.filterByKind(kind)
    fun members(kind: KClass<out DocumentationNode>) = members.filterByKind(kind)
    fun inheritedMembers(kind: KClass<out DocumentationNode>): List<DocumentationNode> =
        inheritedMembers.filterByKind(kind)

    fun inheritedCompanionObjectMembers(kind: KClass<out DocumentationNode>): List<DocumentationNode> =
        inheritedCompanionObjectMembers.filterByKind(kind)

    fun links(kind: KClass<out DocumentationNode>): List<DocumentationNode> = links.filterByKind(kind)

    fun detail(kind: KClass<out DocumentationNode>): DocumentationNode = details.singleByKind(kind)
    fun detailOrNull(kind: KClass<out DocumentationNode>): DocumentationNode? =
        details.singleOrNull { it::class == kind }

    fun member(kind: KClass<out DocumentationNode>): DocumentationNode = members.singleByKind(kind)
    fun link(kind: KClass<out DocumentationNode>): DocumentationNode = links.singleByKind(kind)


    fun references(kind: RefKind): List<DocumentationReference> = references.filter { it.kind == kind }
    fun allReferences(): Set<DocumentationReference> = references

    override fun toString(): String {
        return "${javaClass.name}:$name"
    }
}

fun KClass<out DocumentationNode>.createNode(name: String, descriptor: DeclarationDescriptor? = null) =
    primaryConstructor?.call(name, descriptor)
            ?: throw IllegalArgumentException("Cannot create node of type ${this::class}: invalid primary constructor")

val DocumentationNode.supertypes: List<DocumentationNode>
    get() = details(DocumentationNodes.Supertype::class)

class DocumentationModule(name: String) : DocumentationNode(name)

val DocumentationNode.path: List<DocumentationNode>
    get() {
        val parent = owner ?: return listOf(this)
        return parent.path + this
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

fun DocumentationNode.appendTextNode(
    text: String,
    kind: KClass<out DocumentationNode>,
    descriptor: DeclarationDescriptor? = null,
    refKind: RefKind = RefKind.Detail
) {
    append(kind.createNode(text, descriptor), refKind)
}

fun DocumentationNode.qualifiedName(): String {
    if (this is DocumentationNodes.Type) {
        return qualifiedNameFromType()
    } else if (this is DocumentationNodes.Package) {
        return name
    }
    return path.dropWhile { it is DocumentationNodes.Module }.map { it.name }.filter { it.isNotEmpty() }
        .joinToString(".")
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
    inheritedMembers.groupBy { it.owner!! }.forEach { (node, _) ->
        node.recursiveInheritedMembers(allInheritedMembers)
    }
}

private fun DocumentationNode.isSuperclassFor(node: DocumentationNode): Boolean {
    return when (node) {
        is DocumentationNodes.Object,
        is DocumentationNodes.Class,
        is DocumentationNodes.Enum -> this is DocumentationNodes.Class
        is DocumentationNodes.Exception -> this is DocumentationNodes.Class || this is DocumentationNodes.Exception
        else -> false
    }
}

fun DocumentationNode.classNodeNameWithOuterClass(): String {
    assert(classLike)
    return path.dropWhile { it is DocumentationNodes.Package || it is DocumentationNodes.Module }
        .joinToString(separator = ".") { it.name }
}