package org.jetbrains.dokka

import java.util.*

public open class DocumentationNode(val name: String,
                                    content: Content,
                                    val kind: DocumentationNode.Kind) {

    private val references = LinkedHashSet<DocumentationReference>()

    var content: Content = content
        private set

    public val summary: ContentNode get() = content.summary

    public val owner: DocumentationNode?
        get() = references(DocumentationReference.Kind.Owner).singleOrNull()?.to
    public val details: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Detail).map { it.to }
    public val members: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Member).map { it.to }
    public val inheritedMembers: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.InheritedMember).map { it.to }
    public val extensions: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Extension).map { it.to }
    public val inheritors: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Inheritor).map { it.to }
    public val overrides: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Override).map { it.to }
    public val links: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Link).map { it.to }
    public val hiddenLinks: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.HiddenLink).map { it.to }
    public val annotations: List<DocumentationNode>
        get() = references(DocumentationReference.Kind.Annotation).map { it.to }
    public val deprecation: DocumentationNode?
        get() = references(DocumentationReference.Kind.Deprecation).singleOrNull()?.to

    // TODO: Should we allow node mutation? Model merge will copy by ref, so references are transparent, which could nice
    public fun addReferenceTo(to: DocumentationNode, kind: DocumentationReference.Kind) {
        references.add(DocumentationReference(this, to, kind))
    }

    public fun addAllReferencesFrom(other: DocumentationNode) {
        references.addAll(other.references)
    }

    public fun updateContent(body: MutableContent.() -> Unit) {
        if (content !is MutableContent) {
            content = MutableContent()
        }
        (content as MutableContent).body()
    }

    public fun details(kind: DocumentationNode.Kind): List<DocumentationNode> = details.filter { it.kind == kind }
    public fun members(kind: DocumentationNode.Kind): List<DocumentationNode> = members.filter { it.kind == kind }
    public fun inheritedMembers(kind: DocumentationNode.Kind): List<DocumentationNode> = inheritedMembers.filter { it.kind == kind }
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
        Unknown,

        Package,
        Class,
        Interface,
        Enum,
        AnnotationClass,
        EnumItem,
        Object,

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
        Exception,

        Modifier,
        NullabilityModifier,

        Module,

        ExternalClass,
        Annotation,

        Value,

        SourceUrl,
        SourcePosition,

        /**
         * A note which is rendered once on a page documenting a group of overloaded functions.
         * Needs to be generated equally on all overloads.
         */
        OverloadGroupNote;

        companion object {
            val classLike = setOf(Class, Interface, Enum, AnnotationClass, Object)
        }
    }
}

public class DocumentationModule(name: String, content: Content = Content.Empty)
    : DocumentationNode(name, content, DocumentationNode.Kind.Module) {
}

val DocumentationNode.path: List<DocumentationNode>
    get() {
        val parent = owner ?: return listOf(this)
        return parent.path + this
    }

fun DocumentationNode.findOrCreatePackageNode(packageName: String, packageContent: Map<String, Content>): DocumentationNode {
    val existingNode = members(DocumentationNode.Kind.Package).firstOrNull { it.name  == packageName }
    if (existingNode != null) {
        return existingNode
    }
    val newNode = DocumentationNode(packageName,
            packageContent.getOrElse(packageName) { Content.Empty },
            DocumentationNode.Kind.Package)
    append(newNode, DocumentationReference.Kind.Member)
    return newNode
}

fun DocumentationNode.append(child: DocumentationNode, kind: DocumentationReference.Kind) {
    addReferenceTo(child, kind)
    when (kind) {
        DocumentationReference.Kind.Detail -> child.addReferenceTo(this, DocumentationReference.Kind.Owner)
        DocumentationReference.Kind.Member -> child.addReferenceTo(this, DocumentationReference.Kind.Owner)
        DocumentationReference.Kind.Owner -> child.addReferenceTo(this, DocumentationReference.Kind.Member)
        else -> { /* Do not add any links back for other types */ }
    }
}

fun DocumentationNode.appendTextNode(text: String,
                                     kind: DocumentationNode.Kind,
                                     refKind: DocumentationReference.Kind = DocumentationReference.Kind.Detail) {
    append(DocumentationNode(text, Content.Empty, kind), refKind)
}

fun DocumentationNode.qualifiedName() = path.drop(1).map { it.name }.filter { it.length > 0 }.joinToString(".")
