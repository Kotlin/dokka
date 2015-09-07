package org.jetbrains.dokka

/**
 * Abstract representation of a formatting service used to output documentation in desired format
 *
 * Bundled Formatters:
 * * [HtmlFormatService] – outputs documentation to HTML format
 * * [MarkdownFormatService] – outputs documentation in Markdown format
 * * [TextFormatService] – outputs documentation in Text format
 */
public interface FormatService {
    /** Returns extension for output files */
    val extension: String

    /** Appends formatted content to [StringBuilder](to) using specified [location] */
    fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>)
}

/** Format content to [String] using specified [location] */
fun FormatService.format(location: Location, nodes: Iterable<DocumentationNode>): String = kotlin.StringBuilder { appendNodes(location, this, nodes) }.toString()
