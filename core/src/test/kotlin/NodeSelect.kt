package org.jetbrains.dokka.tests

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.RefKind

class SelectBuilder {
    private val root = ChainFilterNode(SubgraphTraverseFilter(), null)
    private var activeNode = root
    private val chainEnds = mutableListOf<SelectFilter>()

    fun withName(name: String) = matching { it.name == name }

    fun withKind(kind: NodeKind) = matching{ it.kind == kind }

    fun matching(block: (DocumentationNode) -> Boolean) {
        attachFilterAndMakeActive(PredicateFilter(block))
    }

    fun subgraph() {
        attachFilterAndMakeActive(SubgraphTraverseFilter())
    }

    fun subgraphOf(kind: RefKind) {
        attachFilterAndMakeActive(DirectEdgeFilter(kind))
    }

    private fun attachFilterAndMakeActive(next: SelectFilter) {
        activeNode = ChainFilterNode(next, activeNode)
    }

    private fun endChain() {
        chainEnds += activeNode
    }

    fun build(): SelectFilter {
        endChain()
        return CombineFilterNode(chainEnds)
    }
}

private class ChainFilterNode(val filter: SelectFilter, val previous: SelectFilter?): SelectFilter() {
    override fun select(roots: Sequence<DocumentationNode>): Sequence<DocumentationNode> {
        return filter.select(previous?.select(roots) ?: roots)
    }
}

private class CombineFilterNode(val previous: List<SelectFilter>): SelectFilter() {
    override fun select(roots: Sequence<DocumentationNode>): Sequence<DocumentationNode> {
        return previous.asSequence().flatMap { it.select(roots) }
    }
}

abstract class SelectFilter {
    abstract fun select(roots: Sequence<DocumentationNode>): Sequence<DocumentationNode>
}

private class SubgraphTraverseFilter: SelectFilter() {
    override fun select(roots: Sequence<DocumentationNode>): Sequence<DocumentationNode> {
        val visited = mutableSetOf<DocumentationNode>()
        return roots.flatMap {
            generateSequence(listOf(it)) { nodes ->
                nodes.flatMap { it.allReferences() }
                    .map { it.to }
                    .filter { visited.add(it) }
                    .takeUnless { it.isEmpty() }
            }
        }.flatten()
    }

}

private class PredicateFilter(val condition: (DocumentationNode) -> Boolean): SelectFilter() {
    override fun select(roots: Sequence<DocumentationNode>): Sequence<DocumentationNode> {
        return roots.filter(condition)
    }
}

private class DirectEdgeFilter(val kind: RefKind): SelectFilter() {
    override fun select(roots: Sequence<DocumentationNode>): Sequence<DocumentationNode> {
        return roots.flatMap { it.references(kind).asSequence() }.map { it.to }
    }
}


fun selectNodes(root: DocumentationNode, block: SelectBuilder.() -> Unit): List<DocumentationNode> {
    val builder = SelectBuilder()
    builder.apply(block)
    return builder.build().select(sequenceOf(root)).toMutableSet().toList()
}