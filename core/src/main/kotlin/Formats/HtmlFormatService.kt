package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

open class HtmlFormatService @Inject constructor(@Named("folders") locationService: LocationService,
                                                 signatureGenerator: LanguageService,
                                                 val templateService: HtmlTemplateService)
: StructuredFormatService(locationService, signatureGenerator, "html"), OutlineFormatService {
    override fun formatText(text: String): String {
        return text.htmlEscape()
    }

    override fun formatSymbol(text: String): String {
        return "<span class=\"symbol\">${formatText(text)}</span>"
    }

    override fun formatKeyword(text: String): String {
        return "<span class=\"keyword\">${formatText(text)}</span>"
    }

    override fun formatIdentifier(text: String, kind: IdentifierKind, signature: String?): String {
        val id = signature?.let { " id=\"$it\"" }.orEmpty()
        return "<span class=\"identifier\"$id>${formatText(text)}</span>"
    }

    override fun appendBlockCode(to: StringBuilder, line: String, language: String) {
        to.append("<pre><code>")
        to.append(line)
        to.append("</code></pre>")
    }

    override fun appendHeader(to: StringBuilder, text: String, level: Int) {
        to.appendln("<h$level>${text}</h$level>")
    }

    override fun appendParagraph(to: StringBuilder, text: String) {
        to.appendln("<p>${text}</p>")
    }

    override fun appendLine(to: StringBuilder, text: String) {
        to.appendln("$text<br/>")
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

    override fun formatUnorderedList(text: String): String = "<ul>${text}</ul>"
    override fun formatOrderedList(text: String): String = "<ol>${text}</ol>"

    override fun formatListItem(text: String, kind: ListKind): String {
        return "<li>${text}</li>"
    }

    override fun formatBreadcrumbs(items: Iterable<FormatLink>): String {
        return items.map { formatLink(it) }.joinToString("&nbsp;/&nbsp;")
    }


    override fun appendNodes(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to, getPageTitle(nodes), calcPathToRoot(location))
        super.appendNodes(location, to, nodes)
        templateService.appendFooter(to)
    }

    override fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to, "Module Contents", calcPathToRoot(location))
        super.appendOutline(location, to, nodes)
        templateService.appendFooter(to)
    }

    private fun calcPathToRoot(location: Location): Path {
        val path = Paths.get(location.path)
        return path.parent?.relativize(Paths.get(locationService.root.path + '/')) ?: path
    }

    override fun getOutlineFileName(location: Location): File {
        return File("${location.path}-outline.html")
    }

    override fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder) {
        val link = ContentNodeDirectLink(node)
        link.append(languageService.render(node, LanguageService.RenderMode.FULL))
        val signature = formatText(location, link)
        to.appendln("<a href=\"${location.path}\">${signature}</a><br/>")
    }

    override fun appendOutlineLevel(to: StringBuilder, body: () -> Unit) {
        to.appendln("<ul>")
        body()
        to.appendln("</ul>")
    }

    override fun formatNonBreakingSpace(): String = "&nbsp;"

    override fun enumerateSupportFiles(callback: (String, String) -> Unit) {
        callback("/dokka/styles/style.css", "style.css")
    }
}

fun getPageTitle(nodes: Iterable<DocumentationNode>): String? {
    val breakdownByLocation = nodes.groupBy { node -> formatPageTitle(node) }
    return breakdownByLocation.keys.singleOrNull()
}

fun formatPageTitle(node: DocumentationNode): String {
    val path = node.path
    if (path.size == 1) {
        return path.first().name
    }
    val qualifiedName = node.qualifiedName()
    if (qualifiedName.length == 0 && path.size == 2) {
        return path.first().name + " / root package"
    }
    return path.first().name + " / " + qualifiedName
}
