package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.pages.*
import kotlin.reflect.KClass

class TopDownPageNodeMerger {
    fun mergeModules(nodes: Iterable<ModulePageNode>): PageNode  {
        assert(nodes.all { it::class == nodes.first()::class }) // TODO check

        val merged = ModulePageNode(nodes.first().name, mergeContent(nodes), null, nodes.first().documentationNode) // TODO: merge documentationNodes
        merged.appendChildren(mergeChildren(nodes.flatMap { it.children }))

        return merged
    }

    private fun mergeChildren(nodes: Iterable<PageNode>): List<PageNode> =
        nodes.groupBy { it.dri }.map { (_, list) ->
            val merged = when (list.first()) {
                is PackagePageNode -> PackagePageNode(list.first().name, mergeContent(list), list.first().parent!!, list.first().dri!!, list.first().documentationNode) // TODO: merge documentationlist
                is ClassPageNode -> ClassPageNode(list.first().name, mergeContent(list), list.first().parent!!, list.first().dri!!, list.first().documentationNode) // TODO: merge documentationlist
                is MemberPageNode -> MemberPageNode(list.first().name, mergeContent(list), list.first().parent!!, list.first().dri!!, list.first().documentationNode) // TODO: merge documentationNodes
                else -> throw IllegalStateException("${list.first()} should not be present here")
            }
            merged.appendChildren(mergeChildren(list.flatMap { it.children }))
            merged
        }

    private fun mergeContent(nodes: Iterable<PageNode>): List<ContentNode> = nodes.flatMap { it.content }.groupBy { it.dci.dri }.flatMap { (_, list) -> list.mergeList() }



    private fun List<ContentBlock>.mergeContent(): List<ContentNode>  =
        this.flatMap { it.children }.groupBy { it.dci.dri }.flatMap { (_, list) -> list.mergeList() }

    private fun List<ContentNode>.mergeList(): List<ContentNode> =
        this.groupBy { it::class }.flatMap { (_, list) ->
            val thisClass = list.first()
            when(thisClass) {
                is ContentBlock -> listOf(ContentBlock(thisClass.name, (list as List<ContentBlock>).mergeContent(), thisClass.dci, list.flatMap { it.annotations }.distinct()))
                else -> list.distinctBy { it.toString().replace("dci=.*,".toRegex(), "")}
            }
        }

}