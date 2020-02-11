package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.*

object SameNamePageMergerStrategy : PageMergerStrategy {
    override fun tryMerge(pages: List<PageNode>): List<PageNode> = listOf(pages.mergePageNodes())

    private fun List<PageNode>.mergePageNodes(): ContentPage {
        val name = first().name
        val resChildren = this.flatMap { it.children }.distinct()
        val contentChildren = this.filterIsInstance<ContentPage>()
        val dri = contentChildren.flatMap { it.dri }.toSet()
        val dci = DCI(
            dri = dri,
            kind = contentChildren.first().content.dci.kind
        )
        return contentChildren.first()
            .modified(
                dri = dri,
                name = name,
                children = resChildren,
                content = asGroup(dci, contentChildren.map { it.content })
            )
    }

    fun asGroup(dci: DCI, nodes: List<ContentNode>): ContentGroup {
        val n = nodes.first()
        return ContentGroup(nodes, dci, n.platforms, n.style, n.extras)
    }
}