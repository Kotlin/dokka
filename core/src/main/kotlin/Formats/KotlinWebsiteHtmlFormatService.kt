package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.Utilities.impliedPlatformsName
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
        val impliedPlatforms: List<String>,
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

    override fun appendSinceKotlin(version: String) {
    }

    override fun appendSinceKotlinWrapped(version: String) {
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

    override fun appendAsOverloadGroup(to: StringBuilder, platforms: PlatformsData, block: () -> Unit) {
        div(to, "overload-group", calculateDataAttributes(platforms)) {
            block()
        }
    }

    override fun appendLink(href: String, body: () -> Unit) = wrap("<a href=\"$href\">", "</a>", body)

    override fun appendTable(vararg columns: String, body: () -> Unit) {
        //to.appendln("<table class=\"api-docs-table\">")
        div(to, "api-declarations-list") {
            body()
        }
        //to.appendln("</table>")
    }

    override fun appendTableBody(body: () -> Unit) {
        //to.appendln("<tbody>")
        body()
        //to.appendln("</tbody>")
    }

    override fun appendTableRow(body: () -> Unit) {
        //to.appendln("<tr>")
        body()
        //to.appendln("</tr>")
    }

    override fun appendTableCell(body: () -> Unit) {
//        to.appendln("<td>")
        body()
//        to.appendln("\n</td>")
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

    private data class PlatformsForElement(
            val platformToVersion: Map<String, String>
    )

    private fun calculatePlatforms(platforms: PlatformsData): PlatformsForElement {
        //val kotlinVersion = platforms.singleOrNull(String::isKotlinVersion)?.removePrefix("Kotlin ")
        val jreVersion = platforms.keys.filter(String::isJREVersion).min()?.takeUnless { it.endsWith("6") }
        val targetPlatforms = platforms.filterNot { it.key.isJREVersion() } +
                listOfNotNull(jreVersion?.let { it to platforms[it]!! })

        return PlatformsForElement(
                targetPlatforms.mapValues { (_, nodes) -> effectiveSinceKotlinForNodes(nodes) }
        )
    }

    private fun calculateDataAttributes(platforms: PlatformsData): String {
        val platformToVersion = calculatePlatforms(platforms).platformToVersion
        val (platformNames, versions) = platformToVersion.toList().unzip()
        return "data-platform=\"${platformNames.joinToString()}\" "+
                "data-kotlin-version=\"${versions.joinToString()}\""
    }

    override fun appendIndexRow(platforms: PlatformsData, block: () -> Unit) {
//        if (platforms.isNotEmpty())
//            wrap("<tr${calculateDataAttributes(platforms)}>", "</tr>", block)
//        else
//            appendTableRow(block)
        div(to, "declarations", otherAttributes = " ${calculateDataAttributes(platforms)}") {
            block()
        }
    }

    override fun appendPlatforms(platforms: PlatformsData) {
        val platformToVersion = calculatePlatforms(platforms).platformToVersion
        div(to, "tags") {
            div(to, "spacer") {}
            platformToVersion.entries.sortedBy {
                platformSortWeight(it.key)
            }.forEach { (platform, version) ->
                div(to, "tags__tag platform tag-value-$platform",
                        otherAttributes = " data-tag-version=\"$version\"") {
                    to.append(platform)
                }
            }
            div(to, "tags__tag kotlin-version") {
                to.append(mergeVersions(platformToVersion.values.toList()))
            }
        }
    }

    override fun appendAsNodeDescription(platforms: PlatformsData, block: () -> Unit) {
        div(to, "node-page-main", otherAttributes = " ${calculateDataAttributes(platforms)}") {
            block()
        }

    }

    override fun appendBreadcrumbSeparator() {
        to.append(" / ")
    }

    override fun appendPlatformsAsText(platforms: PlatformsData) {
        appendHeader(5) {
            val filtered = platforms.keys.filterNot { it.isJREVersion() }.sortedBy { platformSortWeight(it) }
            if (filtered.isNotEmpty()) {
                to.append("For ")
                filtered.joinTo(to)
            }
        }
    }

    override fun appendSampleBlockCode(language: String, imports: () -> Unit, body: () -> Unit) {
        div(to, "sample", otherAttributes = " data-min-compiler-version=\"1.3\"") {
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

    override fun appendAsPlatformDependentBlock(platforms: PlatformsData, block: (PlatformsData) -> Unit) {
            if (platforms.isNotEmpty())
                wrap("<div ${calculateDataAttributes(platforms)}>", "</div>") {
                    block(platforms)
                }
            else
                block(platforms)
    }

    override fun appendAsSummaryGroup(platforms: PlatformsData, block: (PlatformsData) -> Unit) {
        div(to, "summary-group", otherAttributes = " ${calculateDataAttributes(platforms)}") {
            block(platforms)
        }
    }

    fun platformSortWeight(name: String) = when(name.toLowerCase()) {
        "common" -> 0
        "jvm" -> 1
        "js" -> 3
        "native" -> 4
        else -> 2 // This is hack to support JRE/JUnit and so on
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


private fun String.isKotlinVersion() = this.startsWith("Kotlin")
private fun String.isJREVersion() = this.startsWith("JRE", ignoreCase=true)