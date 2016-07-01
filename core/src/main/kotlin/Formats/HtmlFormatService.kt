package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

open class HtmlOutputBuilder(to: StringBuilder,
                             location: Location,
                             locationService: LocationService,
                             languageService: LanguageService,
                             extension: String,
                             val templateService: HtmlTemplateService)
    : StructuredOutputBuilder(to, location, locationService, languageService, extension)
{
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

    override fun appendBlockCode(to: StringBuilder, lines: List<String>, language: String) {
        to.append("<pre><code>")
        to.append(lines.joinToString("\n"))
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

    override fun appendTable(to: StringBuilder, vararg columns: String, body: () -> Unit) {
        to.appendln("<table>")
        body()
        to.appendln("</table>")
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


    override fun appendNodes(nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to, getPageTitle(nodes), locationService.calcPathToRoot(location))
        super.appendNodes(nodes)
        templateService.appendFooter(to)
    }

    override fun formatNonBreakingSpace(): String = "&nbsp;"
}

open class HtmlFormatService @Inject constructor(@Named("folders") locationService: LocationService,
                                                 signatureGenerator: LanguageService,
                                                 val templateService: HtmlTemplateService)
: StructuredFormatService(locationService, signatureGenerator, "html"), OutlineFormatService {

    override fun enumerateSupportFiles(callback: (String, String) -> Unit) {
        callback("/dokka/styles/style.css", "style.css")
    }

    override fun createOutputBuilder(to: StringBuilder, location: Location) =
        HtmlOutputBuilder(to, location, locationService, languageService, extension, templateService)

    override fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to, "Module Contents", locationService.calcPathToRoot(location))
        super.appendOutline(location, to, nodes)
        templateService.appendFooter(to)
    }

    override fun getOutlineFileName(location: Location): File {
        return File("${location.path}-outline.html")
    }

    override fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder) {
        val link = ContentNodeDirectLink(node)
        link.append(languageService.render(node, LanguageService.RenderMode.FULL))
        val signature = createOutputBuilder(to, location).formatText(location, link)
        to.appendln("<a href=\"${location.path}\">${signature}</a><br/>")
    }

    override fun appendOutlineLevel(to: StringBuilder, body: () -> Unit) {
        to.appendln("<ul>")
        body()
        to.appendln("</ul>")
    }
}

private fun LocationService.calcPathToRoot(location: Location): Path {
    val path = Paths.get(location.path)
    return path.parent?.relativize(Paths.get(root.path + '/')) ?: path
}

fun getPageTitle(nodes: Iterable<DocumentationNode>): String? {
    val breakdownByLocation = nodes.groupBy { node -> formatPageTitle(node) }
    return breakdownByLocation.keys.singleOrNull()
}

fun formatPageTitle(node: DocumentationNode): String {
    val path = node.path
    val moduleName = path.first().name
    if (path.size == 1) {
        return moduleName
    }

    val qName = qualifiedNameForPageTitle(node)
    return qName + " - " + moduleName
}

private fun qualifiedNameForPageTitle(node: DocumentationNode): String {
    if (node.kind == NodeKind.Package) {
        var packageName = node.qualifiedName()
        if (packageName.isEmpty()) {
            packageName = "root package"
        }
        return packageName
    }

    val path = node.path
    var pathFromToplevelMember = path.dropWhile { it.kind !in NodeKind.classLike }
    if (pathFromToplevelMember.isEmpty()) {
        pathFromToplevelMember = path.dropWhile { it.kind != NodeKind.Property && it.kind != NodeKind.Function }
    }
    if (pathFromToplevelMember.isNotEmpty()) {
        return pathFromToplevelMember.map { it.name }.filter { it.length > 0 }.joinToString(".")
    }
    return node.qualifiedName()
}
