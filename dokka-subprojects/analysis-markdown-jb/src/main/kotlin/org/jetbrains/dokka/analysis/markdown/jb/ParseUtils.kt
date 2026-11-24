/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb

import org.intellij.markdown.lexer.Compat
import org.intellij.markdown.lexer.Compat.forEachCodePoint
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Text
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.TextNode

private const val AMPERSAND_CODE_POINT = 0x26
private const val LESS_THAN_CODE_POINT = 0x3C
private const val GREATER_THAN_CODE_POINT = 0x3E
private const val NBSP_CODE_POINT = 0xA0

@InternalDokkaApi
public fun String.parseHtmlEncodedWithNormalisedSpaces(
    renderWhiteCharactersAsSpaces: Boolean
): List<DocTag> {
    val accum = StringBuilder()
    val tags = mutableListOf<DocTag>()
    var lastWasWhite = false

    forEachCodePoint { c ->
        when {
            StringUtil.isWhitespace(c) -> {
                if (renderWhiteCharactersAsSpaces) {
                    if (!lastWasWhite) {
                        accum.append(' ')
                        lastWasWhite = true
                    }
                } else {
                    accum.appendCodePoint(c)
                }
            }
            requiresHtmlBodyEscaping(c) -> {
                if (accum.hasNonWhitespace()) {
                    tags.add(Text(accum.toString()))
                }
                accum.setLength(0)

                accum.appendCodePoint(c)
                tags.add(Text(accum.toString(), params = DocTag.contentTypeParam("html")))
                accum.setLength(0)
            }
            !StringUtil.isInvisibleChar(c) -> {
                accum.appendCodePoint(c)
                lastWasWhite = false
            }
        }
    }
    if (accum.hasNonWhitespace()) {
        tags.add(Text(accum.toString()))
    }
    return tags
}

/**
 * Equivalent to `Compat.codePointToString(c).let { it != TextNode(it).outerHtml() }` without allocating
 * a Jsoup [TextNode] and default `Document` on every ASCII / non-breaking-space code point.
 */
private fun requiresHtmlBodyEscaping(c: Int): Boolean {
    if (c < 0x80) {
        return c == AMPERSAND_CODE_POINT || c == LESS_THAN_CODE_POINT || c == GREATER_THAN_CODE_POINT
    }
    if (c == NBSP_CODE_POINT) return true
    return Compat.codePointToString(c).let { it != TextNode(it).outerHtml() }
}

private fun StringBuilder.hasNonWhitespace(): Boolean {
    for (i in 0 until length) {
        if (!this[i].isWhitespace()) return true
    }
    return false
}
