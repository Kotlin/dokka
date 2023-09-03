/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.analysis.kotlin.internal.InheritanceBuilder
import org.jetbrains.dokka.analysis.kotlin.internal.InheritanceNode

public interface JavadocPageNode : ContentPage, WithDocumentables

public interface WithJavadocExtra<T : Documentable> : WithExtraProperties<T> {
    override fun withNewExtras(newExtras: PropertyContainer<T>): T =
        throw IllegalStateException("Merging extras is not applicable for javadoc")
}

public fun interface WithNavigable {
    public fun getAllNavigables(): List<NavigableJavadocNode>
}

public interface WithBrief {
    public val brief: List<ContentNode>
}

public class JavadocModulePageNode(
    override val name: String,
    override val content: JavadocContentNode,
    override val children: List<PageNode>,
    override val dri: Set<DRI>,
    override val extra: PropertyContainer<DModule> = PropertyContainer.empty()
) :
    RootPageNode(),
    WithJavadocExtra<DModule>,
    NavigableJavadocNode,
    JavadocPageNode,
    ModulePage {

    override val documentables: List<Documentable> = emptyList()
    override val embeddedResources: List<String> = emptyList()
    override fun modified(name: String, children: List<PageNode>): RootPageNode =
        JavadocModulePageNode(name, content, children, dri, extra)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = JavadocModulePageNode(name, content as JavadocContentNode, children, dri, extra)

    override fun getId(): String = name

    override fun getDRI(): DRI = dri.first()
}

