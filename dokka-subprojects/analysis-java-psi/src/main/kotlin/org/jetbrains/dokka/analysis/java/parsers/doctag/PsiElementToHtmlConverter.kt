/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import com.intellij.lexer.JavaDocTokenTypes
import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.javadoc.PsiDocParamRef
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocTagValue
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import com.intellij.psi.javadoc.PsiMarkdownCodeBlock
import com.intellij.psi.javadoc.PsiMarkdownReferenceLink
import com.intellij.psi.javadoc.PsiSnippetDocTag
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.java.JavadocTag
import org.jetbrains.dokka.analysis.java.doccomment.DocumentationContent
import org.jetbrains.dokka.analysis.java.doccomment.PsiDocumentationContent
import org.jetbrains.dokka.analysis.java.parsers.CommentResolutionContext
import org.jetbrains.dokka.analysis.java.parsers.doctag.markdown.MarkdownToHtmlConverter
import org.jetbrains.dokka.analysis.java.util.defaultLabel
import org.jetbrains.dokka.analysis.java.util.from
import org.jetbrains.dokka.analysis.java.util.getLocation
import org.jetbrains.dokka.analysis.java.util.lowercase
import org.jetbrains.dokka.analysis.java.util.referenceElement
import org.jetbrains.dokka.analysis.java.util.referenceElementOrSelf
import org.jetbrains.dokka.analysis.java.util.resolveToDocumentedElement
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.htmlEscape

private const val UNRESOLVED_PSI_ELEMENT = "UNRESOLVED_PSI_ELEMENT"
private const val PRE_CLOSING_TAG = "</pre>"

