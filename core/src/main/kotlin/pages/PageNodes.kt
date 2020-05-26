package org.jetbrains.dokka.pages

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI
import java.util.*

interface PageNode {
    val name: String
    val children: List<PageNode>

    fun modified(
        name: String = this.name,
        children: List<PageNode> = this.children
    ): PageNode
}

interface ContentPage: PageNode {
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

abstract class RootPageNode: PageNode {
    val parentMap: Map<PageNode, PageNode> by lazy {
        IdentityHashMap<PageNode, PageNode>().apply {
            fun process(parent: PageNode) {
                parent.children.forEach {  child ->
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
    override val embeddedResources: List<String> = listOf()
) : RootPageNode(), ContentPage {
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
        else ModulePageNode(name, content, documentable, children, embeddedResources)
}

class PackagePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,

    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : ContentPage {
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
        else PackagePageNode(name, content, dri, documentable, children, embeddedResources)
}

class ClasslikePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : ContentPage {
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
        else ClasslikePageNode(name, content, dri, documentable, children, embeddedResources)
}

class MemberPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : ContentPage {
    override fun modified(name: String, children: List<PageNode>): MemberPageNode =
        modified(name = name, content = this.content, children = children) as MemberPageNode

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): MemberPageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else MemberPageNode(name, content, dri, documentable, children, embeddedResources)
}

fun PageNode.dfs(predicate: (PageNode) -> Boolean): PageNode? = if (predicate(this)) {
    this
} else {
    this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
}

fun PageNode.asSequence(): Sequence<PageNode> = sequence {
    yield(this@asSequence)
    children.asSequence().flatMap { it.asSequence() }.forEach { yield(it) }
}

class MultimoduleRootPageNode(
    override val name: String,
    override val dri: Set<DRI>,
    override val content: ContentNode,
    override val embeddedResources: List<String> = emptyList()
) : RootPageNode(), ContentPage {

    override val children: List<PageNode> = emptyList()

    override val documentable: Documentable? = null

    override fun modified(name: String, children: List<PageNode>): RootPageNode =
        MultimoduleRootPageNode(name, dri, content, embeddedResources)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ) =
    if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
    else MultimoduleRootPageNode(name, dri, content, embeddedResources)
}

inline fun <reified T: PageNode> PageNode.children() = children.filterIsInstance<T>()

private infix fun <T> List<T>.shallowEq(other: List<T>) =
    this === other || (this.size == other.size && (this zip other).all { (a, b) -> a === b })
