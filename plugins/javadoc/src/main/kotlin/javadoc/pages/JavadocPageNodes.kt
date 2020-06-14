package javadoc.pages

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.renderers.platforms
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

interface JavadocPageNode : ContentPage {
    val contentMap: Map<String, Any?>
}

class JavadocModulePageNode(
    override val name: String, override val content: JavadocContentNode, override val children: List<PageNode>,
    override val dri: Set<DRI>
) :
    RootPageNode(),
    JavadocPageNode {
    override val contentMap: Map<String, Any?> by lazy { mapOf("kind" to "main") + content.contentMap }

    val version: String = "0.0.1"
    val pathToRoot: String = ""

    override val documentable: Documentable? = null
    override val embeddedResources: List<String> = emptyList()
    override fun modified(name: String, ch: List<PageNode>): RootPageNode =
        JavadocModulePageNode(name, content, ch, dri)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        ch: List<PageNode>
    ): ContentPage = JavadocModulePageNode(name, content as JavadocContentNode, ch, dri)
}

class JavadocPackagePageNode(
    override val name: String,
    override val content: JavadocContentNode,
    override val dri: Set<DRI>,

    override val documentable: Documentable? = null,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : JavadocPageNode {
    override val contentMap: Map<String, Any?> by lazy { mapOf("kind" to "package") + content.contentMap }
    override fun modified(
        name: String,
        ch: List<PageNode>
    ): PageNode = JavadocPackagePageNode(
        name,
        content,
        dri,
        documentable,
        ch,
        embeddedResources
    )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        ch: List<PageNode>
    ): ContentPage =
        JavadocPackagePageNode(
            name,
            content as JavadocContentNode,
            dri,
            documentable,
            ch,
            embeddedResources
        )
}

data class JavadocEntryNode(
    val signature: ContentNode,
    val brief: String
) {
    val contentMap: Map<String, Any?> = mapOf(
        "brief" to brief
    )
}

data class JavadocParameterNode(
    val name: String,
    val type: String,
    val description: ContentNode
) {
    val contentMap: Map<String, Any?> = mapOf(
        "name" to name,
        "type" to type
    )
}

data class JavadocPropertyNode(
    val signature: ContentNode,
    val brief: ContentNode
){
    val modifiersAndSignature: Pair<ContentNode, ContentNode>
        get() = (signature as ContentGroup).splitSignatureIntoModifiersAndName()
}

data class JavadocFunctionNode(
    val signature: ContentNode,
    val brief: ContentNode,
    val parameters: List<JavadocParameterNode>,
    override val name: String,
    override val dri: Set<DRI> = emptySet(),
    override val content: ContentNode = EmptyNode(DRI.topLevel, ContentKind.Classlikes, emptySet()),
    override val children: List<PageNode> = emptyList(),
    override val documentable: Documentable? = null,
    override val embeddedResources: List<String> = emptyList(),
): JavadocPageNode {
    override val contentMap: Map<String, Any?> = mapOf(
        "name" to name
    )

    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = TODO()

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = TODO()

    val modifiersAndSignature: Pair<ContentNode, ContentNode>
        get() = (signature as ContentGroup).splitSignatureIntoModifiersAndName()

}

