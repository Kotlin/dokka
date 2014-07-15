package org.jetbrains.dokka

public open class HtmlFormatService(locationService: LocationService, signatureGenerator: LanguageService)
: StructuredFormatService(locationService, signatureGenerator) {
    override val extension: String = "html"

    override public fun formatText(text: String): String {
        return text.htmlEscape()
    }

    override fun appendBlockCode(to: StringBuilder, line: String) {
        to.appendln("<code>")
        to.appendln(formatText(line))
        to.appendln("</code>")
    }

    override fun appendBlockCode(to: StringBuilder, lines: Iterable<String>) {
        to.appendln("<code>")
        to.appendln(lines.map { formatText(it) }.join("\n"))
        to.appendln("</code>")
    }

    override fun appendHeader(to: StringBuilder, text: String, level: Int) {
        to.appendln("<h$level>${formatText(text)}</h$level>")
    }

    override fun appendText(to: StringBuilder, text: String) {
        to.appendln("<p>${formatText(text)}</p>")
    }

    override fun appendLine(to: StringBuilder, text: String) {
        to.appendln("${formatText(text)}<br/>")
    }

    override fun appendLine(to: StringBuilder) {
        to.appendln("<br/>")
    }

    override fun formatLink(text: String, location: Location): String {
        return "<a href=\"${location.path}\">${formatText(text)}</a>"
    }

    override fun formatBold(text: String): String {
        return "<b>${formatText(text)}</b>"
    }

    override fun formatCode(code: String): String {
        return "<code>${formatText(code)}</code>"
    }

    override fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        return items.map { formatLink(it) }.joinToString("&nbsp;/&nbsp;")
    }

    override fun appendOutlineChildren(to: StringBuilder, nodes: Iterable<DocumentationNode>) {
    }
    override fun appendOutlineHeader(to: StringBuilder, node: DocumentationNode) {
    }
}