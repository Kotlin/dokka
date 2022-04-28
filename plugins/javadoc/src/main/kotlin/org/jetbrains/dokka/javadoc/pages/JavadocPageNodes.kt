package org.jetbrains.dokka.javadoc.pages

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForType

interface JavadocPageNode : ContentPage, WithDocumentables

interface WithJavadocExtra<T : Documentable> : WithExtraProperties<T> {
    override fun withNewExtras(newExtras: PropertyContainer<T>): T =
        throw IllegalStateException("Merging extras is not applicable for javadoc")
}

fun interface WithNavigable {
    fun getAllNavigables(): List<NavigableJavadocNode>
}

interface WithBrief {
    val brief: List<ContentNode>
}

class JavadocModulePageNode(
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

class JavadocPackagePageNode(
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

interface NavigableJavadocNode {
    fun getId(): String
    fun getDRI(): DRI
}

sealed class AnchorableJavadocNode(open val name: String, open val dri: DRI) : NavigableJavadocNode {
    override fun getId(): String = name
    override fun getDRI(): DRI = dri
}

data class JavadocEntryNode(
    override val dri: DRI,
    override val name: String,
    val signature: JavadocSignatureContentNode,
    override val brief: List<ContentNode>,
    override val extra: PropertyContainer<DEnumEntry> = PropertyContainer.empty()
) : AnchorableJavadocNode(name, dri), WithJavadocExtra<DEnumEntry>, WithBrief

data class JavadocParameterNode(
    override val dri: DRI,
    override val name: String,
    val type: ContentNode,
    val description: List<ContentNode>,
    val typeBound: Bound,
    override val extra: PropertyContainer<DParameter> = PropertyContainer.empty()
) : AnchorableJavadocNode(name, dri), WithJavadocExtra<DParameter>

data class JavadocPropertyNode(
    override val dri: DRI,
    override val name: String,
    val signature: JavadocSignatureContentNode,
    override val brief: List<ContentNode>,
    override val extra: PropertyContainer<DProperty> = PropertyContainer.empty()
) : AnchorableJavadocNode(name, dri), WithJavadocExtra<DProperty>, WithBrief

data class JavadocFunctionNode(
    val signature: JavadocSignatureContentNode,
    override val brief: List<ContentNode>,
    val description: List<ContentNode>,
    val parameters: List<JavadocParameterNode>,
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

class JavadocClasslikePageNode(
    override val name: String,
    override val content: JavadocContentNode,
    override val dri: Set<DRI>,
    val signature: JavadocSignatureContentNode,
    val description: List<ContentNode>,
    val constructors: List<JavadocFunctionNode>,
    val methods: List<JavadocFunctionNode>,
    val entries: List<JavadocEntryNode>,
    val classlikes: List<JavadocClasslikePageNode>,
    val properties: List<JavadocPropertyNode>,
    override val brief: List<ContentNode>,
    override val documentables: List<Documentable> = emptyList(),
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf(),
    override val extra: PropertyContainer<DClasslike> = PropertyContainer.empty(),
) : JavadocPageNode, WithJavadocExtra<DClasslike>, NavigableJavadocNode, WithNavigable, WithBrief, ClasslikePage {

    override fun getAllNavigables(): List<NavigableJavadocNode> =
        methods + entries + classlikes.map { it.getAllNavigables() }.flatten() + this

    fun getAnchorables(): List<AnchorableJavadocNode> =
        constructors + methods + entries + properties

    val kind: String? = documentables.firstOrNull()?.kind()
    val packageName = dri.first().packageName

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
            documentables,
            children,
            embeddedResources,
            extra
        )
}

class AllClassesPage(val classes: List<JavadocClasslikePageNode>) : JavadocPageNode {
    val classEntries =
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

class DeprecatedPage(
    val elements: Map<DeprecatedPageSection, Set<DeprecatedNode>>,
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

class DeprecatedNode(val name: String, val address: DRI, val description: List<ContentNode>) {
    override fun equals(other: Any?): Boolean =
        (other as? DeprecatedNode)?.address == address

    override fun hashCode(): Int = address.hashCode()
}

enum class DeprecatedPageSection(val id: String, val caption: String, val header: String) {
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

class IndexPage(
    val id: Int,
    val elements: List<NavigableJavadocNode>,
    val keys: List<Char>,
    sourceSet: Set<DisplaySourceSet>

) : JavadocPageNode {
    override val name: String = "index-$id"
    override val dri: Set<DRI> = setOf(DRI.topLevel)
    override val documentables: List<Documentable> = emptyList()
    override val children: List<PageNode> = emptyList()
    override val embeddedResources: List<String> = listOf()
    val title: String = "${keys[id - 1]}-index"

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

class TreeViewPage(
    override val name: String,
    val packages: List<JavadocPackagePageNode>?,
    val classes: List<JavadocClasslikePageNode>?,
    override val dri: Set<DRI>,
    override val documentables: List<Documentable> = emptyList(),
    val root: PageNode
) : JavadocPageNode {
    init {
        assert(packages == null || classes == null)
        assert(packages != null || classes != null)
    }

    private val childrenDocumentables = root.children.filterIsInstance<WithDocumentables>().flatMap { node ->
        getDocumentableEntries(node)
    }.groupBy({ it.first }) { it.second }.map { (l, r) -> l to r.first() }.toMap()

    private val descriptorMap = getDescriptorMap()
    private val inheritanceTuple = generateInheritanceTree()
    internal val classGraph = inheritanceTuple.first
    internal val interfaceGraph = inheritanceTuple.second

    override val children: List<PageNode> = emptyList()

    val title = when (documentables.firstOrNull()) {
        is DPackage -> "$name Class Hierarchy"
        else -> "All packages"
    }

    val kind = when (documentables.firstOrNull()) {
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
            root = root
        )

    override fun modified(name: String, children: List<PageNode>): PageNode =
        TreeViewPage(
            name,
            packages = children.filterIsInstance<JavadocPackagePageNode>().takeIf { it.isNotEmpty() },
            classes = children.filterIsInstance<JavadocClasslikePageNode>().takeIf { it.isNotEmpty() },
            dri = dri,
            documentables,
            root = root
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

        fun gatherPsiClasses(psi: PsiClass): List<Pair<PsiClass, List<PsiClass>>> = psi.supers.toList().let { l ->
            listOf(psi to l) + l.flatMap { gatherPsiClasses(it) }
        }

        val psiInheritanceTree =
            childrenDocumentables.flatMap { (_, v) -> (v as? WithSources)?.sources?.values.orEmpty() }
                .filterIsInstance<PsiDocumentableSource>().mapNotNull { it.psi as? PsiClass }
                .flatMap(::gatherPsiClasses)
                .flatMap { entry -> entry.second.map { it to entry.first } }
                .let {
                    it + it.map { it.second to null }
                }
                .groupBy({ it.first }) { it.second }
                .map { it.key to it.value.filterNotNull().distinct() }
                .map { (k, v) ->
                    InheritanceNode(
                        DRI.from(k),
                        v.map { InheritanceNode(DRI.from(it)) },
                        k.supers.filter { it.isInterface }.map { DRI.from(it) },
                        k.isInterface
                    )

                }

        val descriptorInheritanceTree = descriptorMap.flatMap { (_, v) ->
            v.typeConstructor.supertypes
                .map { getClassDescriptorForType(it) to v }
        }
            .let {
                it + it.map { it.second to null }
            }
            .groupBy({ it.first }) { it.second }
            .map { it.key to it.value.filterNotNull().distinct() }
            .map { (k, v) ->
                InheritanceNode(
                    DRI.from(k),
                    v.map { InheritanceNode(DRI.from(it)) },
                    k.typeConstructor.supertypes.map { getClassDescriptorForType(it) }
                        .mapNotNull { cd -> cd.takeIf { it.kind == ClassKind.INTERFACE }?.let { DRI.from(it) } },
                    isInterface = k.kind == ClassKind.INTERFACE
                )
            }

        descriptorInheritanceTree.forEach { addToMap(it, mergeMap) }
        psiInheritanceTree.forEach { addToMap(it, mergeMap) }

        val rootNodes = mergeMap.entries.filter {
            it.key.classNames in setOf("Any", "Object") //TODO: Probably should be matched by DRI, not just className
        }.map {
            collect(it.value.dri)
        }

        return rootNodes.let { Pair(it.mapNotNull(::classTree), it.mapNotNull(::interfaceTree)) }
    }

    private fun generateInterfaceGraph() {
        childrenDocumentables.values.filterIsInstance<DInterface>()
    }

    private fun getDocumentableEntries(node: WithDocumentables): List<Pair<DRI, Documentable>> =
        node.documentables.map { it.dri to it } +
                (node as? ContentPage)?.children?.filterIsInstance<WithDocumentables>()
                    ?.flatMap(::getDocumentableEntries).orEmpty()

    private fun getDescriptorMap(): Map<DRI, ClassDescriptor> {
        val map: MutableMap<DRI, ClassDescriptor> = mutableMapOf()
        childrenDocumentables
            .mapNotNull { (k, v) ->
                v.descriptorForPlatform()?.let { k to it }?.also { (k, v) -> map[k] = v }
            }.map { it.second }.forEach { gatherSupertypes(it, map) }

        return map.toMap()
    }

    private fun gatherSupertypes(descriptor: ClassDescriptor, map: MutableMap<DRI, ClassDescriptor>) {
        map.putIfAbsent(DRI.from(descriptor), descriptor)
        descriptor.typeConstructor.supertypes.map { getClassDescriptorForType(it) }
            .forEach { gatherSupertypes(it, map) }
    }

    private fun Documentable?.descriptorForPlatform(platform: Platform = Platform.jvm) =
        (this as? WithSources).descriptorForPlatform(platform)

    private fun WithSources?.descriptorForPlatform(platform: Platform = Platform.jvm) = this?.let {
        it.sources.entries.find { it.key.analysisPlatform == platform }?.value?.let { it as? DescriptorDocumentableSource }?.descriptor as? ClassDescriptor
    }

    data class InheritanceNode(
        val dri: DRI,
        val children: List<InheritanceNode> = emptyList(),
        val interfaces: List<DRI> = emptyList(),
        val isInterface: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean = other is InheritanceNode && other.dri == dri
        override fun hashCode(): Int = dri.hashCode()
    }
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
