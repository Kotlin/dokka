package org.jetbrains.dokka

public interface Generator {
    fun buildPages(nodes: Iterable<DocumentationNode>)
    fun buildOutlines(nodes: Iterable<DocumentationNode>)

    final fun buildAll(nodes: Iterable<DocumentationNode>) {
        buildPages(nodes)
        buildOutlines(nodes)
    }

    final fun buildPage(node: DocumentationNode): Unit = buildPages(listOf(node))
    final fun buildOutline(node: DocumentationNode): Unit = buildOutlines(listOf(node))
    final fun buildAll(node: DocumentationNode): Unit = buildAll(listOf(node))
}
