package org.jetbrains.dokka.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithChildren
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
    val embeddedResources: List<String>

    @Deprecated("Deprecated. Remove its usages from your code.",
        ReplaceWith("this.documentables.firstOrNull()")
    )
    val documentable: Documentable?
        get() = if (this is WithDocumentables) this.documentables.firstOrNull() else null

    fun modified(
        name: String = this.name,
        content: ContentNode = this.content,
        dri: Set<DRI> = this.dri,
        embeddedResources: List<String> = this.embeddedResources,
        children: List<PageNode> = this.children
    ): ContentPage
}

interface WithDocumentables {
    val documentables: List<Documentable>
}

abstract class RootPageNode(val forceTopLevelName: Boolean = false) : PageNode {
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
    override val documentables: List<Documentable> = listOf(),
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : RootPageNode(), ModulePage {
    override val dri: Set<DRI> = setOf(DRI.topLevel)

    override fun modified(name: String, children: List<PageNode>): ModulePageNode =
        modified(name = name, content = this.content, dri = dri, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ModulePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ModulePageNode(name, content, documentables, children, embeddedResources)
}

class PackagePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentables: List<Documentable> = listOf(),
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : PackagePage {

    init {
        require(name.isNotBlank()) { "PackagePageNode.name cannot be blank" }
    }

    override fun modified(name: String, children: List<PageNode>): PackagePageNode =
        modified(name = name, content = this.content, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): PackagePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else PackagePageNode(name, content, dri, documentables, children, embeddedResources)
}

class ClasslikePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentables: List<Documentable> = listOf(),
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : ClasslikePage {
    override fun modified(name: String, children: List<PageNode>): ClasslikePageNode =
        modified(name = name, content = this.content, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ClasslikePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ClasslikePageNode(name, content, dri, documentables, children, embeddedResources)
}

class MemberPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentables: List<Documentable> = listOf(),
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : MemberPage {
    override fun modified(name: String, children: List<PageNode>): MemberPageNode =
        modified(name = name, content = this.content, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): MemberPageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else MemberPageNode(name, content, dri, documentables, children, embeddedResources)
}


class MultimoduleRootPageNode(
    override val dri: Set<DRI>,
    override val content: ContentNode,
    override val embeddedResources: List<String> = emptyList()
) : RootPageNode(forceTopLevelName = true), MultimoduleRootPage {
    override val name = "All modules"

    override val children: List<PageNode> = emptyList()

    override fun modified(name: String, children: List<PageNode>): RootPageNode =
        MultimoduleRootPageNode(dri, content, embeddedResources)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ) =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else MultimoduleRootPageNode(dri, content, embeddedResources)
}

inline fun <reified T : PageNode> PageNode.children() = children.filterIsInstance<T>()

private infix fun <T> List<T>.shallowEq(other: List<T>) =
    this === other || (this.size == other.size && (this zip other).all { (a, b) -> a === b })
