package org.jetbrains.dokka.base.transformers.pages.merger

import org.jetbrains.dokka.pages.*

object SameMethodNamePageMergerStrategy : PageMergerStrategy {
    override fun tryMerge(pages: List<PageNode>, path: List<String>): List<PageNode> {
        val name = pages.first().name
        val members = pages.filterIsInstance<MemberPageNode>()

        if (members.isEmpty()) return pages

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
            content = asGroup(
                dci,
                members.map { it.content }),
            documentable = null
        )

        return others + listOf(merged)
    }

    fun asGroup(dci: DCI, nodes: List<ContentNode>): ContentGroup =
        nodes.first().let { ContentGroup(nodes, dci, it.platforms, it.style, it.extra) }

}