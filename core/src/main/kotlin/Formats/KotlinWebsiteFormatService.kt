package org.jetbrains.dokka

import com.google.inject.Inject


class KotlinWebsiteOutputBuilder(to: StringBuilder,
                                 location: Location,
                                 locationService: LocationService,
                                 languageService: LanguageService,
                                 extension: String)
    : JekyllOutputBuilder(to, location, locationService, languageService, extension)
{
    private var needHardLineBreaks = false
    private var insideDiv = 0

    override fun appendFrontMatter(nodes: Iterable<DocumentationNode>, to: StringBuilder) {
        super.appendFrontMatter(nodes, to)
        to.appendln("layout: api")
    }

    override fun appendBreadcrumbs(path: Iterable<FormatLink>) {
        if (path.count() > 1) {
            to.append("<div class='api-docs-breadcrumbs'>")
            super.appendBreadcrumbs(path)
            to.append("</div>")
        }
    }

    override fun appendCode(body: () -> Unit) = wrapIfNotEmpty("<code>", "</code>", body)

    override fun appendStrikethrough(body: () -> Unit) = wrapInTag("s", body)

    private fun div(to: StringBuilder, cssClass: String, block: () -> Unit) {
        to.append("<div class=\"$cssClass\">")
        insideDiv++
        block()
        insideDiv--
        to.append("</div>\n")
    }

    override fun appendAsSignature(node: ContentNode, block: () -> Unit) {
        val contentLength = node.textLength
        if (contentLength == 0) return
        div(to, "signature") {
            needHardLineBreaks = contentLength >= 62
            try {
                block()
            } finally {
                needHardLineBreaks = false
            }
        }
    }

    override fun appendAsOverloadGroup(to: StringBuilder, block: () -> Unit) {
        to.append("<div class=\"overload-group\">")
        ensureParagraph()
        block()
        ensureParagraph()
        to.append("</div>")
    }

    override fun appendLink(href: String, body: () -> Unit) = wrap("<a href=\"$href\">", "</a>", body)

    override fun appendHeader(level: Int, body: () -> Unit) {
        if (insideDiv > 0) {
            wrapInTag("p", body, newlineAfterClose = true)
        }
        else {
            super.appendHeader(level, body)
        }
    }

    override fun appendLine() {
        if (insideDiv > 0) {
            to.appendln("<br/>")
        }
        else {
            super.appendLine()
        }
    }

    override fun appendTable(vararg columns: String, body: () -> Unit) {
        to.appendln("<table class=\"api-docs-table\">")
        body()
        to.appendln("</table>")
    }

    override fun appendTableBody(body: () -> Unit) {
        to.appendln("<tbody>")
        body()
        to.appendln("</tbody>")
    }

    override fun appendTableRow(body: () -> Unit) {
        to.appendln("<tr>")
        body()
        to.appendln("</tr>")
    }

    override fun appendTableCell(body: () -> Unit) {
        to.appendln("<td markdown=\"1\">")
        body()
        to.appendln("\n</td>")
    }

    override fun appendBlockCode(language: String, body: () -> Unit) {
        if (language.isNotEmpty()) {
            super.appendBlockCode(language, body)
        } else {
            wrap("<pre markdown=\"1\">", "</pre>", body)
        }
    }

    override fun appendSymbol(text: String) {
        to.append("<span class=\"symbol\">${text.htmlEscape()}</span>")
    }

    override fun appendKeyword(text: String) {
        to.append("<span class=\"keyword\">${text.htmlEscape()}</span>")
    }

    override fun appendIdentifier(text: String, kind: IdentifierKind, signature: String?) {
        val id = signature?.let { " id=\"$it\"" }.orEmpty()
        to.append("<span class=\"${identifierClassName(kind)}\"$id>${text.htmlEscape()}</span>")
    }

    override fun appendSoftLineBreak() {
        if (needHardLineBreaks)
            to.append("<br/>")

    }
    override fun appendIndentedSoftLineBreak() {
        if (needHardLineBreaks) {
            to.append("<br/>&nbsp;&nbsp;&nbsp;&nbsp;")
        }
    }

    private fun identifierClassName(kind: IdentifierKind) = when(kind) {
        IdentifierKind.ParameterName -> "parameterName"
        IdentifierKind.SummarizedTypeName -> "summarizedTypeName"
        else -> "identifier"
    }
}

class KotlinWebsiteFormatService @Inject constructor(locationService: LocationService,
                                 signatureGenerator: LanguageService)
    : JekyllFormatService(locationService, signatureGenerator, "html")
{
    override fun createOutputBuilder(to: StringBuilder, location: Location) =
        KotlinWebsiteOutputBuilder(to, location, locationService, languageService, extension)
}