internal class PsiElementToHtmlConverter(
    private val inheritDocTagResolver: InheritDocTagResolver,
    private val sourceSet: DokkaSourceSet,
    private val logger: DokkaLogger,
    private val markdownToHtmlConverterProvider: () -> MarkdownToHtmlConverter
) {
    fun convert(
        psiElements: Iterable<PsiElement>,
        docTagParserContext: DocTagParserContext,
        commentResolutionContext: CommentResolutionContext
    ): String? {
        return WithContext(docTagParserContext, commentResolutionContext)
            .convert(psiElements)
    }

    private class ParseState(
        val currentJavadocTag: JavadocTag?
    ) {
        var previousElement: PsiElement? = null
        var openPreTags: Int = 0
        var closedPreTags: Int = 0
        var hasElements: Boolean = false
        var hasNonNullOutput: Boolean = false
        val out: StringBuilder = StringBuilder()

        fun append(parsed: String?) {
            hasElements = true
            if (parsed != null) {
                out.append(parsed)
                hasNonNullOutput = true
            }
        }

        fun toParsedLine(): String? {
            if (!hasElements) return null
            return if (hasNonNullOutput) out.toString() else ""
        }
    }

    private inner class WithContext(
        private val docTagParserContext: DocTagParserContext,
        private val commentResolutionContext: CommentResolutionContext
    ) {
        private val isMarkdownDocComment = commentResolutionContext.comment.isMarkdownComment

        private val snippetToHtmlConverter: SnippetToHtmlConverter by lazy {
            DefaultSnippetToHtmlConverter(sourceSet, docTagParserContext, logger)
        }

        fun convert(psiElements: Iterable<PsiElement>): String? {
            val state = ParseState(commentResolutionContext.tag)
            for (psiElement in psiElements) {
                parseHtml(psiElement, state)
            }
            return state.toParsedLine()?.trim()?.let {
                if (isMarkdownDocComment) markdownToHtmlConverterProvider().convertMarkdownToHtml(it.trimIndent()) else it
            }
        }

        private fun parseHtml(psiElement: PsiElement, state: ParseState) {
            if (psiElement is PsiReference) {
                val children = psiElement.children
                if (children.isEmpty()) {
                    state.hasElements = true
                } else {
                    for (child in children) {
                        parseHtml(child, state)
                    }
                }
            } else {
                parseHtmlOfSimpleElement(psiElement, state)
            }
        }

        private fun parseHtmlOfSimpleElement(psiElement: PsiElement, state: ParseState) {
            val text = psiElement.text

            var openPre = state.openPreTags
            var closedPre = state.closedPreTags
            if (text.indexOf('<') >= 0) {
                openPre += PRE_OPENING_TAG_REGEX.findAll(text).count()
                closedPre += countLiteralOccurrences(text, PRE_CLOSING_TAG)
            }
            val isInsidePre = openPre > closedPre

            val parsed = when (psiElement) {
                is PsiInlineDocTag -> psiElement.toHtml(state.currentJavadocTag)
                is PsiDocParamRef -> psiElement.toDocumentationLinkString()
                is PsiMarkdownCodeBlock -> psiElement.toHtml()
                is PsiMarkdownReferenceLink -> psiElement.linkElement?.referenceElementOrSelf()?.toDocumentationLinkString(psiElement.label?.text ?: "")
                is PsiDocTagValue, is LeafPsiElement -> {
                    psiElement.stringifyElementAsText(isInsidePre || isMarkdownDocComment, state.previousElement, text)
                }
                else -> null
            }
            state.append(parsed)
            if (!text.isBlank()) {
                state.previousElement = psiElement
            }
            state.openPreTags = openPre
            state.closedPreTags = closedPre
        }

        /**
         * Inline tags can be met in the middle of some text. Example of an inline tag usage:
         *
         * ```java
         * Use the {@link #getComponentAt(int, int) getComponentAt} method.
         * ```
         */
        private fun PsiInlineDocTag.toHtml(javadocTag: JavadocTag?): String? =
            when (this.name) {
                "link", "linkplain" -> this.referenceElement()?.let { ref ->
                    val labelSb = StringBuilder()
                    var first = true
                    for (dataEl in this.dataElements) {
                        if (dataEl is PsiDocToken) {
                            if (!first) labelSb.append(' ')
                            first = false
                            labelSb.append(dataEl.stringifyElementAsText(keepFormatting = false).orEmpty())
                        }
                    }
                    ref.toDocumentationLinkString(labelSb.toString())
                }

                "code" -> dataElementsAsText(this).wrapInCodeTag(true)
                "literal" -> "<literal>${dataElementsAsText(this)}</literal>"
                "index" -> {
                    val sb = StringBuilder("<index>")
                    var first = true
                    for (child in this.children) {
                        if (child is PsiDocTagValue) {
                            if (!first) sb.append(", ")
                            first = false
                            sb.append(child.text)
                        }
                    }
                    sb.append("</index>").toString()
                }
                "inheritDoc" -> {
                    val inheritDocContent = inheritDocTagResolver.resolveContent(commentResolutionContext)
                    if (inheritDocContent == null) {
                        ""
                    } else {
                        val subState = ParseState(javadocTag)
                        for (content in inheritDocContent) {
                            content.appendInheritDocHtml(subState, docTagParserContext)
                        }
                        subState.toParsedLine().orEmpty()
                    }
                }
                "snippet" -> if (this is PsiSnippetDocTag) snippetToHtmlConverter.convertSnippet(this) else this.text

                else -> this.text
            }

        private fun DocumentationContent.appendInheritDocHtml(
            parserState: ParseState,
            docTagParserContext: DocTagParserContext
        ) {
            if (this is PsiDocumentationContent) {
                parseHtml(this.psiElement, parserState)
            } else {
                parserState.append(inheritDocTagResolver.convertToHtml(this, docTagParserContext))
            }
        }

        /**
         * Appends the data elements of an inline tag, along with the whitespace tokens between them.
         *
         * [PsiInlineDocTag.dataElements] used to include whitespaces, but since IntelliJ platform 261
         * `WHITE_SPACE` is no longer part of `PsiInlineDocTagImpl.VALUE_BIT_SET`. Whitespaces carry the line
         * breaks of a multiline tag body (e.g. `{@code}` spanning several lines), so they must be preserved
         * for tags whose content is rendered with formatting kept.
         */
        private fun dataElementsAsText(tag: PsiInlineDocTag): String {
            val dataElements = tag.dataElements
            val children = tag.children
            val sb = StringBuilder()
            var nextDataElement = 0
            for (child in children) {
                val include = if (nextDataElement < dataElements.size && dataElements[nextDataElement] === child) {
                    nextDataElement++
                    true
                } else {
                    child is PsiWhiteSpace
                }
                if (include) {
                    sb.append(child.stringifyElementAsText(keepFormatting = true).orEmpty())
                }
            }
            return sb.toString().htmlEscape()
        }

        private fun PsiElement.toDocumentationLinkString(label: String = ""): String {
            val displayLabel = label.ifBlank { defaultLabel().text }

            val driId = reference?.resolveToDocumentedElement()?.takeIf { it !is PsiParameter }?.let {
                val dri = DRI.from(it)
                docTagParserContext.store(dri)
            } ?: run {
                logger.warn("Couldn't resolve JavaDoc link '$displayLabel' in ${getLocation(this)}")
                UNRESOLVED_PSI_ELEMENT
            }

            return """<a data-dri="${driId.htmlEscape()}">$displayLabel</a>"""
        }

        private fun PsiMarkdownCodeBlock.toHtml() =
            if (isInline) {
                codeText.wrapInCodeTag(true)
            } else {
                "<pre${codeLanguage?.id?.lowercase()?.let { " lang=\"$it\"" } ?: ""}><code>${codeText.trimIndent()}</code></pre>"
            }

        private fun String.wrapInCodeTag(isDataInline: Boolean = false) =
            if (isMarkdownDocComment) "`$this`" // use ` for Markdown comments, further will be converted to <code> by MarkdownToHtmlConverter
            else "<code${if (isDataInline) " data-inline" else ""}>$this</code>"
    }

    private companion object {
        private val PRE_OPENING_TAG_REGEX = "<pre(\\s+.*)?>".toRegex()
    }
}

