/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.analysis.java.DescriptionJavadocTag
import org.jetbrains.dokka.analysis.java.JavadocTag

internal fun PsiDocComment.hasTag(tag: JavadocTag): Boolean =
    when (tag) {
        DescriptionJavadocTag -> descriptionElements.isNotEmpty()
        else -> findTagByName(tag.name) != null
    }

internal fun PsiDocTag.contentElementsWithSiblingIfNeeded(): List<PsiElement> {
    val elements = dataElements
    if (elements.isEmpty()) return emptyList()
    val first = elements[0]
    val second = elements.getOrNull(1)
    val nextSibling = first.nextSibling?.takeIf { sibling ->
        sibling !== second && sibling.text != second?.text
    }
    val result = ArrayList<PsiElement>(elements.size + (if (nextSibling != null) 1 else 0))
    result.add(first)
    if (nextSibling != null) {
        result.add(nextSibling)
    }
    for (i in 1 until elements.size) {
        result.add(elements[i])
    }
    return result
}

internal fun PsiDocTag.resolveToElement(): PsiElement? =
    dataElements.firstOrNull()?.firstChild?.referenceElementOrSelf()?.resolveToGetDri()

internal fun PsiDocTag.referenceElement(): PsiElement? =
    linkElement()?.referenceElementOrSelf()

internal fun PsiElement.referenceElementOrSelf(): PsiElement? =
    if (node.elementType == JavaDocElementType.DOC_REFERENCE_HOLDER) {
        PsiTreeUtil.findChildOfType(this, PsiJavaCodeReferenceElement::class.java)
    } else this

internal fun PsiDocTag.linkElement(): PsiElement? =
    dataElements.firstOrNull {
        /**
         * * According to the [com.intellij.java.syntax.parser.JavaDocParser],
         * no other node is expected here for the reference (link)
         */
        it.node.elementType == JavaDocElementType.DOC_REFERENCE_HOLDER ||
                it.node.elementType == JavaDocElementType.DOC_METHOD_OR_FIELD_REF
    }

internal fun PsiElement.defaultLabel() = children.firstOrNull {
    it is PsiDocToken && it.text.isNotBlank() && !it.isSharpToken()
} ?: this

internal fun PsiDocToken.isSharpToken() = tokenType == JavaDocTokenType.DOC_TAG_VALUE_SHARP_TOKEN
