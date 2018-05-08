package org.jetbrains.dokka

/**
 * Provides facility for rendering [DocumentationNode] as a language-dependent declaration
 */
interface LanguageService {
    enum class RenderMode {
        /** Brief signature (used in a list of all members of the class). */
        SUMMARY,
        /** Full signature (used in the page describing the member itself */
        FULL
    }

    /**
     * Renders a [node] as a class, function, property or other signature in a target language.
     * @param node A [DocumentationNode] to render
     * @return [ContentNode] which is a root for a rich content tree suitable for formatting with [FormatService]
     */
    fun render(node: DocumentationNode, renderMode: RenderMode = RenderMode.FULL): ContentNode

    /**
     * Tries to summarize the signatures of the specified documentation nodes in a compact representation.
     * Returns the representation if successful, or null if the signatures could not be summarized.
     */
    fun summarizeSignatures(nodes: List<DocumentationNode>): ContentNode?

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

    fun renderNameWithOuterClass(node: DocumentationNode): String
}

fun example(service: LanguageService, node: DocumentationNode) {
    println("Node name: ${service.renderName(node)}")
}