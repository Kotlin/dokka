package org.jetbrains.dokka

import com.google.inject.Inject


open class MarkdownFormatService
    @Inject constructor(locationService: LocationService,
                        signatureGenerator: LanguageService,
                        linkExtension: String = "md")
: StructuredFormatService(locationService, signatureGenerator, "md", linkExtension) {
    override fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        return items.map { formatLink(it) }.joinToString(" / ")
    }

    override fun formatText(text: String): String = text.htmlEscape()
    override fun formatSymbol(text: String): String = text.htmlEscape()
    override fun formatKeyword(text: String): String = text.htmlEscape()
    override fun formatIdentifier(text: String, kind: IdentifierKind, signature: String?): String = text.htmlEscape()

    override fun formatCode(code: String): String {
        return "`$code`"
    }

    override fun formatUnorderedList(text: String): String = text + "\n"
    override fun formatOrderedList(text: String): String = text + "\n"

    override fun formatListItem(text: String, kind: ListKind): String {
        val itemText = if (text.endsWith("\n")) text else text + "\n"
        return if (kind == ListKind.Unordered) "* $itemText" else "1. $itemText"
    }

    override fun formatStrong(text: String): String {
        return "**$text**"
    }

    override fun formatEmphasis(text: String): String {
        return "*$text*"
    }

    override fun formatStrikethrough(text: String): String {
        return "~~$text~~"
    }

    override fun formatLink(text: String, href: String): String {
        var split = href.split(delimiters = '/', ignoreCase = true);
        if(split.last() == href)
            return formatStrong("[$text]($href)")
        return "[$text]($href)"
    }

    override fun appendLine(to: StringBuilder, text: String) {
        to.appendln(text)
    }

    override fun appendAnchor(to: StringBuilder, anchor: String) {
        // no anchors in Markdown
    }

    override fun appendParagraph(to: StringBuilder, text: String) {
        to.appendln()
        to.appendln(text)
        to.appendln()
    }

    override fun appendHeader(to: StringBuilder, text: String, level: Int) {
        appendLine(to)
        appendLine(to, "${"#".repeat(level)} $text")
        appendLine(to)
    }

    override fun appendBlockCode(to: StringBuilder, line: String, language: String) {
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
