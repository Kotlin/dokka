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



    private fun List<ContentBlock>.mergeContent(): ContentBlock  =
        ContentBlock(this.first().name, this.flatMap { it.children }.groupBy { it.dci.dri }.flatMap { (_, list) -> list.mergeList() }, DCI(this.first().dci.dri, this.flatMap { it.dci.platformDataList }.distinct()), this.flatMap { it.annotations })

    private fun List<ContentNode>.mergeList(): List<ContentNode> =
        this.groupBy { it::class }.flatMap { (_, list) ->
            val thisClass = list.first()
            val newDCI = DCI(thisClass.dci.dri, list.flatMap { it.dci.platformDataList }.distinct())
            when(thisClass) {
                is ContentBlock -> list.groupBy{ (it as ContentBlock).name }.map { (it.value as List<ContentBlock>).mergeContent() } //(ContentBlock(thisClass.name, (list as List<ContentBlock>).mergeContent(), newDCI, list.flatMap { it.annotations }.distinct()))
                is ContentHeader -> list.distinctBy { it.toString().replace("dci=.*,".toRegex(), "") }.map { (it as ContentHeader).copy(dci = newDCI)}
                is ContentStyle -> list.distinctBy { it.toString().replace("dci=.*,".toRegex(), "") }.map { (it as ContentStyle).copy(dci = newDCI)}
                is ContentSymbol -> list.distinctBy { it.toString().replace("dci=.*,".toRegex(), "") }.map { (it as ContentSymbol).copy(dci = newDCI)}
                is ContentComment -> list.distinctBy { it.toString().replace("dci=.*,".toRegex(), "") }.map { (it as ContentComment).copy(dci = newDCI)}
                is ContentGroup -> list.distinctBy { it.toString().replace("dci=.*,".toRegex(), "") }.map { (it as ContentGroup).copy(dci = newDCI)}
                is ContentList -> list.distinctBy { it.toString().replace("dci=.*,".toRegex(), "") }.map { (it as ContentList).copy(dci = newDCI)}
                else -> list.distinctBy { it.toString().replace("dci=.*,".toRegex(), "") }
            }
        }
}