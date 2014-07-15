package org.jetbrains.dokka


public open class MarkdownFormatService(locationService: LocationService, signatureGenerator: SignatureGenerator)
: StructuredFormatService(locationService, signatureGenerator) {

    override val extension: String = "md"

    override public fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        return items.map { formatLink(it) }.joinToString(" / ")
    }

    override public fun formatCode(code: String): String {
        return "`$code`"
    }

    override public fun formatBold(text: String): String {
        return "**$text**"
    }

    override public fun formatLink(link: FormatLink): String {
        return "[${link.text}](${link.location.path})"
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
        appendLine(to, "```")
        for (line in lines)
            appendLine(to, line)
        appendLine(to, "```")
    }

    override public fun appendBlockCode(to: StringBuilder, line: String) {
        appendLine(to, "```")
        appendLine(to, line)
        appendLine(to, "```")
    }
}