private fun countLiteralOccurrences(text: String, needle: String): Int {
    var count = 0
    var index = text.indexOf(needle)
    while (index >= 0) {
        count++
        index = text.indexOf(needle, index + needle.length)
    }
    return count
}

private fun PsiElement.stringifyElementAsText(
    keepFormatting: Boolean,
    previousElement: PsiElement? = null,
    rawText: String? = text
): String? =
    if (keepFormatting) {
        /*
        For values in the <pre> tag we try to keep formatting, so only the leading space is trimmed,
        since it is there because it separates this line from the leading asterisk
         */
        rawText?.let {
            if (((prevSibling as? PsiDocToken)?.isLeadingAsterisk() == true || (prevSibling as? PsiDocToken)?.isTagName() == true) && it.firstOrNull() == ' ')
                it.drop(1) else it
        }?.let {
            if ((nextSibling as? PsiDocToken)?.isLeadingAsterisk() == true) it.dropLastWhile { ch -> ch == ' ' } else it
        }
    } else {
        /*
        Outside of the <pre> we would like to trim everything from the start and end of a line since
        javadoc doesn't care about it.
         */
        rawText?.let { nonNullRaw ->
            val nonBlank = !nonNullRaw.isBlank()
            var adjusted = if ((prevSibling as? PsiDocToken)?.isLeadingAsterisk() == true && nonBlank && previousElement !is PsiInlineDocTag) {
                nonNullRaw.trimStart()
            } else {
                nonNullRaw
            }
            if ((nextSibling as? PsiDocToken)?.isLeadingAsterisk() == true && nonBlank) {
                adjusted = adjusted.trimEnd()
            }
            if (shouldHaveSpaceAtTheEnd(nonNullRaw)) "$adjusted " else adjusted
        }
    }

private fun PsiDocToken.isLeadingAsterisk() = tokenType == JavaDocTokenType.DOC_COMMENT_LEADING_ASTERISKS

private fun PsiDocToken.isTagName() = tokenType == JavaDocTokenType.DOC_TAG_NAME

/**
 * Checks whether a PSI sibling contains only whitespace without invoking [PsiElement.getText]
 * on composite [PsiDocTag] / [PsiInlineDocTag] subtrees.
 */
private fun PsiElement.isBlankJavadocSibling(): Boolean {
    if (this is PsiWhiteSpace) return true
    if (this is PsiDocTag || this is PsiInlineDocTag) return false
    return text.isBlank()
}

/**
 * Determines whether a trailing space should be appended after this element when joining multi-line Javadoc text.
 *
 * The space is required when:
 *  - tag spans multiple lines, between every line we would need a space
 *
 * We wouldn't like to render a space if:
 *  - tag is followed by an end of comment
 *  - after a tag there is another tag (eg. multiple @author tags)
 *  - they end with an html tag like: <a href="...">Something</a> since then the space will be displayed in the following text
 *  - next line starts with a <p> or <pre> token
 */
private fun PsiElement.shouldHaveSpaceAtTheEnd(rawText: String): Boolean {
    val immediateNext = nextSibling as? PsiWhiteSpace ?: return false
    if (!immediateNext.text.startsWith("\n ")) return false

    var cur = immediateNext.nextSibling
    var checkedFirstNonWhitespace = false
    while (cur != null) {
        if (!checkedFirstNonWhitespace && cur !is PsiWhiteSpace) {
            checkedFirstNonWhitespace = true
            if ((cur as? PsiDocToken)?.tokenType == JavaDocTokenTypes.INSTANCE.commentEnd()) {
                return false
            }
        }
        if (!cur.isBlankJavadocSibling()) break
        cur = cur.nextSibling
    }

    val nextNotEmptySibling = cur as? PsiDocToken ?: return false
    if (!nextNotEmptySibling.isLeadingAsterisk()) return false

    var endIdx = rawText.length - 1
    while (endIdx >= 0 && rawText[endIdx].isWhitespace()) {
        endIdx--
    }
    if (endIdx >= 0 && rawText[endIdx] == '>') {
        val lastHtmlTag = rawText.trim().substringAfterLast("<")
        if (lastHtmlTag.endsWith(">") && !lastHtmlTag.startsWith("</")) {
            return false
        }
    }

    var scan = nextNotEmptySibling.nextSibling
    while (scan != null) {
        if (scan is PsiDocToken && !scan.isLeadingAsterisk() && !scan.text.isBlank()) {
            return scan.tokenType == JavaDocTokenTypes.INSTANCE.commentData()
        }
        if (scan is PsiDocTag && scan !is PsiInlineDocTag) {
            return false
        }
        scan = scan.nextSibling
    }
    return false
}
