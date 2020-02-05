package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.*

object DefaultPageNodeMerger : PageNodeTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(children = input.children.map { it.mergeChildren() })

    fun asGroup(dci: DCI, nodes: List<ContentNode>): ContentGroup {
        val n = nodes.first()
        return ContentGroup(nodes, dci, n.platforms, n.style, n.extras)
    }

    fun PageNode.mergeChildren(): PageNode = if (children.isNotEmpty()) {
        children.groupBy { it.name }
            .map { (k, v) ->
                if (v.size > 1) {
                    v.mergePageNodes(k)
                } else v.first()
            }
            .map { it.mergeChildren() }
            .let {
                modified(children = it)
            }
    } else this

    private fun List<PageNode>.mergePageNodes(
        k: String
    ): ContentPage {
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
                name = k,
                children = resChildren,
                content = asGroup(dci, contentChildren.map { it.content })
            )
    }
}