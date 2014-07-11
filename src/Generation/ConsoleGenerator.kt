package org.jetbrains.dokka

public class ConsoleGenerator() {
    val IndentStep = "  "

    public fun generate(node: DocumentationNode, indent: String = "") {
        generateHeader(node, indent)
        generateDetails(node, indent)
        generateMembers(node, indent)
    }

    public fun generateHeader(node: DocumentationNode, indent: String = "") {
        println("$indent${node.kind}: ${node.name}")
        println("$indent\"${node.doc.replace("\n", "\n$indent")}\"")
        println()
    }

    public fun generateDetails(node: DocumentationNode, indent: String = "") {
        for (child in node.details)
            generate(child, indent + IndentStep)
    }

    public fun generateMembers(node: DocumentationNode, indent: String = "") {
        for (child in node.members)
            generate(child, indent + IndentStep)
    }
}