class JavadocClasslikePageNode(
    override val name: String,
    override val content: JavadocContentNode,
    override val dri: Set<DRI>,
    val modifiers: List<String>,
    val signature: ContentNode,
    val description: String,
    val constructors: List<JavadocFunctionNode>,
    val methods: List<JavadocFunctionNode>,
    val entries: List<JavadocEntryNode>,
    val classlikes: List<JavadocClasslikePageNode>,
    val properties: List<JavadocPropertyNode>,
    override val documentable: Documentable? = null,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : JavadocPageNode {
    override val contentMap: Map<String, Any?> by lazy {
        mapOf(
            "kind" to documentable?.kind(),
            "name" to name,
            "package" to dri.first().packageName,
            "classlikeDocumentation" to description
        ) + content.contentMap
    }

    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = JavadocClasslikePageNode(name, content, dri, modifiers, signature, description, constructors, methods, entries, classlikes, properties, documentable, children, embeddedResources)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        JavadocClasslikePageNode(name, content as JavadocContentNode, dri, modifiers, signature, description, constructors, methods, entries, classlikes, properties, documentable, children, embeddedResources)
}

class AllClassesPage(val classes: List<JavadocClasslikePageNode>) : JavadocPageNode {
    val classEntries = classes.map { LinkJavadocListEntry(it.name, it.dri, ContentKind.Classlikes, it.platforms().toSet()) }

    override val contentMap: Map<String, Any?> = mapOf(
        "title" to "All Classes",
        "list" to classEntries
    )

    override val name: String = "All Classes"
    override val dri: Set<DRI> = setOf(DRI.topLevel)

    override val documentable: Documentable? = null
    override val embeddedResources: List<String> = emptyList()

    override val content: ContentNode =
        EmptyNode(
            DRI.topLevel,
            ContentKind.Classlikes,
            classes.flatMap { it.platforms() }.toSet()
        )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = TODO()

    override fun modified(name: String, children: List<PageNode>): PageNode =
        TODO()

    override val children: List<PageNode> = emptyList()

}

class TreeViewPage(
    override val name: String,
    val packages: List<JavadocPackagePageNode>?,
    val classes: List<JavadocClasslikePageNode>?,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    val root: PageNode
) : JavadocPageNode {
    init {
        assert(packages == null || classes == null)
        assert(packages != null || classes != null)
    }

    private val documentables = root.children.filterIsInstance<ContentPage>().flatMap { node ->
        getDocumentableEntries(node)
    }.groupBy({ it.first }) { it.second }.map { (l, r) -> l to r.first() }.toMap()

    private val descriptorMap = getDescriptorMap()
    private val inheritanceTuple = generateInheritanceTree()
    private val classGraph = inheritanceTuple.first
    private val interfaceGraph = inheritanceTuple.second

    override val children: List<PageNode> = emptyList()

    override val contentMap: Map<String, Any?> = mapOf(
        "title" to (when (documentable) {
            is DPackage -> "$name Class Hierarchy"
            else -> "All packages"
        }),
        "name" to name,
        "kind" to (when (documentable) {
            is DPackage -> "package"
            else -> "main"
        }),
        "list" to packages.orEmpty() + classes.orEmpty(),
        "classGraph" to classGraph,
        "interfaceGraph" to interfaceGraph
    )

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
            documentable = documentable,
            root = root
        )

    override fun modified(name: String, children: List<PageNode>): PageNode =
        TreeViewPage(
            name,
            packages = children.filterIsInstance<JavadocPackagePageNode>().takeIf { it.isNotEmpty() },
            classes = children.filterIsInstance<JavadocClasslikePageNode>().takeIf { it.isNotEmpty() },
            dri = dri,
            documentable = documentable,
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
            node.children.flatMap (::classTreeRec)
        }
        else {
            listOf(node.copy(children = node.children.flatMap (::classTreeRec)))
        }
        fun classTree(node: InheritanceNode) = classTreeRec(node).singleOrNull()

        fun interfaceTreeRec(node: InheritanceNode): List<InheritanceNode> = if (node.isInterface) {
            listOf(node.copy(children = node.children.filter { it.isInterface }))
        }
        else {
            node.children.flatMap(::interfaceTreeRec)
        }

        fun interfaceTree(node: InheritanceNode) = interfaceTreeRec(node).firstOrNull() // TODO.single()

        fun gatherPsiClasses(psi: PsiClass): List<Pair<PsiClass, List<PsiClass>>> = psi.supers.toList().let { l ->
            listOf(psi to l) + l.flatMap { gatherPsiClasses(it) }
        }


        val psiInheritanceTree = documentables.flatMap { (_, v) -> (v as? WithExpectActual)?.sources?.values.orEmpty() }
            .filterIsInstance<PsiDocumentableSource>().mapNotNull { it.psi as? PsiClass }.flatMap(::gatherPsiClasses)
            .flatMap { entry -> entry.second.map { it to entry.first } }
            .let {
                it + it.map { it.second to null }
            }
            .groupBy({it.first}) {it.second}
            .map { it.key to it.value.filterNotNull().distinct() }
            .map { (k,v) ->
                InheritanceNode(
                    DRI.from(k),
                    v.map { InheritanceNode(DRI.from(it)) },
                    k.supers.filter { it.isInterface }.map { DRI.from(it) },
                    k.isInterface
                )

            }

        val descriptorInheritanceTree = descriptorMap.flatMap {
                (_,v) -> v.typeConstructor.supertypes
            .map { getClassDescriptorForType(it) to v }
        }
            .let {
                it + it.map { it.second to null }
            }
            .groupBy ({ it.first }) { it.second }
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
        documentables.values.filterIsInstance<DInterface>()
    }

    private fun getDocumentableEntries(node: ContentPage): List<Pair<DRI, Documentable>> =
        listOfNotNull(node.documentable?.let { it.dri to it }) +
                node.children.filterIsInstance<ContentPage>().flatMap(::getDocumentableEntries)

    private fun getDescriptorMap(): Map<DRI, ClassDescriptor> {
        val map: MutableMap<DRI, ClassDescriptor> = mutableMapOf()
        documentables
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
        (this as? WithExpectActual).descriptorForPlatform(platform)

    private fun WithExpectActual?.descriptorForPlatform(platform: Platform = Platform.jvm) = this?.let {
        it.sources.entries.find { it.key.platform == platform }?.value?.let { it as? DescriptorDocumentableSource }?.descriptor as? ClassDescriptor
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

interface ContentValue
data class StringValue(val text: String) : ContentValue
data class ListValue(val list: List<String>) : ContentValue

private fun ContentGroup.splitSignatureIntoModifiersAndName(): Pair<ContentNode, ContentNode> {
    val signature = children.firstIsInstance<ContentGroup>()
    val modifiers = signature.children.takeWhile { it !is ContentLink }
    return Pair(signature.copy(children = modifiers), signature.copy(children = signature.children.drop(modifiers.size)))
}

private fun Documentable.kind(): String? =
    when(this){
        is DClass -> "class"
        is DEnum -> "enum"
        is DAnnotation -> "annotation"
        is DObject -> "object"
        is DInterface -> "interface"
        else -> null
    }