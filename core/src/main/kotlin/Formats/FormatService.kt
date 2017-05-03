package org.jetbrains.dokka

/**
 * Abstract representation of a formatting service used to output documentation in desired format
 *
 * Bundled Formatters:
 * * [HtmlFormatService] – outputs documentation to HTML format
 * * [MarkdownFormatService] – outputs documentation in Markdown format
 */
interface FormatService {
    /** Returns extension for output files */
    val extension: String

    /** extension which will be used for internal and external linking */
    val linkExtension: String
        get() = extension

    fun createOutputBuilder(to: StringBuilder, location: Location): FormattedOutputBuilder

    fun enumerateSupportFiles(callback: (resource: String, targetPath: String) -> Unit) {
    }
}

interface FormattedOutputBuilder {
    /** Appends formatted content to [StringBuilder](to) using specified [location] */
    fun appendNodes(nodes: Iterable<DocumentationNode>)
}

/** Format content to [String] using specified [location] */
fun FormatService.format(location: Location, nodes: Iterable<DocumentationNode>): String = StringBuilder().apply {
    createOutputBuilder(this, location).appendNodes(nodes)
}.toString()
