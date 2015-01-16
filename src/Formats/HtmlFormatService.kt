package org.jetbrains.dokka

import java.io.File

public open class HtmlFormatService(locationService: LocationService,
                                    signatureGenerator: LanguageService,
                                    val templateService: HtmlTemplateService = HtmlTemplateService.default())
: StructuredFormatService(locationService, signatureGenerator), OutlineFormatService {
    override val extension: String = "html"

    override public fun formatText(text: String): String {
        return text.htmlEscape()
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

    override fun appendBlockCode(to: StringBuilder, line: String) {
        to.append("<pre><code>")
        to.append(line)
        to.append("</code></pre>")
    }

    override fun appendBlockCode(to: StringBuilder, lines: Iterable<String>) {
        to.append("<pre><code>")
        to.append(lines.join("\n"))
        to.append("</code></pre>")
    }

    override fun appendHeader(to: StringBuilder, text: String, level: Int) {
        to.appendln("<h$level>${text}</h$level>")
    }

    override fun appendParagraph(to: StringBuilder, text: String) {
        to.appendln("<p>${text}</p>")
    }

    override fun appendLine(to: StringBuilder, text: String) {
        to.appendln("${text}<br/>")
    }

    override fun appendLine(to: StringBuilder) {
        to.appendln("<br/>")
    }

    override fun appendAnchor(to: StringBuilder, anchor: String) {
        to.appendln("<a name=\"${anchor.htmlEscape()}\"></a>")
    }

    override fun appendTable(to: StringBuilder, body: () -> Unit) {
        to.appendln("<table>")
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
        to.appendln("<td>")
        body()
        to.appendln("</td>")
    }

    override fun formatLink(text: String, location: Location): String {
        return "<a href=\"${location.pathWithAnchor}\">${text}</a>"
    }

    override fun formatLink(text: String, href: String): String {
        return "<a href=\"${href}\">${text}</a>"
    }

    override fun formatStrong(text: String): String {
        return "<strong>${text}</strong>"
    }

    override fun formatEmphasis(text: String): String {
        return "<emph>${text}</emph>"
    }

    override fun formatStrikethrough(text: String): String {
        return "<s>${text}</s>"
    }

    override fun formatCode(code: String): String {
        return "<code>${code}</code>"
    }

    override fun formatList(text: String): String {
        return "<ul>${text}</ul>"
    }

    override fun formatListItem(text: String): String {
        return "<li>${text}</li>"
    }

    override fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        return items.map { formatLink(it) }.joinToString("&nbsp;/&nbsp;")
    }


    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to)
        super<StructuredFormatService>.appendNodes(location, to, nodes)
        templateService.appendFooter(to)
    }

    override fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to)
        super<OutlineFormatService>.appendOutline(location, to, nodes)
        templateService.appendFooter(to)
    }

    override fun getOutlineFileName(location: Location): File {
        return File("${location.path}-outline.html")
    }

    override fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder) {
        val link = ContentNodeLink(node)
        link.append(languageService.render(node, LanguageService.RenderMode.FULL))
        val signature = formatText(location, link)
        to.appendln("${formatLink(signature, location)}<br/>")
    }

    override fun appendOutlineLevel(to: StringBuilder, body: () -> Unit) {
        to.appendln("<ul>")
        body()
        to.appendln("</ul>")
    }
}
