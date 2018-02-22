package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File


object EmptyHtmlTemplateService : HtmlTemplateService {
    override fun appendFooter(to: StringBuilder) {}

    override fun appendHeader(to: StringBuilder, title: String?, basePath: File) {}
}


open class KotlinWebsiteHtmlOutputBuilder(
        to: StringBuilder,
        location: Location,
        generator: NodeLocationAwareGenerator,
        languageService: LanguageService,
        extension: String,
        impliedPlatforms: List<String>,
        templateService: HtmlTemplateService
) : HtmlOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms, templateService) {
    private var needHardLineBreaks = false
    private var insideDiv = 0

    override fun appendLine() {}

    override fun appendBreadcrumbs(path: Iterable<FormatLink>) {
        if (path.count() > 1) {
            to.append("<div class='api-docs-breadcrumbs'>")
            super.appendBreadcrumbs(path)
            to.append("</div>")
        }
    }

    override fun appendCode(body: () -> Unit) = wrapIfNotEmpty("<code>", "</code>", body)

    protected fun div(to: StringBuilder, cssClass: String, otherAttributes: String = "", block: () -> Unit) {
        to.append("<div class=\"$cssClass\"$otherAttributes")
        to.append(">")
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

    override fun appendAsOverloadGroup(to: StringBuilder, platforms: Set<String>, block: () -> Unit) {
        div(to, "overload-group", calculateDataAttributes(platforms)) {
            block()
        }
    }

    override fun appendLink(href: String, body: () -> Unit) = wrap("<a href=\"$href\">", "</a>", body)

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
        to.appendln("<td>")
        body()
        to.appendln("\n</td>")
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

    private fun identifierClassName(kind: IdentifierKind) = when (kind) {
        IdentifierKind.ParameterName -> "parameterName"
        IdentifierKind.SummarizedTypeName -> "summarizedTypeName"
        else -> "identifier"
    }

    fun calculateDataAttributes(platforms: Set<String>): String {
        fun String.isKotlinVersion() = this.startsWith("Kotlin")
        fun String.isJREVersion() = this.startsWith("JRE")
        val kotlinVersion = platforms.singleOrNull(String::isKotlinVersion)
        val jreVersion = platforms.singleOrNull(String::isJREVersion)
        val targetPlatforms = platforms.filterNot { it.isKotlinVersion() || it.isJREVersion() }

        val kotlinVersionAttr = kotlinVersion?.let { " data-kotlin-version=\"$it\"" } ?: ""
        val jreVersionAttr = jreVersion?.let { " data-jre-version=\"$it\"" } ?: ""
        val platformsAttr = targetPlatforms.ifNotEmpty { " data-platform=\"${targetPlatforms.joinToString()}\"" } ?: ""
        return "$platformsAttr$kotlinVersionAttr$jreVersionAttr"
    }

    override fun appendIndexRow(platforms: Set<String>, block: () -> Unit) {
        if (platforms.isNotEmpty())
            wrap("<tr${calculateDataAttributes(platforms)}>", "</tr>", block)
        else
            appendTableRow(block)
    }

    override fun appendPlatforms(platforms: Set<String>) {}

    override fun appendBreadcrumbSeparator() {
        to.append(" / ")
    }

    override fun appendSampleBlockCode(language: String, imports: () -> Unit, body: () -> Unit) {
        div(to, "sample") {
            appendBlockCode(language) {
                imports()
                wrap("\n\nfun main(args: Array<String>) {".htmlEscape(), "}") {
                    wrap("\n//sampleStart\n", "\n//sampleEnd\n", body)
                }
            }
        }
    }

    override fun appendSoftParagraph(body: () -> Unit) = appendParagraph(body)


    override fun appendSectionWithTag(section: ContentSection) {
        appendParagraph {
            appendStrong { appendText(section.tag) }
            appendText(" ")
            appendContent(section)
        }
    }
}

class KotlinWebsiteHtmlFormatService @Inject constructor(
        generator: NodeLocationAwareGenerator,
        signatureGenerator: LanguageService,
        @Named(impliedPlatformsName) impliedPlatforms: List<String>,
        templateService: HtmlTemplateService
) : HtmlFormatService(generator, signatureGenerator, templateService, impliedPlatforms) {

    override fun enumerateSupportFiles(callback: (String, String) -> Unit) {}

    override fun createOutputBuilder(to: StringBuilder, location: Location) =
            KotlinWebsiteHtmlOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms, templateService)
}

