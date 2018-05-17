package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName
import java.io.File

open class HtmlOutputBuilder(to: StringBuilder,
                             location: Location,
                             generator: NodeLocationAwareGenerator,
                             languageService: LanguageService,
                             extension: String,
                             impliedPlatforms: List<String>,
                             val templateService: HtmlTemplateService)
    : StructuredOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms)
{
    override fun appendText(text: String) {
        to.append(text.htmlEscape())
    }

    override fun appendSymbol(text: String) {
        to.append("<span class=\"symbol\">${text.htmlEscape()}</span>")
    }

    override fun appendKeyword(text: String) {
        to.append("<span class=\"keyword\">${text.htmlEscape()}</span>")
    }

    override fun appendIdentifier(text: String, kind: IdentifierKind, signature: String?) {
        val id = signature?.let { " id=\"$it\"" }.orEmpty()
        to.append("<span class=\"identifier\"$id>${text.htmlEscape()}</span>")
    }

    override fun appendBlockCode(language: String, body: () -> Unit) {
        val openTags = if (language.isNotBlank())
            "<pre><code class=\"lang-$language\">"
        else
            "<pre><code>"
        wrap(openTags, "</code></pre>", body)
    }

    override fun appendHeader(level: Int, body: () -> Unit) =
        wrapInTag("h$level", body, newlineBeforeOpen = true, newlineAfterClose = true)
    override fun appendParagraph(body: () -> Unit) =
        wrapInTag("p", body, newlineBeforeOpen = true, newlineAfterClose = true)

    override fun appendSoftParagraph(body: () -> Unit) = appendParagraph(body)

    override fun appendLine() {
        to.appendln("<br/>")
    }

    override fun appendAnchor(anchor: String) {
        to.appendln("<a name=\"${anchor.htmlEscape()}\"></a>")
    }

    override fun appendTable(vararg columns: String, body: () -> Unit) =
            wrapInTag("table", body, newlineAfterOpen = true, newlineAfterClose = true)
    override fun appendTableBody(body: () -> Unit) =
            wrapInTag("tbody", body, newlineAfterOpen = true, newlineAfterClose = true)
    override fun appendTableRow(body: () -> Unit) =
            wrapInTag("tr", body, newlineAfterOpen = true, newlineAfterClose = true)
    override fun appendTableCell(body: () -> Unit) =
            wrapInTag("td", body, newlineAfterOpen = true, newlineAfterClose = true)

    override fun appendLink(href: String, body: () -> Unit) = wrap("<a href=\"$href\">", "</a>", body)

    override fun appendStrong(body: () -> Unit) = wrapInTag("strong", body)
    override fun appendEmphasis(body: () -> Unit) = wrapInTag("em", body)
    override fun appendStrikethrough(body: () -> Unit) = wrapInTag("s", body)
    override fun appendCode(body: () -> Unit) = wrapInTag("code", body)

    override fun appendUnorderedList(body: () -> Unit) = wrapInTag("ul", body, newlineAfterClose = true)
    override fun appendOrderedList(body: () -> Unit) = wrapInTag("ol", body, newlineAfterClose = true)
    override fun appendListItem(body: () -> Unit) = wrapInTag("li", body, newlineAfterClose = true)

    override fun appendBreadcrumbSeparator() {
        to.append("&nbsp;/&nbsp;")
    }

    override fun appendNodes(nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to, getPageTitle(nodes), generator.relativePathToRoot(location))
        super.appendNodes(nodes)
        templateService.appendFooter(to)
    }

    override fun appendNonBreakingSpace() {
        to.append("&nbsp;")
    }

    override fun ensureParagraph() {

    }
}

open class HtmlFormatService @Inject constructor(generator: NodeLocationAwareGenerator,
                                                 signatureGenerator: LanguageService,
                                                 val templateService: HtmlTemplateService,
                                                 @Named(impliedPlatformsName) val impliedPlatforms: List<String>)
: StructuredFormatService(generator, signatureGenerator, "html"), OutlineFormatService {

    override fun enumerateSupportFiles(callback: (String, String) -> Unit) {
        callback("/dokka/styles/style.css", "style.css")
    }

    override fun createOutputBuilder(to: StringBuilder, location: Location) =
        HtmlOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms, templateService)

    override fun appendOutline(location: Location, to: StringBuilder, nodes: Iterable<DocumentationNode>) {
        templateService.appendHeader(to, "Module Contents", generator.relativePathToRoot(location))
        super.appendOutline(location, to, nodes)
        templateService.appendFooter(to)
    }

    override fun getOutlineFileName(location: Location): File {
        return File("${location.path}-outline.html")
    }

    override fun appendOutlineHeader(location: Location, node: DocumentationNode, to: StringBuilder) {
        val link = ContentNodeDirectLink(node)
        link.append(languageService.render(node, LanguageService.RenderMode.FULL))
        val tempBuilder = StringBuilder()
        createOutputBuilder(tempBuilder, location).appendContent(link)
        to.appendln("<a href=\"${location.path}\">$tempBuilder</a><br/>")
    }

    override fun appendOutlineLevel(to: StringBuilder, body: () -> Unit) {
        to.appendln("<ul>")
        body()
        to.appendln("</ul>")
    }
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
