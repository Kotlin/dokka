package org.jetbrains.dokka.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import java.util.*

interface PageNode : WithChildren<PageNode> {
    val name: String
    override val children: List<PageNode>

    fun modified(
        name: String = this.name,
        children: List<PageNode> = this.children
    ): PageNode
}

interface ContentPage : PageNode {
    val content: ContentNode
    val dri: Set<DRI>
    val documentable: Documentable?
    val embeddedResources: List<String>

    fun modified(
        name: String = this.name,
        content: ContentNode = this.content,
        dri: Set<DRI> = this.dri,
        embeddedResources: List<String> = this.embeddedResources,
        children: List<PageNode> = this.children
    ): ContentPage
}

abstract class RootPageNode : PageNode {
    val parentMap: Map<PageNode, PageNode> by lazy {
        IdentityHashMap<PageNode, PageNode>().apply {
            fun process(parent: PageNode) {
                parent.children.forEach { child ->
                    put(child, parent)
                    process(child)
                }
            }
            process(this@RootPageNode)
        }
    }

    fun transformPageNodeTree(operation: (PageNode) -> PageNode) =
        this.transformNode(operation) as RootPageNode

    fun transformContentPagesTree(operation: (ContentPage) -> ContentPage) = transformPageNodeTree {
        if (it is ContentPage) operation(it) else it
    }

    private fun PageNode.transformNode(operation: (PageNode) -> PageNode): PageNode =
        operation(this).let { newNode ->
            newNode.modified(children = newNode.children.map { it.transformNode(operation) })
        }

    abstract override fun modified(
        name: String,
        children: List<PageNode>
    ): RootPageNode
}

class ModulePageNode(
    override val name: String,
    override val content: ContentNode,
    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf(),
    override val extra: PropertyContainer<ContentPage> = PropertyContainer.empty()
) : RootPageNode(), ModulePage, WithExtraProperties<ContentPage> {
    override val dri: Set<DRI> = setOf(DRI.topLevel)

    override fun modified(name: String, children: List<PageNode>): ModulePageNode =
        modified(name = name, content = this.content, dri = dri, children = children)

    //TODO same copy optimisation here?
    override fun withNewExtras(newExtras: PropertyContainer<ContentPage>): ContentPage =
        ModulePageNode(name, content, documentable, children, embeddedResources, newExtras)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ModulePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ModulePageNode(name, content, documentable, children, embeddedResources, extra)
}

class PackagePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf(),
    override val extra: PropertyContainer<ContentPage> = PropertyContainer.empty()
) : PackagePage, WithExtraProperties<ContentPage> {

    init {
        require(name.isNotBlank()) { "PackagePageNode.name cannot be blank" }
    }

    override fun modified(name: String, children: List<PageNode>): PackagePageNode =
        modified(name = name, content = this.content, children = children)

    override fun withNewExtras(newExtras: PropertyContainer<ContentPage>): ContentPage =
        PackagePageNode(name, content, dri, documentable, children, embeddedResources, newExtras)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): PackagePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else PackagePageNode(name, content, dri, documentable, children, embeddedResources, extra)
}

class ClasslikePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf(),
    override val extra: PropertyContainer<ContentPage> = PropertyContainer.empty()
) : ClasslikePage, WithExtraProperties<ContentPage> {
    override fun modified(name: String, children: List<PageNode>): ClasslikePageNode =
        modified(name = name, content = this.content, children = children)

    override fun withNewExtras(newExtras: PropertyContainer<ContentPage>): ContentPage =
        ClasslikePageNode(name, content, dri, documentable, children, embeddedResources, newExtras)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ClasslikePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ClasslikePageNode(name, content, dri, documentable, children, embeddedResources, extra)
}

class MemberPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf(),
    override val extra: PropertyContainer<ContentPage> = PropertyContainer.empty()
) : MemberPage, WithExtraProperties<ContentPage> {
    override fun modified(name: String, children: List<PageNode>): MemberPageNode =
        modified(name = name, content = this.content, children = children)

    override fun withNewExtras(newExtras: PropertyContainer<ContentPage>): ContentPage =
        MemberPageNode(name, content, dri, documentable, children, embeddedResources, newExtras)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): MemberPageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else MemberPageNode(name, content, dri, documentable, children, embeddedResources, extra)
}


class MultimoduleRootPageNode(
    override val name: String,
    override val dri: Set<DRI>,
    override val content: ContentNode,
    override val embeddedResources: List<String> = emptyList(),
    override val extra: PropertyContainer<ContentPage> = PropertyContainer.empty()
) : RootPageNode(), MultimoduleRootPage, WithExtraProperties<ContentPage> {

    override val children: List<PageNode> = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<ContentPage>): ContentPage =
        MultimoduleRootPageNode(name, dri, content, embeddedResources, newExtras)

    override val documentable: Documentable? = null

    override fun modified(name: String, children: List<PageNode>): RootPageNode =
        modified(name, content, dri, embeddedResources, children)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ) =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources) this
        else MultimoduleRootPageNode(name, dri, content, embeddedResources, extra)
}

inline fun <reified T : PageNode> PageNode.children() = children.filterIsInstance<T>()

private infix fun <T> List<T>.shallowEq(other: List<T>) =
    this === other || (this.size == other.size && (this zip other).all { (a, b) -> a === b })
