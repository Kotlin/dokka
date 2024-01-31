/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithChildren
import java.util.*

public interface PageNode : WithChildren<PageNode> {
    public val name: String
    override val children: List<PageNode>

    public fun modified(
        name: String = this.name,
        children: List<PageNode> = this.children
    ): PageNode
}

public interface ContentPage : PageNode {
    public val content: ContentNode
    public val dri: Set<DRI>
    public val embeddedResources: List<String>

    @Deprecated("Deprecated. Remove its usages from your code.",
        ReplaceWith("this.documentables.firstOrNull()")
    )
    public val documentable: Documentable?
        get() = if (this is WithDocumentables) this.documentables.firstOrNull() else null

    public fun modified(
        name: String = this.name,
        content: ContentNode = this.content,
        dri: Set<DRI> = this.dri,
        embeddedResources: List<String> = this.embeddedResources,
        children: List<PageNode> = this.children
    ): ContentPage
}

public interface WithDocumentables {
    public val documentables: List<Documentable>
}

public abstract class RootPageNode(
    public val forceTopLevelName: Boolean = false
) : PageNode {
    public val parentMap: Map<PageNode, PageNode> by lazy {
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

    public fun transformPageNodeTree(operation: (PageNode) -> PageNode): RootPageNode =
        this.transformNode(operation) as RootPageNode

    public fun transformContentPagesTree(operation: (ContentPage) -> ContentPage): RootPageNode = transformPageNodeTree {
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

public class ModulePageNode(
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

public class PackagePageNode(
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

public class ClasslikePageNode(
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

public class MemberPageNode(
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


public class MultimoduleRootPageNode(
    override val dri: Set<DRI>,
    override val content: ContentNode,
    override val embeddedResources: List<String> = emptyList()
) : RootPageNode(forceTopLevelName = true), MultimoduleRootPage {
    override val name: String = "All modules"

    override val children: List<PageNode> = emptyList()

    override fun modified(name: String, children: List<PageNode>): RootPageNode =
        MultimoduleRootPageNode(dri, content, embeddedResources)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else MultimoduleRootPageNode(dri, content, embeddedResources)
}

public inline fun <reified T : PageNode> PageNode.children(): List<T> = children.filterIsInstance<T>()

private infix fun <T> List<T>.shallowEq(other: List<T>) =
    this === other || (this.size == other.size && (this zip other).all { (a, b) -> a === b })
