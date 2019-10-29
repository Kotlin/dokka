package org.jetbrains.dokka.pages

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI

abstract class PageNode(
    val name: String,
    val content: List<ContentNode>,
    val parent: PageNode?,
    val dri: DRI?
//    val declarationNode: DeclarationNode
) {
    val children: List<PageNode>
        get() = _children

    private val _children: MutableList<PageNode> = mutableListOf()

    fun appendChildren(children: List<PageNode>) = _children.addAll(children)
    fun appendChild(child: PageNode) = _children.add(child)

}

class ModulePageNode(name: String, content: List<ContentNode>, parent: PageNode?): PageNode(name, content, parent, null)
class PackagePageNode(name: String, content: List<ContentNode>, parent: PageNode, dri: DRI): PageNode(name, content, parent, dri)
class ClassPageNode(name: String, content: List<ContentNode>, parent: PageNode, dri: DRI): PageNode(name, content, parent, dri)  // class, companion object
class MemberPageNode(name: String, content: List<ContentNode>, parent: PageNode, dri: DRI): PageNode(name, content, parent, dri) // functions, extension functions, properties


data class PlatformData(val platformName: String, val platformType: Platform)

fun PageNode.platforms(): List<PlatformData> = this.content.flatMap { it.platforms }.distinct() // TODO: Override equals???
fun PageNode.dfs(predicate: (PageNode) -> Boolean): PageNode? = if (predicate(this)) { this } else { this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull() }



// Navigation??

// content modifier?
//data class ContentLink(val link: String): ContentNode