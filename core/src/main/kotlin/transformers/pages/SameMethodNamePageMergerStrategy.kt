package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.*

object SameMethodNamePageMergerStrategy : PageMergerStrategy {
    override fun tryMerge(pages: List<PageNode>): List<PageNode> {
        val name = pages.first().name
        val members = pages.filterIsInstance<MemberPageNode>()
        val others = pages.filterNot { it is MemberPageNode }

        val resChildren = members.flatMap { it.children }.distinct()
        val dri = members.flatMap { it.dri }.toSet()
        val dci = DCI(
            dri = dri,
            kind = members.first().content.dci.kind
        )

        val merged = MemberPageNode(
            dri = dri,
            name = name,
            children = resChildren,
            content = asGroup(dci, members.map { it.content }),
            documentable = null
        )

        return others + listOf(merged)
    }

    fun asGroup(dci: DCI, nodes: List<ContentNode>): ContentGroup {
        val n = nodes.first()
        return ContentGroup(nodes, dci, n.platforms, n.style, n.extras)
    }
}