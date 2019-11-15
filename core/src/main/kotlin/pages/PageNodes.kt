package org.jetbrains.dokka.pages

import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI

interface PageNode {
    val name: String
    val content: ContentNode
    val parent: PageNode?
    val dri: DRI
    val documentationNode: DocumentationNode<*>?
    val children: List<PageNode>
}

abstract class BasicPageNode(children: List<PageNode>): PageNode {

    private lateinit var _parent: PageNode
    override val parent: PageNode? by lazy { _parent }
    override val children = children

    override fun equals(other: Any?): Boolean =
        if (other is PageNode) {
            dri == other.dri && name == other.name
        }
        else false

    override fun hashCode(): Int =
        (name + dri).hashCode()

    init {
        children.forEach { if (it is BasicPageNode) it._parent = this }
    }

}

class ModulePageNode(
    override val name: String,
    override val content: ContentNode,
    override val documentationNode: DocumentationNode<*>?,
    children: List<PageNode>
): BasicPageNode(children) {
    override val parent: Nothing? = null
    override val dri: DRI = DRI.topLevel
}

class PackagePageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentationNode: DocumentationNode<*>?,
    children: List<PageNode>
): BasicPageNode(children)

class ClassPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentationNode: DocumentationNode<*>?,
    children: List<PageNode>
): BasicPageNode(children)

class MemberPageNode(
    override val name: String,
    override val content: ContentNode,
    override val dri: DRI,
    override val documentationNode: DocumentationNode<*>?,
    children: List<PageNode> = emptyList()
): BasicPageNode(children)

data class PlatformData(val platformType: Platform, val targets: List<String>) {
    override fun toString() = targets.toString()
}

fun PageNode.dfs(predicate: (PageNode) -> Boolean): PageNode? = if (predicate(this)) { this } else { this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull() }

// Navigation??

// content modifier?
//data class ContentLink(val link: String): ContentNode