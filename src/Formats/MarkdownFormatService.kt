package org.jetbrains.dokka


public open class MarkdownFormatService(locationService: LocationService,
                                        signatureGenerator: LanguageService)
: StructuredFormatService(locationService, signatureGenerator) {

    override val extension: String = "md"

    override public fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        return items.map { formatLink(it) }.joinToString(" / ")
    }

    override public fun formatText(text: String): String {
        return text.htmlEscape()
    }

    override fun formatSymbol(text: String): String {
        return text.htmlEscape()
    }

    override fun formatKeyword(text: String): String {
        return text.htmlEscape()
    }
    override fun formatIdentifier(text: String): String {
        return text.htmlEscape()
    }

    override public fun formatCode(code: String): String {
        return "`$code`"
    }

    override public fun formatBold(text: String): String {
        return "**$text**"
    }

    override public fun formatLink(text: String, location: Location): String {
        return "[${text}](${location.path})"
    }

    override public fun appendLine(to: StringBuilder) {
        to.appendln()
    }

    override public fun appendLine(to: StringBuilder, text: String) {
        to.appendln(text)
    }

    override public fun appendText(to: StringBuilder, text: String) {
        to.append(text)
    }

    override public fun appendHeader(to: StringBuilder, text: String, level: Int) {
        appendLine(to)
        appendLine(to, "${"#".repeat(level)} $text")
        appendLine(to)
    }

    override public fun appendBlockCode(to: StringBuilder, lines: Iterable<String>) {
        appendLine(to)
        appendLine(to, "```")
        for (line in lines)
            to.appendln(line)
        appendLine(to, "```")
        appendLine(to)
    }

    override public fun appendBlockCode(to: StringBuilder, line: String) {
        appendLine(to, "```")
        to.appendln(line)
        appendLine(to, "```")
    }

    override fun appendTable(to: StringBuilder, body: () -> Unit) {
        to.appendln()
        body()
        to.appendln()
    }

    override fun appendTableHeader(to: StringBuilder, body: () -> Unit) {
        body()
    }

    override fun appendTableBody(to: StringBuilder, body: () -> Unit) {
        body()
    }

    override fun appendTableRow(to: StringBuilder, body: () -> Unit) {
        to.append("|")
        body()
        to.appendln()
    }

    override fun appendTableCell(to: StringBuilder, body: () -> Unit) {
        body()
        to.append("|")
    }

    var outlineLevel = 0
    override fun appendOutlineHeader(to: StringBuilder, node: DocumentationNode) {
        val indent = "    ".repeat(outlineLevel)
        appendLine(to, "$indent- title: ${languageService.renderName(node)}")
        appendLine(to, "$indent  url: ${locationService.location(node).path}")
    }

    override fun appendOutlineChildren(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        val indent = "    ".repeat(outlineLevel)
        appendLine(to, "$indent  content:")
        outlineLevel++
        for (node in nodes) {
            appendOutlineHeader(to, node)
            if (node.members.any()) {
                appendOutlineChildren(to, node.members)
            }
            appendLine(to)
        }
        outlineLevel--
    }
}
