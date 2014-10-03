package org.jetbrains.dokka

/**
 * Provides facility for rendering [DocumentationNode] as a language-dependent declaration
 */
trait LanguageService {
    /**
     * Renders a [node](DocumentationNode) as a class, function, property or other signature in a target language.
     * $node: A [DocumentationNode] to render
     * $returns: [ContentNode] which is a root for a rich content tree suitable for formatting with [FormatService]
     */
    fun render(node: DocumentationNode): ContentNode

    /**
     * Renders [node] as a named representation in the target language
     *
     * See also [google](http://google.com)
     *
     * $node: A [DocumentationNode] to render
     * $returns: [String] which is a string representation of the node
     */
    fun renderName(node: DocumentationNode) : String
}

