package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty


open class KotlinWebsiteOutputBuilder(
        to: StringBuilder,
        location: Location,
        generator: NodeLocationAwareGenerator,
        languageService: LanguageService,
        extension: String,
        impliedPlatforms: List<String>
) : JekyllOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms) {
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

    protected fun div(to: StringBuilder, cssClass: String, otherAttributes: String = "", markdown: Boolean = false, block: () -> Unit) {
        to.append("<div class=\"$cssClass\"$otherAttributes")
        if (markdown) to.append(" markdown=\"1\"")
        to.append(">")
        if (!markdown) insideDiv++
        block()
        if (!markdown) insideDiv--
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
        div(to, "overload-group", calculateDataAttributes(platforms), true) {
            ensureParagraph()
            block()
            ensureParagraph()
        }
    }

    override fun appendLink(href: String, body: () -> Unit) = wrap("<a href=\"$href\">", "</a>", body)

    override fun appendHeader(level: Int, body: () -> Unit) {
        if (insideDiv > 0) {
            wrapInTag("p", body, newlineAfterClose = true)
        } else {
            super.appendHeader(level, body)
        }
    }

    override fun appendLine() {
        if (insideDiv > 0) {
            to.appendln("<br/>")
        } else {
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

    override fun appendPlatforms(platforms: Set<String>) {

    }
}

class KotlinWebsiteFormatService @Inject constructor(
        generator: NodeLocationAwareGenerator,
        signatureGenerator: LanguageService,
        @Named(impliedPlatformsName) impliedPlatforms: List<String>,
        logger: DokkaLogger
) : JekyllFormatService(generator, signatureGenerator, "html", impliedPlatforms) {
    init {
        logger.warn("Format kotlin-website deprecated and will be removed in next release")
    }

    override fun createOutputBuilder(to: StringBuilder, location: Location) =
            KotlinWebsiteOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms)
}


class KotlinWebsiteRunnableSamplesOutputBuilder(
        to: StringBuilder,
        location: Location,
        generator: NodeLocationAwareGenerator,
        languageService: LanguageService,
        extension: String,
        impliedPlatforms: List<String>
) : KotlinWebsiteOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms) {

    override fun appendSampleBlockCode(language: String, imports: () -> Unit, body: () -> Unit) {
        div(to, "sample", markdown = true) {
            appendBlockCode(language) {
                imports()
                wrap("\n\nfun main(args: Array<String>) {", "}") {
                    wrap("\n//sampleStart\n", "\n//sampleEnd\n", body)
                }
            }
        }
    }
}

class KotlinWebsiteRunnableSamplesFormatService @Inject constructor(
        generator: NodeLocationAwareGenerator,
        signatureGenerator: LanguageService,
        @Named(impliedPlatformsName) impliedPlatforms: List<String>,
        logger: DokkaLogger
) : JekyllFormatService(generator, signatureGenerator, "html", impliedPlatforms) {

    init {
        logger.warn("Format kotlin-website-samples deprecated and will be removed in next release")
    }

    override fun createOutputBuilder(to: StringBuilder, location: Location) =
            KotlinWebsiteRunnableSamplesOutputBuilder(to, location, generator, languageService, extension, impliedPlatforms)
}

