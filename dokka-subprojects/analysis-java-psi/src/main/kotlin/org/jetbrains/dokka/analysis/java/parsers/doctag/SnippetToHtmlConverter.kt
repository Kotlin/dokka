/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import com.intellij.codeInsight.javadoc.JavaDocUtil
import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiSnippetAttribute
import com.intellij.psi.javadoc.PsiSnippetDocTag
import com.intellij.psi.javadoc.PsiSnippetDocTagBody
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.java.util.from
import org.jetbrains.dokka.analysis.java.util.lowercase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.htmlEscape
import kotlin.collections.fold

private typealias MarkupOperation = (String) -> String

public interface SnippetToHtmlConverter {
    public fun convertSnippet(
        snippet: PsiSnippetDocTag
    ): String
}

internal class DefaultSnippetToHtmlConverter(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val context: DokkaContext,
    private val docTagParserContext: DocTagParserContext
) : SnippetToHtmlConverter {

    private val logger = context.logger

    private val sampleFiles by lazy {
        context.plugin<JavaAnalysisPlugin>().querySingle { samplePsiFilesProvider }
            .getSamplePsiFiles(sourceSet, context)
    }

    private companion object {
        private val ALLOWED_ATTRS = mapOf(
            "start" to setOf("region"),
            "end" to setOf("region"),
            "highlight" to setOf("substring", "regex", "region", "type"),
            "replace" to setOf("substring", "regex", "region", "replacement"),
            "link" to setOf("substring", "regex", "region", "target", "type")
        )

        private const val ALLOWED_TAGS = "start|end|highlight|replace|link"

        // group1: comment syntax
        // group2: entire markup comment, @tag + attributes
        private val MARKUP_SPEC = Regex("(//|#|rem|REM|')\\s*(@(?:$ALLOWED_TAGS)(?:\\s.+)?)$")

        // group1: tag name only
        private val MARKUP_TAG = Regex("@($ALLOWED_TAGS)\\s*")

        // name=value
        // group 1: `name`
        // group 2: `=value` part
        // group 3: `value` part
        // only one of 4-6 groups will be not null
        // group 4: value inside single quotes
        // group 5: value inside double quotes
        // group 6: unquoted value
        private val ATTRIBUTE = Regex("(\\w+)\\s*(=\\s*('([^']*)'|\"([^\"]*)\"|(\\S*)))?\\s*")

        private const val SNIPPET_NOT_RESOLVED = "// snippet not resolved"
    }

    override fun convertSnippet(
        snippet: PsiSnippetDocTag
    ): String {
        val value = snippet.valueElement ?: run {
            logger.error("@snippet: unable to resolve snippet")
            return "<pre>$SNIPPET_NOT_RESOLVED</pre>"
        }

        val attributeList = value.attributeList

        val lang = attributeList.getAttribute(PsiSnippetAttribute.LANG_ATTRIBUTE)?.value?.value

        val inlineSnippetLines = value.body?.getSnippetBodyLines()?.takeIf { it.isNotEmpty() }

        val externalSnippet = run {
            val fileAttr = attributeList.getAttribute(PsiSnippetAttribute.FILE_ATTRIBUTE)?.value
            val classAttr = attributeList.getAttribute(PsiSnippetAttribute.CLASS_ATTRIBUTE)?.value

            // Try to resolve the snippet file through PSI reference resolution (for files in `snippet-files`)
            // If that fails, search within sampleFiles (for snippets specified via Javadoc's --snippet-path, in Dokka's case in `sample` configuration option)
            when {
                fileAttr != null -> fileAttr.reference?.resolve() ?: sampleFiles.singleOrNull {
                    it.name == fileAttr.value
                }

                classAttr != null -> classAttr.reference?.resolve() ?: sampleFiles.singleOrNull {
                    it.name == "${classAttr.value}.java"
                }

                else -> null
            }
        }

        val region = attributeList.getAttribute(PsiSnippetAttribute.REGION_ATTRIBUTE)?.value?.value

        var parsedSnippet = when {
            inlineSnippetLines != null && externalSnippet != null -> {
                val parsedInlineSnippet = parseSnippet(inlineSnippetLines, snippet)
                val parsedExternalSnippet = parseSnippet(externalSnippet.text.split("\n"), externalSnippet, region)

                if (parsedInlineSnippet != parsedExternalSnippet) {
                    logger.warn("@snippet: inline and external snippets are not the same in the hybrid snippet\ndiff:")
                    val parsedInlineSnippetLines = parsedInlineSnippet.split("\n")
                    val parsedExternalSnippetLines = parsedExternalSnippet.split("\n")

                    val maxLines = maxOf(parsedInlineSnippetLines.size, parsedExternalSnippetLines.size)

                    for (i in 0 until maxLines) {
                        val parsedInlineSnippetLine = parsedInlineSnippetLines.getOrNull(i)
                        val parsedExternalSnippetLine = parsedExternalSnippetLines.getOrNull(i)

                        if (parsedInlineSnippetLine != parsedExternalSnippetLine) {
                            logger.warn("line ${i + 1}:\ninline: '$parsedInlineSnippetLine'\nexternal: '$parsedExternalSnippetLine'")
                        }
                    }
                }

                parsedInlineSnippet
            }

            inlineSnippetLines != null -> {
                parseSnippet(inlineSnippetLines, snippet)
            }

            externalSnippet != null -> {
                parseSnippet(externalSnippet.text.split("\n"), externalSnippet, region)
            }

            else -> {
                logger.error("@snippet: unable to resolve snippet")
                SNIPPET_NOT_RESOLVED
            }
        }

        if (parsedSnippet.isBlank()) {
            logger.error("@snippet: unable to resolve snippet")
            parsedSnippet = SNIPPET_NOT_RESOLVED
        }

        return "<pre${if (lang != null) " lang=\"$lang\"" else ""}>$parsedSnippet</pre>"
    }

    /**
     * Parses markup for inline and external snippets. For external snippets, the snippet body is first extracted from the snippet file.
     *
     * @param lines lines of the snippet body for inline snippets, or of the snippet file for external snippets
     * @param externalSnippetRegionName name of the region to extract external snippet body from; null for inline snippets
     *
     * @return parsed snippet with applied markup
     */
    private fun parseSnippet(
        lines: List<String>,
        context: PsiElement,
        externalSnippetRegionName: String? = null
    ): String {
        // externalSnippetRegionName is null in 2 cases:
        // case 1: inline snippet, then we already snippetBody
        // case 2: external snippet where the region is not specified, then we take a whole file
        // externalSnippetRegionName is not null in external snippets with a specified region, then we need firstly to find start of the snippet body (`@start` with the appropriate region name)
        var snippetBodyStarted = externalSnippetRegionName == null

        val result = mutableListOf<String>()

        // Ordered list of snippet regions
        data class Region(
            val regionName: String?, // can be null for anonymous regions
            val operation: MarkupOperation?, // can be null for non-markup tags - `@start` (regionName in this case is specified)
            val isSnippetBodyRegion: Boolean = false // indicates whether this region represents the actual snippet body that is processed
        )

        val regions = mutableListOf<Region>()

        var nextLineMarkupTags = mutableListOf<String>()

        main@ for (line in lines) {
            val currentLineOperations = regions.mapNotNull { it.operation }.toMutableList()

            val markupSpecMatch = MARKUP_SPEC.find(line)
            val markupSpec = markupSpecMatch?.groupValues?.get(2)

            if (markupSpec != null || nextLineMarkupTags.isNotEmpty()) {
                val markupTags =
                    (markupSpec?.split(Regex("(?=@(?:$ALLOWED_TAGS)\\s)")) ?: emptyList()) + nextLineMarkupTags

                nextLineMarkupTags.clear()

                // If the markup comment ends with `:`, it is treated as though it were an end-of-line comment on the following line
                if (markupSpec?.endsWith(":") == true) {
                    nextLineMarkupTags = markupTags.toMutableList()
                    continue
                }

                for (markupTag in markupTags) {
                    val (tagName, attributes) = markupTag.parseMarkupTag() ?: continue

                    when (tagName) {
                        "start" -> {
                            val regionName = attributes["region"]
                            if (regionName == null) {
                                logger.warn("@snippet: tag @start without specified region attribute")
                                continue
                            }

                            if (regionName == externalSnippetRegionName && !snippetBodyStarted) {
                                snippetBodyStarted = true
                                regions.add(Region(regionName, null, true))
                            } else {
                                if (regionName == externalSnippetRegionName) logger.warn("@snippet: use unique region names")

                                regions.add(Region(regionName, null))
                            }
                        }

                        "end" -> {
                            val regionName = attributes["region"]
                            if (regionName != null) {
                                val toRemove = regions.lastOrNull { it.regionName == regionName }

                                if (toRemove == null) {
                                    logger.warn("@snippet: invalid region \"$regionName\" in @end")
                                    continue
                                }

                                if (toRemove.isSnippetBodyRegion) {
                                    result.addIfNotBlank(line.clearMarkupSpecAndApplyMarkup(currentLineOperations))
                                    snippetBodyStarted = false
                                    break@main
                                }

                                regions.remove(toRemove)
                            } else {
                                val lastRegion = regions.lastOrNull()

                                if (lastRegion == null) {
                                    logger.warn("@snippet: `@end` tag without a matching start of the region")
                                    continue
                                }

                                if (lastRegion.isSnippetBodyRegion) {
                                    result.addIfNotBlank(line.clearMarkupSpecAndApplyMarkup(currentLineOperations))
                                    snippetBodyStarted = false
                                    break@main
                                } else {
                                    regions.remove(lastRegion)
                                }
                            }
                        }

                        "highlight", "replace", "link" -> {
                            val operation = when (tagName) {
                                "highlight" -> createHighlightOperation(attributes)
                                "replace" -> createReplaceOperation(attributes)
                                "link" -> createLinkOperation(attributes, context)
                                else -> null
                            } ?: continue

                            if (attributes.contains("region")) {
                                regions.add(Region(attributes["region"], operation))
                            }

                            currentLineOperations.add(operation)
                        }

                        else -> {
                            logger.warn("@snippet: unrecognized tag @$tagName in markup comment")
                        }
                    }
                }
            }

            if (snippetBodyStarted) {
                if (markupSpec != null) {
                    result.addIfNotBlank(line.clearMarkupSpecAndApplyMarkup(currentLineOperations))
                } else {
                    result.add(line.applyMarkup(currentLineOperations))
                }
            }
        }

        if (snippetBodyStarted && externalSnippetRegionName != null) {
            logger.warn("@snippet: external snippet doesn't contain closing @end tag")
        }

        return result.joinToString("").trimIndent()
    }

    private fun createHighlightOperation(attributes: Map<String, String?>): MarkupOperation? {
        val type = attributes["type"]?.lowercase()
        val (startTag, endTag) = when (type) {
            "bold", null -> "<b>" to "</b>"
            "italic" -> "<i>" to "</i>"
            "highlighted" -> "<mark>" to "</mark>"
            else -> {
                logger.warn("@snippet: invalid argument for `@highlight` type $type")
                return null
            }
        }

        fun String.wrapInTag() = "$startTag$this$endTag"

        val substring = attributes["substring"]
        val regex = attributes["regex"]?.toRegex()

        return { line ->
            var result = line

            if (substring != null) result = result.replace(substring, substring.wrapInTag())
            if (regex != null) result = result.replace(regex) { match -> match.value.wrapInTag() }

            if (substring == null && regex == null) result = result.wrapInTag()

            result
        }
    }

    private fun createReplaceOperation(attributes: Map<String, String?>): MarkupOperation? {
        val substring = attributes["substring"]
        val regex = attributes["regex"]?.toRegex()
        val replacement = attributes["replacement"]?.htmlEscape() ?: run {
            logger.warn("@snippet: specify `replacement` attribute for @replace markup tag")
            return null
        }

        return { line ->
            var result = line

            if (substring != null) result = result.replace(substring, replacement)
            if (regex != null) result = result.replace(regex, replacement)

            if (substring == null && regex == null) result = replacement

            result
        }
    }

    private fun createLinkOperation(attributes: Map<String, String?>, context: PsiElement): MarkupOperation? {
        val substring = attributes["substring"]
        val regex = attributes["regex"]?.toRegex()
        val target = attributes["target"] ?: run {
            logger.warn("@snippet: specify `target` attribute for @link markup tag")
            return null
        }

        val resolvedTarget = JavaDocUtil.findReferenceTarget(context.manager, target, context) ?: run {
            logger.warn("@snippet: unresolved target for @link tag")
            return null
        }

        val dri = DRI.from(resolvedTarget)
        val driId = docTagParserContext.store(dri)

        fun String.wrapInLink(): String = """<a data-dri="${driId.htmlEscape()}">$this</a>"""

        return { line ->
            var result = line

            if (substring != null) result = result.replace(substring, substring.wrapInLink())
            if (regex != null) result = result.replace(regex) { match -> match.value.wrapInLink() }

            if (substring == null && regex == null) result = result.wrapInLink()

            result
        }
    }

    private fun MutableList<String>.addIfNotBlank(element: String) {
        if (element.isNotBlank()) this.add(element)
    }

    private fun String.clearMarkupSpec() = this.replace(MARKUP_SPEC, "").trimEnd()

    private fun String.applyMarkup(markupOperations: List<MarkupOperation>): String =
        markupOperations.fold(this.htmlEscape()) { acc, op -> op(acc) }

    private fun String.clearMarkupSpecAndApplyMarkup(markupOperations: List<MarkupOperation>) =
        this.clearMarkupSpec().applyMarkup(markupOperations)

    private fun String.parseMarkupTag(): Pair<String, Map<String, String?>>? {
        val markupTagName = MARKUP_TAG.find(this)?.groupValues?.get(1) ?: return null

        val attributes = ATTRIBUTE.findAll(
            this.removePrefix(
                "@$markupTagName"
            ).trimStart()
        ).mapNotNull { match ->
            val attributeName = match.groupValues[1]
            if (ALLOWED_ATTRS[markupTagName]?.contains(attributeName) != true) {
                logger.warn("@snippet: invalid attribute $attributeName used in @$markupTagName tag")
                null
            } else {
                attributeName to (match.groupValues[4].takeIf { it.isNotBlank() }
                    ?: match.groupValues[5].takeIf { it.isNotBlank() }
                    ?: match.groupValues[6].takeIf { it.isNotBlank() })
            }
        }.toMap()

        return markupTagName to attributes
    }

    // Copied from https://github.com/JetBrains/intellij-community/blob/4a0ea4a70a7d2c1a14318c3d88ca632bcbe27e2f/java/java-impl/src/com/intellij/codeInsight/javadoc/SnippetMarkup.java#L402
    private fun PsiSnippetDocTagBody.getSnippetBodyLines(): List<String> {
        val output = mutableListOf<String>()
        var first = true

        for (element in children) {
            when (element) {
                is PsiDocToken -> {
                    if (element.tokenType == JavaDocTokenType.DOC_COMMENT_DATA) {
                        output.add(element.text)
                    }
                }

                is PsiWhiteSpace -> {
                    val text = element.text

                    if (text.contains("\n")) {
                        val idx = output.lastIndex

                        if (idx >= 0 && !output[idx].endsWith("\n")) {
                            output[idx] = output[idx] + "\n" // append newline to last line
                        } else if (first) {
                            first = false
                        } else {
                            output.add("\n") // add empty line
                        }
                    }
                }
            }
        }

        return output
    }
}
