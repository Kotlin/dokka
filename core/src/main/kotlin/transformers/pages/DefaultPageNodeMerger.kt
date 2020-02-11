package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext

class DefaultPageNodeMerger(val context: DokkaContext) : PageNodeTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(children = input.children.map { it.mergeChildren() })

    fun PageNode.mergeChildren(): PageNode = children.groupBy { it.name }
        .map { (_, v) -> v.mergePageNodes() }
        .let { pages -> modified(children = pages.map { it.first().mergeChildren() }) }

    private fun List<PageNode>.mergePageNodes(): List<PageNode> =
        context[CoreExtensions.pageMergerStrategy].fold(this) { pages, strategy -> tryMerge(strategy, pages) }

    private fun tryMerge(strategy: PageMergerStrategy, pages: List<PageNode>) = if (pages.size > 1)
        strategy.tryMerge(pages)
    else
        pages
}