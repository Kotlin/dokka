package org.jetbrains.dokka

public interface Generator {
    fun buildPages(nodes: Iterable<DocumentationNode>)
    fun buildOutlines(nodes: Iterable<DocumentationNode>)
    fun buildSupportFiles()
}

fun Generator.buildAll(nodes: Iterable<DocumentationNode>) {
    buildPages(nodes)
    buildOutlines(nodes)
    buildSupportFiles()
}

fun Generator.buildPage(node: DocumentationNode): Unit = buildPages(listOf(node))

fun Generator.buildOutline(node: DocumentationNode): Unit = buildOutlines(listOf(node))

fun Generator.buildAll(node: DocumentationNode): Unit = buildAll(listOf(node))
