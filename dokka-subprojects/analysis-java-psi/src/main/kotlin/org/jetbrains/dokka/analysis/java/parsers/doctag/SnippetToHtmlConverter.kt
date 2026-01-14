/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import com.intellij.codeInsight.javadoc.JavaDocUtil
import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiSnippetAttribute
import com.intellij.psi.javadoc.PsiSnippetDocTag
import com.intellij.psi.javadoc.PsiSnippetDocTagBody
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.util.from
import org.jetbrains.dokka.analysis.java.util.lowercase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.htmlEscape
import java.io.File
import kotlin.collections.fold

private typealias MarkupOperation = (String) -> String

/**
 * Converts inline JavaDoc `{@snippet ...}` tags into HTML output.
 *
 * This converter processes both inline and external snippets, applying markup operations
 * such as `@highlight`, `@replace`, and `@link`.
 */
internal interface SnippetToHtmlConverter {
    /**
     * Converts a `PsiSnippetDocTag` into its HTML string representation.
     *
     * @param snippet the PSI element representing a `@snippet` JavaDoc tag
     * @return HTML string with processed snippet content
     */
    fun convertSnippet(
        snippet: PsiSnippetDocTag
    ): String
}

internal class DefaultSnippetToHtmlConverter(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val docTagParserContext: DocTagParserContext,
    private val dokkaLogger: DokkaLogger
) : SnippetToHtmlConverter {

    private val sampleFiles by lazy {
        sourceSet.samples.flatMap {
            it.walkTopDown().filter { file -> file.isFile }
        }
    }

    private class SnippetLogger(
        private val logger: DokkaLogger,
        fileName: String?
    ) {
        private val prefix = buildString {
            append("@snippet")
            if (!fileName.isNullOrBlank()) {
                append(" (")
                append(fileName)
                append(")")
            }
            append(": ")
        }

        fun warn(message: String) {
            logger.warn(prefix + message)
        }
    }

    override fun convertSnippet(
        snippet: PsiSnippetDocTag
    ): String {
        val logger = SnippetLogger(dokkaLogger, snippet.containingFile?.name)

        val value = snippet.valueElement ?: run {
            return "<pre><code>${logAndReturnUnresolvedSnippet(logger)}</code></pre>"
        }

        val attributeList = value.attributeList

        val lang = attributeList.getAttribute(PsiSnippetAttribute.LANG_ATTRIBUTE)?.value?.value

        val inlineSnippetLines = value.body?.lines()?.takeIf { it.isNotEmpty() }

        val externalSnippetLines = run {
            val fileAttr = attributeList.getAttribute(PsiSnippetAttribute.FILE_ATTRIBUTE)?.value
            val classAttr = attributeList.getAttribute(PsiSnippetAttribute.CLASS_ATTRIBUTE)?.value

            when {
                fileAttr != null -> readExternalSnippetLines(fileAttr, fileAttr.value)
                classAttr != null -> readExternalSnippetLines(classAttr, "${classAttr.value}.java")
                else -> null
            }
        }

        val region = attributeList.getAttribute(PsiSnippetAttribute.REGION_ATTRIBUTE)?.value?.value

        var parsedSnippet = when {
            inlineSnippetLines != null && externalSnippetLines != null -> {
                val parsedInlineSnippet = parseSnippet(inlineSnippetLines, snippet, logger)
                val parsedExternalSnippet = parseSnippet(externalSnippetLines, snippet, logger, region)

                if (parsedInlineSnippet != parsedExternalSnippet) {
                    val parsedInlineSnippetLines = parsedInlineSnippet.lines()
                    val parsedExternalSnippetLines = parsedExternalSnippet.lines()

                    val maxLines = maxOf(parsedInlineSnippetLines.size, parsedExternalSnippetLines.size)

                    val diffMessage = buildString {
                        appendLine("inline and external snippets are not the same in the hybrid snippet")
                        appendLine("diff:")

                        for (i in 0 until maxLines) {
                            val parsedInlineSnippetLine = parsedInlineSnippetLines.getOrNull(i)
                            val parsedExternalSnippetLine = parsedExternalSnippetLines.getOrNull(i)

                            if (parsedInlineSnippetLine != parsedExternalSnippetLine) {
                                appendLine("line ${i + 1}:")
                                appendLine("inline: '$parsedInlineSnippetLine'")
                                appendLine("external: '$parsedExternalSnippetLine'")
                            }
                        }
                    }

                    logger.warn(diffMessage)
                }

                parsedInlineSnippet
            }

            inlineSnippetLines != null -> {
                parseSnippet(inlineSnippetLines, snippet, logger)
            }

            externalSnippetLines != null -> {
                parseSnippet(externalSnippetLines, snippet, logger, region)
            }

            else -> logAndReturnUnresolvedSnippet(logger)
        }

        if (parsedSnippet.isBlank()) {
            parsedSnippet = logAndReturnUnresolvedSnippet(logger)
        }

        return "<pre${if (lang != null) " lang=\"$lang\"" else ""}><code>$parsedSnippet</code></pre>"
    }

    /**
     * Parses markup for inline and external snippets. For external snippets, the snippet body is first extracted from the snippet file.
     *
     * @param lines lines of the snippet body for inline snippets, or of the snippet file for external snippets
     * @param context the PSI element providing the resolution context for link targets
     * @param externalSnippetRegionName name of the region to extract external snippet body from; null for inline snippets
     *
     * @return parsed snippet with applied markup
     */
    private fun parseSnippet(
        lines: List<String>,
        context: PsiElement,
        logger: SnippetLogger,
        externalSnippetRegionName: String? = null
    ): String {
        // externalSnippetRegionName is null in 2 cases:
        // case 1: inline snippet, then we already snippetBody
        // case 2: external snippet where the region is not specified, then we take a whole file
        // externalSnippetRegionName is not null in external snippets with a specified region, then we need firstly to find start of the snippet body (`@start` with the appropriate region name)
        var snippetBodyStarted = externalSnippetRegionName == null

        val result = mutableListOf<String>()

        data class Region(
            val regionName: String?, // can be null for anonymous regions
            val operation: MarkupOperation?, // can be null for non-markup tags - `@start` (regionName in this case is specified)
            val isSnippetBodyRegion: Boolean = false // indicates whether this region represents the actual snippet body that is processed
        )
        // Ordered list of snippet regions
        val regions = mutableListOf<Region>()

        var nextLineMarkupTags = mutableListOf<String>()

        main@ for (line in lines) {
            val currentLineOperations = regions.mapNotNull { it.operation }.toMutableList()

            val markupSpecMatch = MARKUP_SPEC.find(line)
            val markupSpec = markupSpecMatch?.groupValues?.get(2)

            if (markupSpec != null || nextLineMarkupTags.isNotEmpty()) {
                val markupTags = (nextLineMarkupTags + (markupSpec?.split(MARKUP_TAG_SPLIT)
                    ?: emptyList())).filter { it.isNotBlank() }

                nextLineMarkupTags.clear()

                // If the markup comment ends with `:`, it is treated as though it were an end-of-line comment on the following line
                if (markupSpec?.endsWith(":") == true) {
                    nextLineMarkupTags = markupTags.toMutableList()
                    continue
                }

                for (markupTag in markupTags) {
                    val (tagName, attributes) = markupTag.parseMarkupTag(logger) ?: continue

                    when (tagName) {
                        "start" -> {
                            val regionName = attributes["region"]
                            if (regionName == null) {
                                logger.warn("@start tag without specified 'region' attribute")
                                continue
                            }

                            if (regionName == externalSnippetRegionName && !snippetBodyStarted) {
                                snippetBodyStarted = true
                                regions.add(Region(regionName, null, true))
                            } else {
                                if (regionName == externalSnippetRegionName) logger.warn("use unique region names")

                                regions.add(Region(regionName, null))
                            }
                        }

                        "end" -> {
                            val regionName = attributes["region"]
                            if (regionName != null) {
                                val toRemove = regions.lastOrNull { it.regionName == regionName }

                                if (toRemove == null) {
                                    logger.warn("@end tag references non-existent region '$regionName'")
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
                                    logger.warn("@end tag without a matching start of the region")
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
                                "highlight" -> createHighlightOperation(attributes, logger)
                                "replace" -> createReplaceOperation(attributes, logger)
                                "link" -> createLinkOperation(attributes, context, logger)
                                else -> null
                            } ?: continue

                            if (attributes.contains("region")) {
                                regions.add(Region(attributes["region"], operation))
                            }

                            currentLineOperations.add(operation)
                        }

                        else -> {
                            logger.warn("unrecognized tag @$tagName in the markup comment")
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
            logger.warn("external snippet doesn't contain closing @end tag")
        }

        return result.joinToString("").trimIndent()
    }

    private fun createHighlightOperation(attributes: Map<String, String?>, logger: SnippetLogger): MarkupOperation? {
        val type = attributes["type"]?.lowercase()
        val (startTag, endTag) = when (type) {
            "bold", null -> "<b>" to "</b>"
            "italic" -> "<i>" to "</i>"
            "highlighted" -> "<mark>" to "</mark>"
            else -> {
                logger.warn("unsupported type attribute '$type' in @highlight markup tag. Valid types: 'bold', 'italic', 'highlighted'")
                return null
            }
        }

        val substring = attributes["substring"]?.htmlEscape()
        val regex = attributes["regex"]?.htmlEscape()?.toRegex()

        fun String.wrapInTag() = "$startTag$this$endTag"

        return { line ->
            var result = line

            if (substring != null) result = result.replace(substring, substring.wrapInTag())
            if (regex != null) result = result.replace(regex) { match -> match.value.wrapInTag() }

            if (substring == null && regex == null) result = result.wrapInTag()

            result
        }
    }

    private fun createReplaceOperation(attributes: Map<String, String?>, logger: SnippetLogger): MarkupOperation? {
        val substring = attributes["substring"]?.htmlEscape()
        val regex = attributes["regex"]?.htmlEscape()?.toRegex()
        if (!attributes.containsKey("replacement")) {
            logger.warn("specify 'replacement' attribute in @replace markup tag")
            return null
        }
        val replacement = attributes["replacement"]?.htmlEscape() ?: ""

        return { line ->
            var result = line

            if (substring != null) result = result.replace(substring, replacement)
            if (regex != null) result = result.replace(regex, replacement)

            if (substring == null && regex == null) result = replacement

            result
        }
    }

    private fun createLinkOperation(
        attributes: Map<String, String?>,
        context: PsiElement,
        logger: SnippetLogger
    ): MarkupOperation? {
        val substring = attributes["substring"]?.htmlEscape()
        val regex = attributes["regex"]?.htmlEscape()?.toRegex()
        val target = attributes["target"] ?: run {
            logger.warn("specify 'target' attribute in @link markup tag")
            return null
        }

        val resolvedTarget = JavaDocUtil.findReferenceTarget(context.manager, target, context) ?: run {
            logger.warn("unresolved target '$target' in @link markup tag")
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

    private fun String.clearMarkupSpec() = this.replace(MARKUP_SPEC, "").trimEnd() + "\n"

    private fun String.applyMarkup(markupOperations: List<MarkupOperation>): String =
        markupOperations.fold(this.htmlEscape()) { acc, op -> op(acc) }

    private fun String.clearMarkupSpecAndApplyMarkup(markupOperations: List<MarkupOperation>) =
        this.clearMarkupSpec().applyMarkup(markupOperations)

    private fun String.parseMarkupTag(logger: SnippetLogger): Pair<String, Map<String, String?>>? {
        val markupTagName = MARKUP_TAG.find(this)?.groupValues?.get(1) ?: return null

        val attributes = ATTRIBUTE.findAll(
            this.removePrefix(
                "@$markupTagName"
            ).trimStart()
        ).mapNotNull { match ->
            val attributeName = match.groupValues[1]
            if (ALLOWED_ATTRS[markupTagName]?.contains(attributeName) != true) {
                logger.warn("unsupported attribute '$attributeName' in @$markupTagName markup tag")
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
    private fun PsiSnippetDocTagBody.lines(): List<String> {
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

    private fun readExternalSnippetLines(attribute: PsiElement, fileName: String): List<String>? {
        /**
         * Try to resolve the snippet file through PSI reference resolution (for files in `snippet-files`)
         * If that fails, search within sampleFiles (for snippets specified via Javadoc's --snippet-path, in Dokka's case in `sample` configuration option)
         */
        val psiFile = attribute.reference?.resolve() as? PsiFile

        return if (psiFile != null) {
            File(psiFile.virtualFile.path).readLinesWithNewlines()
        } else {
            sampleFiles.singleOrNull { it.name == fileName }?.readLinesWithNewlines()
        }
    }

    private fun File.readLinesWithNewlines() = this.readLines().map { it + "\n" }

    private fun logAndReturnUnresolvedSnippet(logger: SnippetLogger): String {
        logger.warn("unable to resolve snippet")
        return SNIPPET_NOT_RESOLVED
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

        private val MARKUP_TAG_SPLIT = Regex("(?=@(?:$ALLOWED_TAGS)\\s)")

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
}
