package org.jetbrains.dokka

public class ConsoleGenerator() {
    val IndentStep = "  |"

    public fun generate(node: DocumentationNode, indent: String = "") {
        generateHeader(node, indent)
        generateDetails(node, indent)
        generateMembers(node, indent)
        generateLinks(node, indent)
    }

    public fun generateHeader(node: DocumentationNode, indent: String = "") {
        println("$indent${node.kind}: ${node.name}")
        println("$indent\"${node.doc.summary.replace("\n", "\n$indent")}\"")
        println()
    }

    public fun generateMembers(node: DocumentationNode, indent: String = "") {
        val items = node.members
        if (items.isEmpty())
            return
        println("$indent Members")
        for (child in items)
            generate(child, indent + IndentStep)
    }

    public fun generateDetails(node: DocumentationNode, indent: String = "") {
        val items = node.details
        if (items.isEmpty())
            return
        for (child in items)
            generate(child, indent + "  ")
    }

    public fun generateLinks(node: DocumentationNode, indent: String = "") {
        val items = node.links
        if (items.isEmpty())
            return
        println("$indent Links")
        for (child in items)
            generate(child, indent + "  ")
    }
}