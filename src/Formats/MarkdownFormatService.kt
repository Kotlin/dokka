package org.jetbrains.dokka


public open class MarkdownFormatService(locationService: LocationService,
                                        signatureGenerator: LanguageService)
: StructuredFormatService(locationService, signatureGenerator, "md") {
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
    override fun formatIdentifier(text: String, kind: IdentifierKind): String {
        return text.htmlEscape()
    }

    override public fun formatCode(code: String): String {
        return "`$code`"
    }

    override public fun formatList(text: String): String {
        return text
    }

    override fun formatListItem(text: String): String {
        return "* $text"
    }

    override public fun formatStrong(text: String): String {
        return "**$text**"
    }

    override fun formatEmphasis(text: String): String {
        return "*$text*"
    }

    override fun formatStrikethrough(text: String): String {
        return "~~$text~~"
    }

    override fun formatLink(text: String, href: String): String {
        return "[$text]($href)"
    }

    override public fun appendLine(to: StringBuilder) {
        to.appendln()
    }

    override public fun appendLine(to: StringBuilder, text: String) {
        to.appendln(text)
    }

    override fun appendAnchor(to: StringBuilder, anchor: String) {
        // no anchors in Markdown
    }

    override public fun appendParagraph(to: StringBuilder, text: String) {
        to.appendln()
        to.appendln(text)
        to.appendln()
    }

    override public fun appendHeader(to: StringBuilder, text: String, level: Int) {
        appendLine(to)
        appendLine(to, "${"#".repeat(level)} $text")
        appendLine(to)
    }

    override public fun appendBlockCode(to: StringBuilder, line: String, language: String) {
        appendLine(to)
        to.appendln("``` ${language}")
        to.appendln(line)
        to.appendln("```")
        appendLine(to)
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
        to.append(" ")
        body()
        to.append(" |")
    }

    override fun formatNonBreakingSpace(): String = "&nbsp;"
}