public class JavadocPackagePageNode(
    override val name: String,
    override val content: JavadocContentNode,
    override val dri: Set<DRI>,

    override val documentables: List<Documentable> = emptyList(),
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : JavadocPageNode,
    WithNavigable,
    NavigableJavadocNode,
    PackagePage {

    init {
        require(name.isNotBlank()) { "Empty name is not supported " }
    }

    override fun getAllNavigables(): List<NavigableJavadocNode> =
        children.filterIsInstance<NavigableJavadocNode>().flatMap {
            if (it is WithNavigable) it.getAllNavigables()
            else listOf(it)
        }

    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = JavadocPackagePageNode(
        name,
        content,
        dri,
        documentables,
        children,
        embeddedResources
    )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        JavadocPackagePageNode(
            name,
            content as JavadocContentNode,
            dri,
            documentables,
            children,
            embeddedResources
        )

    override fun getId(): String = name

    override fun getDRI(): DRI = dri.first()
}

public interface NavigableJavadocNode {
    public fun getId(): String
    public fun getDRI(): DRI
}

public sealed class AnchorableJavadocNode(
    public open val name: String,
    public open val dri: DRI
) : NavigableJavadocNode {
    override fun getId(): String = name
    override fun getDRI(): DRI = dri
}

public data class JavadocEntryNode(
    override val dri: DRI,
    override val name: String,
    val signature: JavadocSignatureContentNode,
    override val brief: List<ContentNode>,
    override val extra: PropertyContainer<DEnumEntry> = PropertyContainer.empty()
) : AnchorableJavadocNode(name, dri), WithJavadocExtra<DEnumEntry>, WithBrief

public data class JavadocParameterNode(
    override val dri: DRI,
    override val name: String,
    val type: ContentNode,
    val description: List<ContentNode>,
    val typeBound: Bound,
    override val extra: PropertyContainer<DParameter> = PropertyContainer.empty()
) : AnchorableJavadocNode(name, dri), WithJavadocExtra<DParameter>

public data class JavadocPropertyNode(
    override val dri: DRI,
    override val name: String,
    val signature: JavadocSignatureContentNode,
    override val brief: List<ContentNode>,

    override val extra: PropertyContainer<DProperty> = PropertyContainer.empty()
) : AnchorableJavadocNode(name, dri), WithJavadocExtra<DProperty>, WithBrief

public data class JavadocFunctionNode(
    val signature: JavadocSignatureContentNode,
    override val brief: List<ContentNode>,
    val description: List<ContentNode>,
    val parameters: List<JavadocParameterNode>,

    val returnTagContent: List<ContentNode>,
    val sinceTagContent: List<List<ContentNode>>,

    override val name: String,
    override val dri: DRI,
    override val extra: PropertyContainer<DFunction> = PropertyContainer.empty()
) : AnchorableJavadocNode(name, dri), WithJavadocExtra<DFunction>, WithBrief {
    val isInherited: Boolean
        get() {
            val extra = extra[InheritedMember]
            return extra?.inheritedFrom?.keys?.firstOrNull { it.analysisPlatform == Platform.jvm }?.let { jvm ->
                extra.isInherited(jvm)
            } ?: false
        }
}

public class JavadocClasslikePageNode(
    override val name: String,
    override val content: JavadocContentNode,
    override val dri: Set<DRI>,
    public val signature: JavadocSignatureContentNode,
    public val description: List<ContentNode>,
    public val constructors: List<JavadocFunctionNode>,
    public val methods: List<JavadocFunctionNode>,
    public val entries: List<JavadocEntryNode>,
    public val classlikes: List<JavadocClasslikePageNode>,
    public val properties: List<JavadocPropertyNode>,
    override val brief: List<ContentNode>,

    public val sinceTagContent: List<List<ContentNode>>,
    public val authorTagContent: List<List<ContentNode>>,

    override val documentables: List<Documentable> = emptyList(),
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf(),
    override val extra: PropertyContainer<DClasslike> = PropertyContainer.empty(),
) : JavadocPageNode, WithJavadocExtra<DClasslike>, NavigableJavadocNode, WithNavigable, WithBrief, ClasslikePage {

    override fun getAllNavigables(): List<NavigableJavadocNode> =
        methods + entries + classlikes.map { it.getAllNavigables() }.flatten() + this

    public fun getAnchorables(): List<AnchorableJavadocNode> =
        constructors + methods + entries + properties

    public val kind: String? = documentables.firstOrNull()?.kind()
    public val packageName: String? = dri.first().packageName

    override fun getId(): String = name
    override fun getDRI(): DRI = dri.first()

    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = JavadocClasslikePageNode(
        name,
        content,
        dri,
        signature,
        description,
        constructors,
        methods,
        entries,
        classlikes,
        properties,
        brief,
        sinceTagContent,
        authorTagContent,
        documentables,
        children,
        embeddedResources,
        extra
    )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        JavadocClasslikePageNode(
            name,
            content as JavadocContentNode,
            dri,
            signature,
            description,
            constructors,
            methods,
            entries,
            classlikes,
            properties,
            brief,
            sinceTagContent,
            authorTagContent,
            documentables,
            children,
            embeddedResources,
            extra
        )
}

public class AllClassesPage(
    public val classes: List<JavadocClasslikePageNode>
) : JavadocPageNode {
    public val classEntries: List<LinkJavadocListEntry> =
        classes.map { LinkJavadocListEntry(it.name, it.dri, ContentKind.Classlikes, it.sourceSets().toSet()) }

    override val name: String = "All Classes"
    override val dri: Set<DRI> = setOf(DRI.topLevel)

    override val documentables: List<Documentable> = emptyList()
    override val embeddedResources: List<String> = emptyList()

    override val content: ContentNode =
        EmptyNode(
            DRI.topLevel,
            ContentKind.Classlikes,
            classes.flatMap { it.sourceSets() }.toSet()
        )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = this

    override fun modified(name: String, children: List<PageNode>): PageNode =
        this

    override val children: List<PageNode> = emptyList()

}

public class DeprecatedPage(
    public val elements: Map<DeprecatedPageSection, Set<DeprecatedNode>>,
    sourceSet: Set<DisplaySourceSet>
) : JavadocPageNode {
    override val name: String = "deprecated"
    override val dri: Set<DRI> = setOf(DRI.topLevel)
    override val documentables: List<Documentable> = emptyList()
    override val children: List<PageNode> = emptyList()
    override val embeddedResources: List<String> = listOf()

    override val content: ContentNode = EmptyNode(
        DRI.topLevel,
        ContentKind.Main,
        sourceSet
    )

    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = this

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = this

}

public class DeprecatedNode(
    public val name: String,
    public val address: DRI,
    public val description: List<ContentNode>
) {
    override fun equals(other: Any?): Boolean =
        (other as? DeprecatedNode)?.address == address

    override fun hashCode(): Int = address.hashCode()
}

public enum class DeprecatedPageSection(
    public val id: String,
    public val caption: String,
    public val header: String,
) {
    DeprecatedModules("module", "Modules", "Module"),
    DeprecatedInterfaces("interface", "Interfaces", "Interface"),
    DeprecatedClasses("class", "Classes", "Class"),
    DeprecatedExceptions("exception", "Exceptions", "Exceptions"),
    DeprecatedFields("field", "Fields", "Field"),
    DeprecatedMethods("method", "Methods", "Method"),
    DeprecatedConstructors("constructor", "Constructors", "Constructor"),
    DeprecatedEnums("enum", "Enums", "Enum"),
    DeprecatedEnumConstants("enum.constant", "Enum Constants", "Enum Constant"),
    DeprecatedForRemoval("forRemoval", "For Removal", "Element");

    internal fun getPosition() = ordinal
}

public class IndexPage(
    public val id: Int,
    public val elements: List<NavigableJavadocNode>,
    public val keys: List<Char>,

    sourceSet: Set<DisplaySourceSet>

) : JavadocPageNode {
    override val name: String = "index-$id"
    override val dri: Set<DRI> = setOf(DRI.topLevel)
    override val documentables: List<Documentable> = emptyList()
    override val children: List<PageNode> = emptyList()
    override val embeddedResources: List<String> = listOf()
    public val title: String = "${keys[id - 1]}-index"

    override val content: ContentNode = EmptyNode(
        DRI.topLevel,
        ContentKind.Classlikes,
        sourceSet
    )

    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = this

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = this

}

public class TreeViewPage(
    override val name: String,
    public val packages: List<JavadocPackagePageNode>?,
    public val classes: List<JavadocClasslikePageNode>?,
    override val dri: Set<DRI>,
    override val documentables: List<Documentable> = emptyList(),
    public val root: PageNode,
    public val inheritanceBuilder: InheritanceBuilder
) : JavadocPageNode {
    init {
        assert(packages == null || classes == null)
        assert(packages != null || classes != null)
    }

    private val childrenDocumentables = root.children.filterIsInstance<WithDocumentables>().flatMap { node ->
        getDocumentableEntries(node)
    }.groupBy({ it.first }) { it.second }.map { (l, r) -> l to r.first() }.toMap()

    private val inheritanceTuple = generateInheritanceTree()
    internal val classGraph = inheritanceTuple.first
    internal val interfaceGraph = inheritanceTuple.second

    override val children: List<PageNode> = emptyList()

    public val title: String = when (documentables.firstOrNull()) {
        is DPackage -> "$name Class Hierarchy"
        else -> "All packages"
    }

    public val kind: String = when (documentables.firstOrNull()) {
        is DPackage -> "package"
        else -> "main"
    }

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        TreeViewPage(
            name,
            packages = children.filterIsInstance<JavadocPackagePageNode>().takeIf { it.isNotEmpty() },
            classes = children.filterIsInstance<JavadocClasslikePageNode>().takeIf { it.isNotEmpty() },
            dri = dri,
            documentables,
            root = root,
            inheritanceBuilder
        )

    override fun modified(name: String, children: List<PageNode>): PageNode =
        TreeViewPage(
            name,
            packages = children.filterIsInstance<JavadocPackagePageNode>().takeIf { it.isNotEmpty() },
            classes = children.filterIsInstance<JavadocClasslikePageNode>().takeIf { it.isNotEmpty() },
            dri = dri,
            documentables,
            root = root,
            inheritanceBuilder
        )

    override val embeddedResources: List<String> = emptyList()

    override val content: ContentNode = EmptyNode(
        DRI.topLevel,
        ContentKind.Classlikes,
        emptySet()
    )

    private fun generateInheritanceTree(): Pair<List<InheritanceNode>, List<InheritanceNode>> {
        val mergeMap = mutableMapOf<DRI, InheritanceNode>()

        fun addToMap(info: InheritanceNode, map: MutableMap<DRI, InheritanceNode>) {
            if (map.containsKey(info.dri))
                map.computeIfPresent(info.dri) { _, info2 ->
                    info.copy(children = (info.children + info2.children).distinct())
                }!!.children.forEach { addToMap(it, map) }
            else
                map[info.dri] = info
        }

        fun collect(dri: DRI): InheritanceNode =
            InheritanceNode(
                dri,
                mergeMap[dri]?.children.orEmpty().map { collect(it.dri) },
                mergeMap[dri]?.interfaces.orEmpty(),
                mergeMap[dri]?.isInterface ?: false
            )

        fun classTreeRec(node: InheritanceNode): List<InheritanceNode> = if (node.isInterface) {
            node.children.flatMap(::classTreeRec)
        } else {
            listOf(node.copy(children = node.children.flatMap(::classTreeRec)))
        }

        fun classTree(node: InheritanceNode) = classTreeRec(node).singleOrNull()

        fun interfaceTreeRec(node: InheritanceNode): List<InheritanceNode> = if (node.isInterface) {
            listOf(node.copy(children = node.children.filter { it.isInterface }))
        } else {
            node.children.flatMap(::interfaceTreeRec)
        }

        fun interfaceTree(node: InheritanceNode) = interfaceTreeRec(node).firstOrNull() // TODO.single()

        val inheritanceNodes = inheritanceBuilder.build(childrenDocumentables)
        inheritanceNodes.forEach { addToMap(it, mergeMap) }

        val rootNodes = mergeMap.entries.filter {
            it.key.classNames in setOf("Any", "Object") //TODO: Probably should be matched by DRI, not just className
        }.map {
            collect(it.value.dri)
        }

        return rootNodes.let { Pair(it.mapNotNull(::classTree), it.mapNotNull(::interfaceTree)) }
    }

    private fun getDocumentableEntries(node: WithDocumentables): List<Pair<DRI, Documentable>> =
        node.documentables.map { it.dri to it } +
                (node as? ContentPage)?.children?.filterIsInstance<WithDocumentables>()
                    ?.flatMap(::getDocumentableEntries).orEmpty()

}

private fun Documentable.kind(): String? =
    when (this) {
        is DClass -> "class"
        is DEnum -> "enum"
        is DAnnotation -> "annotation"
        is DObject -> "object"
        is DInterface -> "interface"
        else -> null
    }
