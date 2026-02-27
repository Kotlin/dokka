/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.doccomment.DocCommentFinder
import org.jetbrains.dokka.analysis.java.doccomment.JavaDocComment
import org.jetbrains.dokka.model.doc.DocumentationNode

internal fun interface JavaDocumentationParser {
    fun parseDocumentation(element: PsiNamedElement, sourceSet: DokkaSourceSet): DocumentationNode
}

@InternalDokkaApi
public class JavadocParser(
    private val docCommentParsers: List<DocCommentParser>,
    private val docCommentFinder: DocCommentFinder
) : JavaDocumentationParser {

    override fun parseDocumentation(element: PsiNamedElement, sourceSet: DokkaSourceSet): DocumentationNode {
        val comment = docCommentFinder.findClosestToElement(element) ?: return DocumentationNode(emptyList())
        return docCommentParsers
            .first { it.canParse(comment) }
            .parse(comment, element, sourceSet)
    }

    /**
     * Parses raw Javadoc text (e.g. from a template) into a [DocumentationNode].
     * Creates a [com.intellij.psi.javadoc.PsiDocComment] from the text using the [context]'s project,
     * wraps it in a [JavaDocComment], and parses it through the doc comment parser pipeline.
     */
    public fun parseDocCommentFromText(javadocText: String, context: PsiNamedElement, sourceSet: DokkaSourceSet): DocumentationNode {
        val psiDocComment = JavaPsiFacade.getElementFactory(context.project)
            .createDocCommentFromText(javadocText)
        val docComment = JavaDocComment(psiDocComment)
        return docCommentParsers
            .first { it.canParse(docComment) }
            .parse(docComment, context, sourceSet)
    }
}
