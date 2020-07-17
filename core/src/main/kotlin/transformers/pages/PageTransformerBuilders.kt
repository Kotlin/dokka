package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode

fun pageScanner(block: PageNode.() -> Unit) = object : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = input.invokeOnAll(block) as RootPageNode
}

fun pageMapper(block: PageNode.() -> PageNode) = object : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = input.alterChildren(block) as RootPageNode
}

fun pageStructureTransformer(block: RootPageNode.() -> RootPageNode) = object : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = block(input)
}

fun PageNode.invokeOnAll(block: PageNode.() -> Unit): PageNode =
    this.also(block).also { it.children.forEach { it.invokeOnAll(block) } }

fun PageNode.alterChildren(block: PageNode.() -> PageNode): PageNode =
    block(this).modified(children = this.children.map { it.alterChildren(block) })