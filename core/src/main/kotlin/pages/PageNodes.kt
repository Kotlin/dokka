package org.jetbrains.dokka.pages

import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI

interface PageNode {
    val name: String
    val content: ContentNode
    val parent: PageNode?
    val dri: DRI
    val documentationNode: DocumentationNode?
    val embeddedResources: List<String>
    val children: List<PageNode>

    fun modified(
        name: String = this.name,
        content: ContentNode = this.content,
        embeddedResources: List<String> = this.embeddedResources,
        children: List<PageNode> = this.children
    ): PageNode
}

abstract class BasicPageNode(children: List<PageNode>) : PageNode {

    private lateinit var _parent: PageNode
    override val parent: PageNode? by lazy { _parent }
    override val children = children

    override fun equals(other: Any?): Boolean =
        if (other is PageNode) {
            dri == other.dri && name == other.name
        } else false

    override fun hashCode(): Int =
        (name + dri).hashCode()

    init {
        children.forEach { if (it is BasicPageNode) it._parent = this }
    }
}

class ModulePageNode(
    override val name: String,
    override val content: ContentNode,
    override val documentationNode: DocumentationNode?,
    children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : BasicPageNode(children) {
    override val parent: Nothing? = null
    override val dri: DRI = DRI.topLevel

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ModulePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ModulePageNode(name, content, documentationNode, children, embeddedResources)

    private fun PageNode.transformNode(operation: (PageNode) -> PageNode): PageNode =
        operation(this).let { newNode ->
            newNode.modified(children = newNode.children.map { it.transformNode(operation) })
        }

    fun transformPageNodeTree(operation: (PageNode) -> PageNode) =
        this.transformNode(operation) as ModulePageNode
}

class PackagePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentationNode: DocumentationNode?,
    children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : BasicPageNode(children) {

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): PackagePageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else PackagePageNode(name, content, dri, documentationNode, children, embeddedResources)
}

class ClassPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentationNode: DocumentationNode?,
    children: List<PageNode>,
    override val embeddedResources: List<String> = listOf()
) : BasicPageNode(children) {

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ClassPageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else ClassPageNode(name, content, dri, documentationNode, children, embeddedResources)
}

class MemberPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentationNode: DocumentationNode?,
    children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : BasicPageNode(children) {

    override fun modified(
        name: String,
        content: ContentNode,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): MemberPageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else MemberPageNode(name, content, dri, documentationNode, children, embeddedResources)
}

data class PlatformData(val platformType: Platform, val targets: List<String>) {
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