package org.jetbrains.dokka

/**
 * Provides facility for rendering [DocumentationNode] as a language-dependent declaration
 */
trait LanguageService {
    enum class RenderMode {
        /** Brief signature (used in a list of all members of the class). */
        SUMMARY
        /** Full signature (used in the page describing the member itself */
        FULL
    }

    /**
     * Renders a [node](DocumentationNode) as a class, function, property or other signature in a target language.
     * $node: A [DocumentationNode] to render
     * $returns: [ContentNode] which is a root for a rich content tree suitable for formatting with [FormatService]
     */
    fun render(node: DocumentationNode, renderMode: RenderMode = RenderMode.FULL): ContentNode

    /**
     * Renders [node] as a named representation in the target language
     *
     * For example:
     * ${code org.jetbrains.dokka.example}
     *
     * $node: A [DocumentationNode] to render
     * $returns: [String] which is a string representation of the node's name
     */
    fun renderName(node: DocumentationNode): String
}

fun example(service: LanguageService, node: DocumentationNode) {
    println("Node name: ${service.renderName(node)}")
}