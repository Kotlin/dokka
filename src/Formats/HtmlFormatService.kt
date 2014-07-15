package org.jetbrains.dokka

public open class HtmlFormatService(locationService: LocationService, signatureGenerator: SignatureGenerator)
: StructuredFormatService(locationService, signatureGenerator) {
    override val extension: String = "html"

    override fun appendBlockCode(to: StringBuilder, line: String) {
        to.appendln("<code>")
        to.appendln(line)
        to.appendln("</code>")
    }

    override fun appendBlockCode(to: StringBuilder, lines: Iterable<String>) {
        to.appendln("<code>")
        to.appendln(lines.join("\n"))
        to.appendln("</code>")
    }

    override fun appendHeader(to: StringBuilder, text: String, level: Int) {
        to.appendln("<h$level>$text</h$level>")
    }

    override fun appendText(to: StringBuilder, text: String) {
        to.appendln("<p>$text</p>")
    }

    override fun appendLine(to: StringBuilder, text: String) {
        to.appendln("$text<br/>")
    }

    override fun appendLine(to: StringBuilder) {
        to.appendln("<br/>")
    }

    override fun formatLink(link: FormatLink): String {
        return "<a href=\"${link.location.path}\">${link.text}</a>"
    }

    override fun formatBold(text: String): String {
        return "<b>$text</b>"
    }

    override fun formatCode(code: String): String {
        return "<code>$code</code>"
    }

    override fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        return items.map { formatLink(it) }.joinToString("&nbsp;/&nbsp;")
    }
}