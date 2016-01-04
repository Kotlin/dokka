package org.jetbrains.dokka

class ConsoleGenerator(val signatureGenerator: LanguageService, val locationService: LocationService) {
    val IndentStep = "  "

    fun generate(node: DocumentationNode, indent: String = "") {
        println("@${locationService.location(node).path}")
        generateHeader(node, indent)
        //generateDetails(node, indent)
        generateMembers(node, indent)
        generateLinks(node, indent)
    }

    fun generateHeader(node: DocumentationNode, indent: String = "") {
        println(indent + signatureGenerator.render(node))
        val docString = node.content.toString()
        if (!docString.isEmpty())
            println("$indent\"${docString.replace("\n", "\n$indent")}\"")
        println()
    }

    fun generateMembers(node: DocumentationNode, indent: String = "") {
        val items = node.members.sortedBy { it.name }
        for (child in items)
            generate(child, indent + IndentStep)
    }

    fun generateDetails(node: DocumentationNode, indent: String = "") {
        val items = node.details
        for (child in items)
            generate(child, indent + "  ")
    }

    fun generateLinks(node: DocumentationNode, indent: String = "") {
        val items = node.links
        if (items.isEmpty())
            return
        println("$indent Links")
        for (child in items)
            generate(child, indent + "  ")
    }
}