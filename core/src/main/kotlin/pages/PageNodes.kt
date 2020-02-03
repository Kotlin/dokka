package org.jetbrains.dokka.pages

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
    val dri: DRI
    val documentable: Documentable?
    val embeddedResources: List<String>

    fun modified(
        name: String = this.name,
        content: ContentNode = this.content,
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
    } as RootPageNode

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
    override val dri: DRI = DRI.topLevel

    override fun modified(name: String, children: List<PageNode>): ModulePageNode =
        modified(name = name, content = this.content, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ModulePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ModulePageNode(name, content, documentable, children, embeddedResources)
}

class PackagePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,

    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : ContentPage {
    override fun modified(name: String, children: List<PageNode>): PackagePageNode =
        modified(name = name, content = this.content, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): PackagePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else PackagePageNode(name, content, dri, documentable, children, embeddedResources)
}

class ClassPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : ContentPage {
    override fun modified(name: String, children: List<PageNode>): ClassPageNode =
        modified(name = name, content = this.content, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ClassPageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ClassPageNode(name, content, dri, documentable, children, embeddedResources)
}

class MemberPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentable: Documentable?,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : ContentPage {
    override fun modified(name: String, children: List<PageNode>): MemberPageNode =
        modified(name = name, content = this.content, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): MemberPageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else MemberPageNode(name, content, dri, documentable, children, embeddedResources)
}

data class PlatformData(val name: String, val platformType: Platform, val targets: List<String>) {
    override fun toString() = targets.toString()
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

inline fun <reified T: PageNode> PageNode.children() = children.filterIsInstance<T>()

private infix fun <T> List<T>.shallowEq(other: List<T>) =
    this === other || (this.size == other.size && (this zip other).all { (a, b) -> a === b })
