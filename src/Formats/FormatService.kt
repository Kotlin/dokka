package org.jetbrains.dokka

/**
 * Abstract representation of a formatting service used to output documentation in desired format
 *
 * Bundled Formatters:
 * * [HtmlFormatService] – outputs documentation to HTML format
 * * [MarkdownFormatService] – outputs documentation in Markdown format
 * * [TextFormatService] – outputs documentation in Text format
 */
public trait FormatService {
    /** Returns extension for output files */
    val extension: String

    /** Appends formatted content to [StringBuilder](to) using specified [location] */
    fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>)

    /** Appends formatted outline to [StringBuilder](to) using specified [location] */
    fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>)
}

/** Format content to [String] using specified [location] */
fun FormatService.format(location: Location, nodes: Iterable<DocumentationNode>): String = StringBuilder { appendNodes(location, this, nodes) }.toString()

/** Format outline to [String] using specified [location] */
fun FormatService.formatOutline(location: Location, nodes: Iterable<DocumentationNode>): String = StringBuilder { appendOutline(location, this, nodes) }.toString()