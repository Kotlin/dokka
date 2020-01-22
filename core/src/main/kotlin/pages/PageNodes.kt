package org.jetbrains.dokka.pages

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import java.util.*

interface PageNode {
    val name: String
    val content: ContentNode
    val dri: DRI
    val documentable: Documentable?
    val embeddedResources: List<String>
    val children: List<PageNode>

    fun modified(
        name: String = this.name,
        content: ContentNode = this.content,
        embeddedResources: List<String> = this.embeddedResources,
        children: List<PageNode> = this.children
    ): PageNode
}

class ModulePageNode(
    override val name: String,
    override val content: ContentNode,

    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : PageNode {
    override val dri: DRI = DRI.topLevel

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ModulePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ModulePageNode(name, content, documentable, children, embeddedResources)

    private fun PageNode.transformNode(operation: (PageNode) -> PageNode): PageNode =
        operation(this).let { newNode ->
            newNode.modified(children = newNode.children.map { it.transformNode(operation) })
        }

    fun transformPageNodeTree(operation: (PageNode) -> PageNode) =
        this.transformNode(operation) as ModulePageNode

    val parentMap: IdentityHashMap<PageNode, PageNode> by lazy {
        IdentityHashMap<PageNode, PageNode>().apply {
            fun addParent(parent: PageNode) {
                parent.children.forEach { child ->
                    put(child, parent)
                    addParent(child)
                }
            }
            addParent(this@ModulePageNode)
        }
    }
}

class PackagePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,

    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : PageNode {

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): PackagePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else PackagePageNode(name, content, dri, documentable, children, embeddedResources)
}

class ClasslikePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentable: Documentable?,
    override val children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : PageNode {

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ClasslikePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ClasslikePageNode(name, content, dri, documentable, children, embeddedResources)
}

class MemberPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentable: Documentable?,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : PageNode {

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

private infix fun <T> List<T>.shallowEq(other: List<T>) =
    this === other || (this.size == other.size && (this zip other).all { (a, b) -> a === b })

// Navigation??

// content modifier?
//data class ContentLink(val link: String): ContentNode