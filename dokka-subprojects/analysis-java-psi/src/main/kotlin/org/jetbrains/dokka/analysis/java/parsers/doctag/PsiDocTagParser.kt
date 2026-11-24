/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.java.parsers.CommentResolutionContext
import org.jetbrains.dokka.analysis.java.parsers.doctag.markdown.JavaDocMarkdownFlavourDescriptor
import org.jetbrains.dokka.analysis.java.parsers.doctag.markdown.MarkdownToHtmlConverter
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * Parses [PsiElement] of [PsiDocTag] into Dokka's [DocTag]
 */
internal class PsiDocTagParser(
    private val context: DokkaContext,
    private val inheritDocTagResolver: InheritDocTagResolver
) {

    private val markdownToHtmlConverter by lazy { MarkdownToHtmlConverter(JavaDocMarkdownFlavourDescriptor()) }

    fun parse(
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext,
        sourceSet: DokkaSourceSet
    ): List<DocTag> = parse(asParagraph = false, psiElements, commentResolutionContext, sourceSet)

    fun parseAsParagraph(
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext,
        sourceSet: DokkaSourceSet
    ): List<DocTag> = parse(asParagraph = true, psiElements, commentResolutionContext, sourceSet)

    private fun parse(
        asParagraph: Boolean,
        psiElements: Iterable<PsiElement>,
        commentResolutionContext: CommentResolutionContext,
        sourceSet: DokkaSourceSet
    ): List<DocTag> {
        val docTagParserContext = DocTagParserContext()

        val psiToHtmlConverter = PsiElementToHtmlConverter(
            inheritDocTagResolver = inheritDocTagResolver,
            sourceSet = sourceSet,
            logger = context.logger,
            markdownToHtmlConverterProvider = { markdownToHtmlConverter }
        )

        val elementsHtml = psiToHtmlConverter.convert(psiElements, docTagParserContext, commentResolutionContext)
            ?: return emptyList()
        if (elementsHtml.isEmpty()) return emptyList()
        if (isPlainSingleLineText(elementsHtml)) {
            val textTags = listOf(Text(body = elementsHtml))
            return if (asParagraph) listOf(P(textTags)) else textTags
        }

        val htmlToDocTagConverter = HtmlToDocTagConverter(docTagParserContext)
        val html = if (asParagraph) "<p>$elementsHtml</p>" else elementsHtml
        return htmlToDocTagConverter.convertToDocTag(html)
    }

    /**
     * Fast check for single-line plain ASCII text free of HTML tags, entities, leading/trailing spaces,
     * and consecutive spaces, allowing [parse] to construct [Text]/[P] directly without Jsoup DOM allocation.
     */
    private fun isPlainSingleLineText(s: String): Boolean {
        if (s.isEmpty() || s[0] == ' ' || s[s.length - 1] == ' ') return false
        var prevSpace = false
        for (i in s.indices) {
            val ch = s[i]
            if (ch < ' ' || ch > '~' || ch == '<' || ch == '>' || ch == '&') {
                return false
            }
            if (ch == ' ') {
                if (prevSpace) return false
                prevSpace = true
            } else {
                prevSpace = false
            }
        }
        return true
    }
}
