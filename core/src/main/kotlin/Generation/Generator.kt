package org.jetbrains.dokka

import java.io.File

interface Generator {
    fun buildPages(nodes: Iterable<DocumentationNode>)
    fun buildOutlines(nodes: Iterable<DocumentationNode>)
    fun buildSupportFiles()
    fun buildPackageList(nodes: Iterable<DocumentationNode>)
}

fun Generator.buildAll(nodes: Iterable<DocumentationNode>) {
    buildPages(nodes)
    buildOutlines(nodes)
    buildSupportFiles()
    buildPackageList(nodes)
}

fun Generator.buildPage(node: DocumentationNode): Unit = buildPages(listOf(node))

fun Generator.buildOutline(node: DocumentationNode): Unit = buildOutlines(listOf(node))

fun Generator.buildAll(node: DocumentationNode): Unit = buildAll(listOf(node))


interface NodeLocationAwareGenerator: Generator {
    fun location(node: DocumentationNode): Location
    val root: File
}