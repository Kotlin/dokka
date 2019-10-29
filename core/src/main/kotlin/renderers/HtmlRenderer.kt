package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.ContentLink
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.resolvers.LocationProvider

open class HtmlRenderer(outputDir: String, fileWriter: FileWriter, locationProvider: LocationProvider): DefaultRenderer(outputDir, fileWriter, locationProvider) {

    override fun buildComment(parts: List<ContentNode>): String = "<p>${super.buildComment(parts)}</p>"

    override fun buildSymbol(parts: List<ContentNode>): String = "<code>${super.buildSymbol(parts)}</code>"

    override fun buildHeader(level: Int, text: String): String = "<h$level>$text</h$level>\n"

    override fun buildNewLine(): String = "<br/>"

    override fun buildLink(text: String, address: String): String = "<a href=\"$address\">$text</a>"

    override fun buildCode(code: String): String = "<code>$code</code>"

    override fun buildNavigation(): String = "" // TODO implement

    override fun buildGroup(children: List<ContentNode>): String = "<tr>\n" +
            "<td>" + children.find { it is ContentLink }?.build() + "</td>\n" +
            "<td>" + children.filterNot { it is ContentLink }.joinToString("\n") { it.build() } + "</td>\n" +
            "</tr>\n"

    override fun buildBlock(name: String, content: List<ContentNode>): String =
        buildHeader(2, name) + "<table>\n" + content.joinToString("\n") { it.build() } + "</table>"

    override fun renderPage(page: PageNode) {
        val pageText = buildStartHtml(page) + buildPageContent(page) + buildEndHtml()
        fileWriter.write(locationProvider.resolve(page), pageText)
    }

    protected open fun buildStartHtml(page: PageNode) = """<!DOCTYPE html>
        |<html>
        |<head>
        |<title>${page.name}</title>
        |</head>
        |<body>
        |""".trimMargin()

    protected open fun buildEndHtml() =
        """
        |
        |</body>
        |</html>
    """.trimMargin()
}