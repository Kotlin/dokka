package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.htmlEscape
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.resolvers.LocationProvider
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

open class HtmlRenderer(
    fileWriter: FileWriter,
    locationProvider: LocationProvider,
    context: DokkaContext
) : DefaultRenderer(fileWriter, locationProvider, context) {

    override fun buildList(node: ContentList, pageContext: PageNode): String = if (node.ordered) {
        "<ol>${buildListItems(node.children, pageContext)}</ol>"
    } else {
        "<ul>${buildListItems(node.children, pageContext)}</ul>"
    }

    protected open fun buildListItems(items: List<ContentNode>, pageContext: PageNode) =
        "<li>\n${items.joinToString("\n</li>\n<li>\n") { it.build(pageContext) }}\n</li>"

    override fun buildResource(node: ContentEmbeddedResource, pageContext: PageNode): String { // TODO: extension point there
        val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "tif", "webp", "svg")
        return if (File(node.address).extension.toLowerCase() in imageExtensions) {
            val imgAttrs = node.extras.filterIsInstance<HTMLSimpleAttr>().joinAttr()
            """<img src="${node.address}" alt="${node.altText}" $imgAttrs>"""
        } else {
            println("Unrecognized resource type: $node")
            ""
        }
    }

    override fun buildTable(node: ContentTable, pageContext: PageNode): String {
        val tableHeader =
            """<thead>
                |<tr>
                |<th> ${node.header.joinToString("<th>\n</tr>\n<tr>\n<th>") { it.build(pageContext) }} </th>
                |</tr>
                |</thead>""".trimMargin()

        return """<table>
            |$tableHeader
            |<tbody>
            |<tr>
            |<td>${node.children.joinToString("</td>\n</tr>\n<tr>\n<td>\n") { it.buildTableRow(pageContext) }}</td>
            |</tr>
            |</tbody>
            |</table>""".trimMargin()
    }

    protected open fun ContentGroup.buildTableRow(pageContext: PageNode) = children.joinToString("</td>\n<td>\n") { it.build(pageContext) }

    override fun buildHeader(level: Int, text: String): String = "<h$level>$text</h$level>\n"

    override fun buildNewLine(): String = "<br/>"

    override fun buildLink(text: String, address: String): String = "<a href=\"$address\">$text</a>"

    override fun buildCode(code: List<ContentNode>, language: String, pageContext: PageNode): String = "<code>$code</code>"

    override fun buildText(textNode: ContentText): String = super.buildText(textNode).htmlEscape()

    override fun renderPage(page: PageNode) {
        val pageText = buildStartHtml(page) + buildPageContent(page) + buildEndHtml()
        fileWriter.write(locationProvider.resolve(page), pageText, "")
    }

    override fun buildSupportFiles() { // TODO copy file instead of reading
        fileWriter.write(
            "style.css",
            javaClass.getResourceAsStream("/dokka/styles/style.css").reader().readText()
        )
    }

    protected open fun buildScripts(page: PageNode) =
        page.embeddedResources.filter { URL(it).path.substringAfterLast('.') == "js" }
            .joinToString(separator = "") { """<script type = "text/javascript" async src = "$it"></script>""" + "\n" }

    protected open fun buildStartHtml(page: PageNode) = """<!DOCTYPE html>
        |<html>
        |<head>
        |<title>${page.name}</title>
        |<link rel="stylesheet" href="${locationProvider.resolveRoot(page)}style.css" />
        |${buildScripts(page)}
        |</head>
        |<body>
        |""".trimMargin()

    protected open fun buildEndHtml() =
        """
        |
        |</body>
        |</html>
    """.trimMargin()

    protected open fun List<HTMLMetadata>.joinAttr() = this.joinToString(" ") { it.key + "=" + it.value }
}