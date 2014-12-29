package org.jetbrains.dokka

public class KotlinWebsiteFormatService(locationService: LocationService,
                                 signatureGenerator: LanguageService)
: JekyllFormatService(locationService, signatureGenerator) {
    override val extension: String = "md"

    override fun appendFrontMatter(nodes: Iterable<DocumentationNode>, to: StringBuilder) {
        super.appendFrontMatter(nodes, to)
        to.appendln("layout: api")
    }

    override public fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        items.drop(1)

        if (items.count() > 1) {
            return "<div class='api-docs-breadcrumbs'>" +
                    items.map { formatLink(it) }.joinToString(" / ") +
                    "</div>"
        }

        return ""
    }

    override fun formatLink(text: String, location: Location): String {
        val href = location.path.replace("\\", "/")
                                .replaceAfterLast(".", "html")
                                .replace("/index.html", "/");

        return "<a href=\"${href}\">${text}</a>"
    }

    override fun formatLink(text: String, href: String): String {
        return "<a href=\"${href}\">${text}</a>"
    }

    override fun appendTable(to: StringBuilder, body: () -> Unit) {
        to.appendln("<table class=\"api-docs-table\">")
        body()
        to.appendln("</table>")
    }

    override fun appendTableHeader(to: StringBuilder, body: () -> Unit) {
        to.appendln("<thead>")
        body()
        to.appendln("</thead>")
    }

    override fun appendTableBody(to: StringBuilder, body: () -> Unit) {
        to.appendln("<tbody>")
        body()
        to.appendln("</tbody>")
    }

    override fun appendTableRow(to: StringBuilder, body: () -> Unit) {
        to.appendln("<tr>")
        body()
        to.appendln("</tr>")
    }

    override fun appendTableCell(to: StringBuilder, body: () -> Unit) {
        to.appendln("<td markdown=\"1\">")
        body()
        to.appendln("\n</td>")
    }

    override public fun appendBlockCode(to: StringBuilder, lines: Iterable<String>) {
        to.append("<pre markdown=\"1\">")
        for (line in lines)
            to.appendln(line)
        to.append(to, "</pre>")
    }

    override public fun appendBlockCode(to: StringBuilder, line: String) {
        to.append("<pre markdown=\"1\">")
        to.append(line.trimLeading())
        to.append("</pre>")
    }

    override fun formatSymbol(text: String): String {
        return "<span class=\"symbol\">${formatText(text)}</span>"
    }

    override fun formatKeyword(text: String): String {
        return "<span class=\"keyword\">${formatText(text)}</span>"
    }

    override fun formatIdentifier(text: String): String {
        return "<span class=\"identifier\">${formatText(text)}</span>"
    }
}